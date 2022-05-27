import React, { useState } from "react"
// eslint-disable-next-line import/namespace
import { StyleSheet, Text, TextInput, TouchableOpacity, View } from "react-native"
import { Accelerometer } from "expo-sensors"
const TOLERATE = 0.01
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: "center",
    paddingHorizontal: 10,
  },
  text: {
    textAlign: "center",
  },
  buttonContainer: {
    flexDirection: "row",
    marginTop: 15,
  },
  button: {
    flex: 1,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "#eee",
    padding: 10,
    width: 40,
  },
  middleButton: {
    borderLeftWidth: 1,
    borderRightWidth: 1,
    borderColor: "#ccc",
  },
  input: {
    height: 40,
    margin: 12,
    borderWidth: 1,
    padding: 10,
  },
})

export default function App() {
  const [data, setData] = useState({
    x: 0,
    y: 0,
    z: 0,
  })
  const [isEstablished, setIsEstablished] = useState(false)
  const [isEnd, setIsEnd] = useState(false)
  const [text, onChangeText] = React.useState("ws://192.168.50.2:8091")
  const [isWorking, setIsWorking] = React.useState(false)

  const connect = () => {
    setIsWorking(true)
    const ws = new WebSocket(text)
    ws.onopen = () => {
      setIsEstablished(true)
      Accelerometer.setUpdateInterval(100)
      let unsubscribe: () => void
      const subscription = Accelerometer.addListener((accelerometerData) => {
        if (judgeEnd(accelerometerData)) {
          setIsEnd(true)
          unsubscribe()
        }
        setData(accelerometerData)
        ws.send(JSON.stringify(accelerometerData))
      })
      unsubscribe = () => {
        subscription?.remove()
        ws.close()
      }
    }
    ws.onclose = () => {
      setIsEstablished(false)
    }
  }
  const { x, y, z } = data
  return (
    <View style={styles.container}>
      <Text style={styles.text}>
        作者：袁嘉昊 2019010070
      </Text>
      {!isWorking
        ? <View>
          <TextInput
            style={styles.input}
            onChangeText={onChangeText}
            value={text}
          />
          <View style={styles.buttonContainer}>
            <TouchableOpacity onPress={connect} style={[styles.button, styles.middleButton]}>
              <Text>连接</Text>
            </TouchableOpacity>
          </View>
        </View>
        : <View>
          <Text style={styles.text}>{isEstablished ? "websocket 建立" : "websocket 断开"}</Text>
          {!isEnd
            ? <View>
              <Text style={styles.text}>
                x: {round(x)}
              </Text>
              <Text style={styles.text}>
                y: {round(y)}
              </Text>
              <Text style={styles.text}>
                z: {round(z)}
              </Text>
            </View>
            : <View>
              <Text style={styles.text}>停止录制</Text>
            </View>}
        </View>}
    </View>
  )
}
function round(n: number) {
  if (!n) {
    return 0
  }
  return Math.floor(n * 10) / 10
}

function judgeEnd({ x, y, z }: { x: number; y: number; z: number }) {
  return Math.abs(x) < TOLERATE && Math.abs(y) < TOLERATE && Math.abs(z + 1) < TOLERATE
}
