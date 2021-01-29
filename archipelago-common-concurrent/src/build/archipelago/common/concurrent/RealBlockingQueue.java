package build.archipelago.common.concurrent;

import java.util.concurrent.*;

/**
 * A blocking queue implementation that turns also the offer() method
 * calls into put() or offer(e, timeout) calls.
 * <p>
 * The main purpose is to easily turn a ThreadPoolExecutor into a real
 * blocking executor that blocks on the submit(Runnable) calls (if all the
 * workers are busy and the queue is full) instead of throwing a
 * RejectedExecutionException.
 *
 * @param <E>
 */
public class RealBlockingQueue<E> extends ArrayBlockingQueue<E> {
    private final int offerTimeout;
    private final TimeUnit offerTimeoutUnit;

    /**
     * The queue will block, without any timeout.
     *
     * @param capacity
     */
    public RealBlockingQueue(int capacity) {
        this(capacity, 0, TimeUnit.SECONDS);
    }

    /**
     * @param capacity         The capacity of the queue.
     * @param offerTimeout     If it's less than or equal to 0 the offer() method call will block without a timeout,
     *                         otherwise the timeout will apply.
     * @param offerTimeoutUnit Units of the timeout specified.
     */
    public RealBlockingQueue(int capacity, int offerTimeout, TimeUnit offerTimeoutUnit) {
        super(capacity);
        this.offerTimeout = offerTimeout;
        this.offerTimeoutUnit = offerTimeoutUnit;
    }

    @Override
    public boolean offer(E e) {
        // turn offer() and add() into blocking calls (unless interrupted)
        try {
            if (this.hasTimeoutOnOffer()) {
                return this.offer(e, offerTimeout, offerTimeoutUnit);
            } else {
                this.put(e);
                return true;
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private boolean hasTimeoutOnOffer() {
        return offerTimeout > 0;
    }

}