package me.saharnooby.agent.minecraft.threadpool;

import lombok.NonNull;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * @author saharNooby
 * @since 18:20 30.09.2020
 */
@SuppressWarnings("unused")
public final class AgentRuntime {

	private static long start;

	public static String poolName;

	static {
		System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
	}

	public static ExecutorService replacePool(@NonNull ExecutorService service) {
		if (!(service instanceof ForkJoinPool)) {
			System.out.println(service + " is not an instance of ForkJoinPool, skipping");
			return service;
		}

		int originalCount = ((ForkJoinPool) service).getParallelism();

		System.out.println("Original size of the pool " + poolName + ": " + originalCount);

		int customCount = getCustomCount(poolName);

		if (customCount < 0) {
			System.out.println("Custom pool size not set");
		} else if (customCount == originalCount) {
			System.out.println("Custom pool size is equal to original size");
		} else {
			System.out.println("Using pool size " + customCount);

			return new ForkJoinPool(
					customCount,
					((ForkJoinPool) service).getFactory(),
					((ForkJoinPool) service).getUncaughtExceptionHandler(),
					((ForkJoinPool) service).getAsyncMode()
			);
		}

		return service;
	}

	private static int getCustomCount(String poolName) {
		if (poolName != null) {
			// Get setting for specific pool, if present
			int count = Integer.getInteger("minecraft" + poolName + "ThreadPoolSize", -1);

			if (count != -1) {
				System.out.println("Custom size for pool " + poolName + " is set to " + count);

				return count;
			}
		}

		// Get setting for any pool
		return Integer.getInteger("minecraftThreadPoolSize", -1);
	}

	public static void startProfiling() {
		start = System.nanoTime();
	}

	public static void endProfiling() {
		long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

		//noinspection RedundantStringFormatCall
		System.out.println(String.format(Locale.ROOT, "Done initial reload of resource manager, %.2f sec passed since start", ms / 1000.0));
	}

}
