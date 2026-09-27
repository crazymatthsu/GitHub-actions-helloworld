package com.example.deephaven;

import java.util.List;

public final class SampleEquityOrders {

    private SampleEquityOrders() {}

    public static List<EquityOrder> orders() {
        return List.of(
                new EquityOrder("ORD-1001", "AAPL", "BUY", 100, 189.50),
                new EquityOrder("ORD-1002", "MSFT", "SELL", 50, 415.20),
                new EquityOrder("ORD-1003", "NVDA", "BUY", 25, 875.00));
    }
}
