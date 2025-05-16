package com.jd.live.agent.core.util.pool;

import com.jd.live.agent.core.util.pool.robust.RobustObjectPool;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

public class ObjectPoolTest {

    @Test
    void shouldCreateAndReuseObjects() {
        // Given
        AtomicInteger creationCount = new AtomicInteger();
        Supplier<TestResource> factory = () -> {
            creationCount.incrementAndGet();
            return new TestResource();
        };

        ObjectPool<TestResource> pool = new RobustObjectPool<>(factory, 2, null);

        // When
        TestResource obj1 = pool.borrow();
        TestResource obj2 = pool.borrow();
        pool.release(obj1);
        TestResource obj3 = pool.borrow();

        // Then
        assertEquals(2, creationCount.get()); // Should create only 2 objects
        assertSame(obj1, obj3); // Should reuse the released object
    }

    @Test
    void shouldValidateReturnedObjects() {
        // Given
        Predicate<TestResource> validator = obj -> obj.valid;
        RobustObjectPool<TestResource> pool = new RobustObjectPool<>(TestResource::new, 1, validator);

        // When
        TestResource validObj = pool.borrow();
        validObj.valid = false;
        pool.release(validObj);

        TestResource newObj = pool.borrow();

        // Then
        assertNotSame(validObj, newObj); // Should discard invalid object
    }

    @Test
    void shouldHandleCapacityLimits() {
        // Given
        RobustObjectPool<String> pool = new RobustObjectPool<>(() -> "object", 1, null);

        // When
        String obj1 = pool.borrow();
        String obj2 = pool.borrow(); // Should exceed capacity
        pool.release(obj1);
        String obj3 = pool.borrow();

        // Then
        assertEquals("object", obj1);
        assertEquals("object", obj2);
        assertSame(obj1, obj3); // Should reuse the released object
    }

    @Test
    void shouldTrackPoolStatus() {
        // Given
        RobustObjectPool<Integer> pool = new RobustObjectPool<>(() -> 42, 2, null);

        // When
        PoolStatus initial = pool.getStatus();
        Integer obj1 = pool.borrow();
        PoolStatus afterBorrow = pool.getStatus();
        pool.release(obj1);
        PoolStatus afterRelease = pool.getStatus();

        // Then
        assertEquals(0, initial.getPooledObjects());
        assertEquals(0, initial.getActiveObjects());

        assertEquals(0, afterBorrow.getPooledObjects());
        assertEquals(1, afterBorrow.getActiveObjects());

        assertEquals(1, afterRelease.getPooledObjects());
        assertEquals(0, afterRelease.getActiveObjects());
    }

    @Test
    void shouldHandleNullObjects() {
        // Given
        RobustObjectPool<String> pool = new RobustObjectPool<>(
                () -> "test", 1, null);

        // When
        pool.release(null); // Should not throw
        String obj = pool.borrow();

        // Then
        assertEquals("test", obj);
    }

    @Test
    void shouldHandleConcurrentAccess() throws InterruptedException {
        // Given
        int threadCount = 10;
        RobustObjectPool<Integer> pool = new RobustObjectPool<>(
                () -> 0, threadCount, null);

        // When
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                Integer obj = pool.borrow();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                pool.release(obj);
            });
            threads[i].start();
        }

        // Then
        for (Thread t : threads) {
            t.join();
        }
        PoolStatus finalStatus = pool.getStatus();
        assertEquals(threadCount, finalStatus.getPooledObjects());
        assertEquals(0, finalStatus.getActiveObjects());
    }

    // Test object class
    static class TestResource {
        boolean valid = true;
    }
}