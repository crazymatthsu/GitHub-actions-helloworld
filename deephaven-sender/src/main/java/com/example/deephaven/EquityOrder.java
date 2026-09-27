package com.example.deephaven;

public record EquityOrder(String orderId, String symbol, String side, long quantity, double price) {}
