
#客户端调用
io.grpc.stub.ClientCalls
#服务端调用
io.grpc.stub.ServerCalls

以上两个类出现在dependency, 可以尝试增强这两个方法试试
<dependency>
    <groupId>io.grpc</groupId>
    <aritifactId>grpc-stub</aritifactId>
    <version>1.30.0</version>
</dependency>

调用方式可以参看项目：SpringBoot-Labs中的 labx-30-spring-cloud-grpc


gRpc demo调试：

需要先将项目切换到jdk8，然后单独对joylive-demo-grpc-service-api进行clean package，生成出grpc的java类，
生成地址在target/generated-sources/protobuf目录下，生成完毕后，再将项目整体切到jdk22进行编译。

