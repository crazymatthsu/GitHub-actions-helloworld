package com.example.deephaven;

import java.util.List;

public final class PublishEquityOrders {

    private PublishEquityOrders() {}

    public static void main(String[] args) throws Exception {
        String target = target(args);
        List<EquityOrder> orders = SampleEquityOrders.orders();
        System.out.println(EquityOrderJson.toJson(orders));
        publishWithRetry(target, orders);
        System.out.println("Published " + orders.size() + " orders to " + DeephavenEquityOrderPublisher.TABLE_NAME);
    }

    static String target(String[] args) {
        String fromEnv = System.getenv("DEEPHAVEN_TARGET");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return args.length > 0 ? args[0] : "localhost:10000";
    }

    private static void publishWithRetry(String target, List<EquityOrder> orders) throws Exception {
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= 36; attempt++) {
            try {
                DeephavenEquityOrderPublisher.publish(target, orders);
                return;
            } catch (Exception ex) {
                lastFailure = ex;
                System.out.println("Publish attempt " + attempt + " to " + target + " failed: " + ex.getMessage());
                Thread.sleep(5_000);
            }
        }
        throw lastFailure;
    }
}
