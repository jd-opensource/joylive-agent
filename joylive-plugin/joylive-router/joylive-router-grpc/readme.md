
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

