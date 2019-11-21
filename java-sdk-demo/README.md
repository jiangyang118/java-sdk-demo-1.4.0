## 使用方法

通过如下步骤运行fabric-sdk-java的示例程序。 

### 确认配置文件

将SDK包中的配置文件 `connection-profile-standard.yaml` 复制到当前目录。

### 安装 jar 包

我们已预先下载了java-sdk 1.2.0版本的jar包，位于 `lib` 目录下，

`fabric-sdk-java-1.2.0-jar-with-dependencies.jar`: 包含了fabric-sdk-java和它的所有依赖。
`fabric-sdk-java-1.2.0-sources.jar`: 包含了fabric-sdk-java的源码。


执行如下命令将jar包安装到本地maven仓库：
```
mvn install:install-file -Dfile=./lib/fabric-sdk-java-1.2.0-jar-with-dependencies.jar -DgroupId=org.hyperledger.fabric-sdk-java -DartifactId=fabric-sdk-java -Dversion=1.2.0 -Dpackaging=jar
```

若需要阅读fabric-sdk-java源码，可以安装源码包：

```
mvn install:install-file -Dfile=./lib/fabric-sdk-java-1.2.0-sources.jar -DgroupId=org.hyperledger.fabric-sdk-java -DartifactId=fabric-sdk-java -Dversion=1.2.0 -Dpackaging=jar -Dclassifier=sources
```

### 部署链码

将 chaincode 目录中的 `sacc.out` 上传至 BaaS 平台，并完成安装、实例化步骤，具体方法请参考：https://help.aliyun.com/document_detail/85739.html

### 打开项目

打开任意IDE，导入java项目

修改`java-sdk-demo/src/main/java/com/aliyun/baas/Main.java` 文件中的内容以匹配您的配置

```java
    private static String channelName = "first-channel";  // 通道名称
    private static String userName = "user1";             // 用户名
    private static String secret = "password";            // 密码
```

执行 com.aliyun.baas.Main 程序即可

```shell
mvn compile
mvn exec:java -Dexec.mainClass="com.aliyun.baas.Main"  -Dexec.classpathScope=runtime -Dexec.cleanupDaemonThreads=false
```

## 示例程序说明

该示例程序会执行如下操作：

1. Enroll 用户
2. 读取配置文件，连接到channel相关的peer，并监听块事件。
3. 获取账本的块信息并输出
4. 调用 sacc 智能合约，修改账本，并读取
5. 断开和peer的连接

## 运行结果

如示例程序运行成功，可观察到类似如下的输出信息：

```
11:05:14,362 INFO  - com.aliyun.baas.Main              - =============================================================
11:05:14,418 INFO  - com.aliyun.baas.ChaincodeExecuter - [√] Got success response from peer peer2.aliorg.aliyunbaas.com:31121 => payload: 235
11:05:14,418 INFO  - com.aliyun.baas.ChaincodeExecuter - [√] Got success response from peer peer1.aliorg.aliyunbaas.com:31111 => payload: 235
11:05:14,418 INFO  - com.aliyun.baas.ChaincodeExecuter - Sending transaction to orderers...
11:05:16,976 INFO  - com.aliyun.baas.Main              - Receive block event (number 16) from Peer{ id: 5, name: peer1.aliorg.aliyunbaas.com:31111, channelName: testchannel01, url: grpcs://peer1.aliorg.aliyunbaas.com:31111}
11:05:16,979 INFO  - com.aliyun.baas.Main              - Receive block event (number 16) from Peer{ id: 6, name: peer2.aliorg.aliyunbaas.com:31121, channelName: testchannel01, url: grpcs://peer2.aliorg.aliyunbaas.com:31121}
11:05:16,980 INFO  - com.aliyun.baas.ChaincodeExecuter - Orderer response: txid23197603990e6f8482e01ed6aeb1a9e6294b8937b9522f18f3474a5f7381c510
11:05:16,980 INFO  - com.aliyun.baas.ChaincodeExecuter - Orderer response: block number: 16
11:05:17,036 INFO  - com.aliyun.baas.ChaincodeExecuter - [√] Got success response from peer peer2.aliorg.aliyunbaas.com:31121 => payload: 235
11:05:17,036 INFO  - com.aliyun.baas.ChaincodeExecuter - [√] Got success response from peer peer1.aliorg.aliyunbaas.com:31111 => payload: 235
11:05:17,036 INFO  - com.aliyun.baas.Main              - =============================================================
11:05:17,088 INFO  - com.aliyun.baas.ChaincodeExecuter - [√] Got success response from peer peer2.aliorg.aliyunbaas.com:31121 => payload: 882
11:05:17,089 INFO  - com.aliyun.baas.ChaincodeExecuter - [√] Got success response from peer peer1.aliorg.aliyunbaas.com:31111 => payload: 882
11:05:17,089 INFO  - com.aliyun.baas.ChaincodeExecuter - Sending transaction to orderers...
11:05:19,351 INFO  - com.aliyun.baas.Main              - Receive block event (number 17) from Peer{ id: 5, name: peer1.aliorg.aliyunbaas.com:31111, channelName: testchannel01, url: grpcs://peer1.aliorg.aliyunbaas.com:31111}
11:05:19,354 INFO  - com.aliyun.baas.Main              - Receive block event (number 17) from Peer{ id: 6, name: peer2.aliorg.aliyunbaas.com:31121, channelName: testchannel01, url: grpcs://peer2.aliorg.aliyunbaas.com:31121}
11:05:19,354 INFO  - com.aliyun.baas.ChaincodeExecuter - Orderer response: txidffdaf80944641184175a0506460cffa3bda21200f477a4b853bed546c3326338
11:05:19,354 INFO  - com.aliyun.baas.ChaincodeExecuter - Orderer response: block number: 17
11:05:19,414 INFO  - com.aliyun.baas.ChaincodeExecuter - [√] Got success response from peer peer2.aliorg.aliyunbaas.com:31121 => payload: 882
11:05:19,414 INFO  - com.aliyun.baas.ChaincodeExecuter - [√] Got success response from peer peer1.aliorg.aliyunbaas.com:31111 => payload: 882
11:05:19,414 INFO  - com.aliyun.baas.Main              - Shutdown channel.
11:05:19,420 INFO  - org.hyperledger.fabric.sdk.Channel - Channel testchannel01 eventThread shutting down. shutdown: true  thread: pool-1-thread-1
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  10.747 s
[INFO] Finished at: 2019-07-17T11:05:19+08:00
[INFO] ------------------------------------------------------------------------
```

## 问题诊断

1. 错误示例：send proposal超时失败

```
20:23:45,728 ERROR - org.hyperledger.fabric.sdk.Channel - Channel Channel{id: 1, name: testchannel01} sending proposal with transaction 45bb66f204d45c19b36b4d0d2cc2871c6f2bf33c309961960364eda05b0a1e99 to Peer{ id: 5, name: peer1.aliorg.aliyunbaas.com:31111, channelName: testchannel01, url: grpcs://peer1.aliorg.aliyunbaas.com:31111} failed because of timeout(6000 milliseconds) expiration
java.util.concurrent.TimeoutException: Waited 6000 milliseconds for io.grpc.stub.ClientCalls$GrpcFuture@b660793[status=PENDING, info=[GrpcFuture{clientCall={delegate={delegate=ClientCallImpl{method=MethodDescriptor{fullMethodName=protos.Endorser/ProcessProposal, type=UNARY, idempotent=false, safe=false, sampledToLocalTracing=true, requestMarshaller=io.grpc.protobuf.lite.ProtoLiteUtils$MessageMarshaller@7fd71cf, responseMarshaller=io.grpc.protobuf.lite.ProtoLiteUtils$MessageMarshaller@4f796e35, schemaDescriptor=org.hyperledger.fabric.protos.peer.EndorserGrpc$EndorserMethodDescriptorSupplier@5a101a3}}}}}]]
	at com.google.common.util.concurrent.AbstractFuture.get(AbstractFuture.java:471)
	at org.hyperledger.fabric.sdk.Channel.sendProposalToPeers(Channel.java:4108)
	at org.hyperledger.fabric.sdk.Channel.sendProposal(Channel.java:4032)
	at org.hyperledger.fabric.sdk.Channel.sendTransactionProposal(Channel.java:3915)
	at com.aliyun.baas.ChaincodeExecuter.executeTransaction(ChaincodeExecuter.java:93)
	at com.aliyun.baas.Main.executeChaincode(Main.java:107)
	at com.aliyun.baas.Main.main(Main.java:85)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at org.codehaus.mojo.exec.ExecJavaMojo$1.run(ExecJavaMojo.java:282)
	at java.lang.Thread.run(Thread.java:748)
```

```
20:23:45,734 WARN  - com.aliyun.baas.ChaincodeExecuter - [×] Got failed response from peer peer1.aliorg.aliyunbaas.com:31111 => FAILURE: Channel Channel{id: 1, name: testchannel01} sending proposal with transaction 45bb66f204d45c19b36b4d0d2cc2871c6f2bf33c309961960364eda05b0a1e99 to Peer{ id: 5, name: peer1.aliorg.aliyunbaas.com:31111, channelName: testchannel01, url: grpcs://peer1.aliorg.aliyunbaas.com:31111} failed because of timeout(6000 milliseconds) expiration
```

如遇到类似上述的错误，可能的原因包括客户端应用程序与Fabric网络的peer服务通信超时。解决方法可考虑：
- 检查客户端应用程序与Fabric网络peer服务间的网络连通性
- 如连接是通过外部网络或网络延时较高，可考虑调整 `ChaincodeExecuter.java`中的 `waitTime` 数值



## 参考链接

- [fabric-sdk-java官方地址](https://github.com/hyperledger/fabric-sdk-java)
- [sdk-java maven 仓库](http://central.maven.org/maven2/org/hyperledger/fabric-sdk-java/fabric-sdk-java/1.2.0/)


