package build.archipelago.common.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BlockingExecutorServiceFactory implements ExecutorServiceFactory {

    public static final long DEFAULT_KEEP_ALIVE_SECS = Long.MAX_VALUE;
    public static final int DEFAULT_MAX_POOL_SIZE = 30;
    private int maximumPoolSize = DEFAULT_MAX_POOL_SIZE;
    public static final int DEFAULT_QUEUE_CAPACITY = 100;
    private int queueCapacity = DEFAULT_QUEUE_CAPACITY;
    private int blockTimeout = 0; // by default we'll block indefinitely
    private TimeUnit blockTimeoutUnit = TimeUnit.SECONDS;
    private ThreadFactory threadFactory = Executors.defaultThreadFactory();

    @Override
    public ExecutorService create() {
        return new ThreadPoolExecutor(getMaximumPoolSize(), getMaximumPoolSize(),
                DEFAULT_KEEP_ALIVE_SECS, TimeUnit.SECONDS,
                new RealBlockingQueue<Runnable>(getQueueCapacity(), blockTimeout, blockTimeoutUnit),
                threadFactory);
    }

    /**
     * ThreadPoolExecutor's parameter (see
     * https://docs.oracle.com/javase/7/docs/api
     * /java/util/concurrent/ThreadPoolExecutor.html)
     */
    public void setThreadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    /**
     * ThreadPoolExecutor's parameter (see
     * http://docs.oracle.com/javase/6/docs/api
     * /java/util/concurrent/ThreadPoolExecutor.html)
     */
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(final int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    /**
     * ThreadPoolExecutor's parameter (see
     * http://docs.oracle.com/javase/6/docs/api
     * /java/util/concurrent/ThreadPoolExecutor.html)
     */
    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(final int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    /**
     * The timeout to accept a new task when it is submitted to the executor. Applied when the queue of the
     * executor is full, the executor will wait this much time before throwing
     * a {@link java.util.concurrent.RejectedExecutionException}.
     *
     * If it's greater than 0, the timeout will be applied, otherwise the queue will block
     * indefinitely if it's full and we try to insert an item.
     */
    public int getBlockTimeout() {
        return blockTimeout;
    }

    /**
     * @param blockTimeout If it's greater than 0, the timeout will be applied, otherwise the queue will block
     *                     indefinitely if it's full and we try to insert an item.
     */
    public void setBlockTimeout(int blockTimeout) {
        this.blockTimeout = blockTimeout;
    }

    public TimeUnit getBlockTimeoutUnit() {
        return blockTimeoutUnit;
    }

    public void setBlockTimeoutUnit(TimeUnit blockTimeoutUnit) {
        this.blockTimeoutUnit = blockTimeoutUnit;
    }
}
