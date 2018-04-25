# RemoteDebug
包含两个功能，远程代理内网穿透和远程调试，基于 [Android-Debug-Database](https://github.com/amitshekhariitbhu/Android-Debug-Database)

接入说明：

1. 在项目中引入libdebug，一行代码开启调试`DebugHelper.start(this)`,结束调试调用`DebugHelper.close(this)`
2. 浏览器访问设备ip:8080，如192.168.10.101:8080
3. 即可成功开启数据库调试，浏览文件
4. 提供adb web调试，请点击Terminal

