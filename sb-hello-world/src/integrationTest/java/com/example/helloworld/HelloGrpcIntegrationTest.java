package com.example.helloworld;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.helloworld.grpc.HelloReply;
import com.example.helloworld.grpc.HelloRequest;
import com.example.helloworld.grpc.HelloWorldGrpc;
import com.example.helloworld.grpc.HelloWorldGrpc.HelloWorldBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"server.port=0", "spring.grpc.server.port=0"})
class HelloGrpcIntegrationTest {

    @LocalGrpcServerPort
    private int port;

    @Test
    void sayHelloUsesSharedGreeting() {
        ManagedChannel channel = NettyChannelBuilder.forAddress("localhost", port).usePlaintext().build();
        try {
            HelloWorldBlockingStub stub = HelloWorldGrpc.newBlockingStub(channel);
            HelloReply reply = stub.sayHello(HelloRequest.newBuilder().setName("Spring").build());
            assertEquals("Hello World, Spring", reply.getMessage());
        } finally {
            channel.shutdownNow();
        }
    }
}
