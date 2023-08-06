package com.tastybug.springtaskexecutor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

@Component
public class Kitchen {

    private final Executor cooks;

    /*
        Note: This component is a singleton by default, so multiple calls to this would share the
        same task executor and thus the same underlying thread pool.
     */
    public Kitchen(@Qualifier("cooks") Executor cooks) {
        this.cooks = cooks;
    }

    public ConcurrentHashMap<String, String> processOrder(List<String> orderItems) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(orderItems.size());
        final ConcurrentHashMap<String, String> orderReport = new ConcurrentHashMap<>();

        for (String lineItem : orderItems) {
            cooks.execute(() -> prepareOneItemFromOrder(latch, orderReport, lineItem));
        }
        System.out.printf("Order with %d items submitted, waiting now until everything is processed.%n", orderItems.size());
        latch.await(); // blocks until all tasks are done
        return orderReport;
    }

    private void prepareOneItemFromOrder(CountDownLatch latch, ConcurrentHashMap<String, String> order, String lineItem) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        latch.countDown();
        order.put(lineItem, "Finished " + lineItem);
    }
}
