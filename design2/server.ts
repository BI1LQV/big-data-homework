import { WebSocketServer, type WebSocketClient } from "https://deno.land/x/websocket@v0.1.3/mod.ts"
import { MongoClient, type ObjectId } from "https://deno.land/x/mongo@v0.30.0/mod.ts"
// @deno-types="https://unpkg.com/dayjs@1.8.23/index.d.ts"
import dayjs from 'https://unpkg.com/dayjs@1.8.23/esm/index.js'
//定义常量
//端口
const RECEIVE_PORT = 8091
const PUBLISH_PORT = 8092
const DISPLAY_PORT = 8093
//mongodb
const MONGO_DB_URL = "mongodb://127.0.0.1:27017" //如果有密码 可以这样写  "mongodb+srv://<username>:<password>@<db_cluster_url>/<db_name>?authMechanism=SCRAM-SHA-1",
const DATABASE_NAME = 'sensorData'
const COLLECTION_NAME = 'data'

type sensorData = { x: number, y: number, z: number }
interface SenSorSchema extends sensorData {
    _id: ObjectId
    time: Date
}
function parseData(data: string): sensorData & { time: Date } | null {
    try {
        const { x, y, z } = JSON.parse(data)
        if (
            typeof x === 'number' &&
            typeof y === 'number' &&
            typeof z === 'number'
        ) {
            return { x, y, z, time: new Date() }
        }
    } catch (e) { }
    return null
}

//连接mongodb
const client = new MongoClient()
await client.connect(MONGO_DB_URL)
const db = client.database(DATABASE_NAME)
const sensorDatas = db.collection<SenSorSchema>(COLLECTION_NAME)

//启动发送数据的静态服务器
const wssPub = new WebSocketServer(PUBLISH_PORT)
let wsPub: WebSocketClient | null = null
wssPub.on("connection", (ws: WebSocketClient) => {
    if (wsPub) {
        ws.close(400)
        return
    }
    console.log("wssPub established")
    wsPub = ws
    ws.on("close", function () {
        console.log("wssPub disconnected")
        wsPub = null
    })
    sensorDatas.find({
        time: {
            $gt: dayjs().subtract(1, 'minute').toDate()
        }
    }).toArray().then((res: SenSorSchema[]) => {
        ws.send(JSON.stringify(res))
    })
})

//启动接受数据的websocket
const wssRec = new WebSocketServer(RECEIVE_PORT)
wssRec.on("connection", function (ws: WebSocketClient) {
    console.log("wssRec established")
    ws.on("message", function (data: string) {
        const parsedData = parseData(data)
        if (parsedData) {
            sensorDatas.insertOne(parsedData)
            wsPub?.send(JSON.stringify(parsedData))
        }
    })
    ws.on('close', function () {
        console.log('wssRec disconnected')
    })
})

//展示页面的静态服务器
const server = Deno.listen({ port: DISPLAY_PORT })
for await (const conn of server) {
    serveHttp(conn);
}

async function serveHttp(conn: Deno.Conn) {
    const httpConn = Deno.serveHttp(conn)
    for await (const requestEvent of httpConn) {
        const body = await Deno.readTextFile("./display.htm")
        requestEvent.respondWith(
            new Response(body.replace('__WEBSOCKET_PORT__', String(PUBLISH_PORT)), {
                status: 200,
                headers: {
                    'content-type': 'text/html'
                },
            }),
        )
    }
}