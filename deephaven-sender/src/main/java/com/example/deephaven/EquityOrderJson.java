package com.example.deephaven;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

public final class EquityOrderJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<EquityOrder>> ORDER_LIST = new TypeReference<>() {};

    private EquityOrderJson() {}

    public static String toJson(List<EquityOrder> orders) {
        try {
            return MAPPER.writeValueAsString(orders);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not write equity orders as JSON", ex);
        }
    }

    public static List<EquityOrder> fromJson(String json) {
        try {
            return MAPPER.readValue(json, ORDER_LIST);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not read equity orders from JSON", ex);
        }
    }
}
