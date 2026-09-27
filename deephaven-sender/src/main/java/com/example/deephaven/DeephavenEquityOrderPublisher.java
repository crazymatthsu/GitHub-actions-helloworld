package com.example.deephaven;

import io.deephaven.client.impl.DaggerDeephavenFlightRoot;
import io.deephaven.client.impl.FlightSession;
import io.deephaven.client.impl.ScopeId;
import io.deephaven.client.impl.TableHandle;
import io.deephaven.qst.column.header.ColumnHeader;
import io.deephaven.qst.table.InMemoryAppendOnlyInputTable;
import io.deephaven.qst.table.NewTable;
import io.deephaven.qst.table.TableHeader;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;

public final class DeephavenEquityOrderPublisher {

    public static final String TABLE_NAME = "equity_orders";

    private DeephavenEquityOrderPublisher() {}

    public static void publish(String target, List<EquityOrder> orders) throws Exception {
        BufferAllocator allocator = new RootAllocator();
        try {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
            try {
                ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
                try {
                    FlightSession flight = DaggerDeephavenFlightRoot.create()
                            .factoryBuilder()
                            .managedChannel(channel)
                            .scheduler(scheduler)
                            .allocator(allocator)
                            .build()
                            .newFlightSession();
                    try {
                        publish(flight, allocator, orders);
                    } finally {
                        try {
                            flight.session().closeFuture().get(5, TimeUnit.SECONDS);
                        } finally {
                            flight.close();
                        }
                    }
                } finally {
                    channel.shutdownNow();
                }
            } finally {
                scheduler.shutdownNow();
            }
        } finally {
            allocator.close();
        }
    }

    private static void publish(FlightSession flight, BufferAllocator allocator, List<EquityOrder> orders)
            throws Exception {
        ColumnHeader<String> orderId = ColumnHeader.ofString("OrderId");
        ColumnHeader<String> symbol = ColumnHeader.ofString("Symbol");
        ColumnHeader<String> side = ColumnHeader.ofString("Side");
        ColumnHeader<Long> quantity = ColumnHeader.ofLong("Quantity");
        ColumnHeader<Double> price = ColumnHeader.ofDouble("Price");
        ColumnHeader<String> orderJson = ColumnHeader.ofString("OrderJson");
        TableHeader header = TableHeader.of(orderId, symbol, side, quantity, price, orderJson);

        ScopeId scopeId;
        try (TableHandle handle = flight.session().execute(InMemoryAppendOnlyInputTable.of(header))) {
            flight.session().publish(TABLE_NAME, handle).get(10, TimeUnit.SECONDS);
            scopeId = new ScopeId(TABLE_NAME);
        }

        var rows = orderId.header(symbol).header(side).header(quantity).header(price).header(orderJson)
                .start(orders.size());
        for (EquityOrder order : orders) {
            rows.row(
                    order.orderId(),
                    order.symbol(),
                    order.side(),
                    order.quantity(),
                    order.price(),
                    EquityOrderJson.toJson(List.of(order)));
        }
        NewTable table = rows.newTable();
        flight.addToInputTable(scopeId, table, allocator).get(10, TimeUnit.SECONDS);
    }
}
