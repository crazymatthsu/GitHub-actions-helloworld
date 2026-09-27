package com.example.deephaven;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EquityOrderJsonTest {

    @Test
    void sampleOrdersRoundTripAsJson() {
        List<EquityOrder> orders = SampleEquityOrders.orders();
        String json = EquityOrderJson.toJson(orders);

        assertTrue(json.contains("\"symbol\":\"AAPL\""));
        assertTrue(json.contains("\"symbol\":\"MSFT\""));
        assertTrue(json.contains("\"symbol\":\"NVDA\""));
        assertEquals(orders, EquityOrderJson.fromJson(json));
    }
}
