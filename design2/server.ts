import { WebSocketClient, WebSocketServer } from "https://deno.land/x/websocket@v0.1.3/mod.ts";

const wss = new WebSocketServer(8091);
let socket:WebSocketClient;
wss.on("connection", function (ws: WebSocketClient) {
  console.log("websocket established");
  ws.send("websocket established");
  socket=ws;
});

console.log(socket)