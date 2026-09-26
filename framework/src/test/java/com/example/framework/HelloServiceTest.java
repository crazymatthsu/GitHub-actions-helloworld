package com.example.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HelloServiceTest {

    @Test
    void helloReturnsGreeting() {
        assertEquals("Hello World", new HelloService().hello());
    }
}
