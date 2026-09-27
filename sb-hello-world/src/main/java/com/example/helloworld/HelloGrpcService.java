package com.example.helloworld;

import com.example.framework.HelloService;
import com.example.helloworld.grpc.HelloReply;
import com.example.helloworld.grpc.HelloRequest;
import com.example.helloworld.grpc.HelloWorldGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class HelloGrpcService extends HelloWorldGrpc.HelloWorldImplBase {

    private final HelloService helloService;

    public HelloGrpcService(HelloService helloService) {
        this.helloService = helloService;
    }

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String name = request.getName();
        String message = name.isBlank() ? helloService.hello() : helloService.hello() + ", " + name;
        responseObserver.onNext(HelloReply.newBuilder().setMessage(message).build());
        responseObserver.onCompleted();
    }
}
