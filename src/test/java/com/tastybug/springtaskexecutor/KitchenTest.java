package com.tastybug.springtaskexecutor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
class KitchenTest {


	@Import(KitchenConfig.class)
	@Configuration
	static class TestConfig {

		@Bean
		@Qualifier("waiters")
		public TaskExecutor waiters() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			executor.setCorePoolSize(5);
			executor.setMaxPoolSize(5);
			executor.initialize();
			return executor;
		}
	}

	@Autowired
	private Kitchen kitchen;

	@Autowired
	@Qualifier("waiters")
	private Executor waiters;

	@Test
	void oneWaiter() throws InterruptedException {
		//
		List<String> order = order20("cake");

		//
		ConcurrentHashMap<String, String> result = kitchen.processOrder(order);

		//
		assertThat(result).hasSize(order.size());
		assertThat(result.get("cake" + order.size())).isEqualTo("Finished cake20");
	}

	/*
		The component ParallelTaskExecution is a singleton, which can be used from multiple threads.
		In that case, all callers use the same underlying thread pool of ParallelTaskExecution.
		There more there is to do, the longer this can take. The following test is meant to illustrate this and
		show threat safety in the approach.

		It orders 20 cakes, 20 beef, 20 beer, 20 wine and 20 coffee. All utilizing the same cooks.
	 */
	@Test
	void multipleWaiters() throws InterruptedException {
		List<String> menuItems = List.of("cake", "beef", "beer", "wine", "coffee");
		CountDownLatch orderFulfilledLatch = new CountDownLatch(menuItems.size());
		Map<String, ConcurrentHashMap<String, String>> allOrders = new ConcurrentHashMap<>();

		// when: we order 20 of each menu item
		for (String menuItem : menuItems) {
			waiters.execute(() -> {
				try {
					allOrders.put(menuItem, kitchen.processOrder(order20(menuItem)));
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				} finally {
					orderFulfilledLatch.countDown();
				}
			});
		}

		// then: we wait for ALL orders to complete and see everything is done
		assertThat(orderFulfilledLatch.await(2, TimeUnit.MINUTES)).isTrue();
		for (String menuItem : menuItems) {
			ConcurrentHashMap<String, String> order = allOrders.get(menuItem);
			assertThat(order).hasSize(20);
			for (int i = 1; i <= 20; i++) {
				assertThat(order.get(menuItem + i)).isEqualTo("Finished " + menuItem + i);
			}
		}
	}

	private List<String> order20(String whatToMake) {
		ArrayList<String> inputs = new ArrayList<>();
		for (int i = 1; i <= 20; i++) {
			inputs.add(whatToMake + i);
		}
		return inputs;
	}
}
