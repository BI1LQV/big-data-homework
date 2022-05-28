## 文件介绍

server.ts 为服务器端 运行时为deno

client 为下位机 运行时为react native

display.htm 为展示 运行时为浏览器

## 依赖安装

deno安装：`https://deno.land/#installation`

注：在windows下只需要**以管理员**打开powershell运行`iwr https://deno.land/install.ps1 -useb | iex`

如果是第一次使用powershell，需要以**以管理员**打开powershell，并运行指令`set-executionpolicy remotesigned`，根据提示选择同意

## 启动服务器端服务

`deno run --allow-net --allow-read --allow-run server.ts`

具体端口配置可以在server.ts头部修改

## 下位机app打包

如果想要亲自打包app，需要安装nodejs，然后运行

```shell
npm -g i yarn
cd client
yarn
npm -g i expo-cli
expo build:ios
expo build:android
```

根据提示登陆/注册即可获得打包产物

需要注意的是，ios打包需要开发者账号的证书
