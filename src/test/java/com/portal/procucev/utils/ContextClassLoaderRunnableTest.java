package com.portal.procucev.utils;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ContextClassLoaderRunnableTest {

    @Test
    void testNullDelegateThrows() {
        assertThrows(IllegalArgumentException.class, () -> ContextClassLoaderRunnable.preservingContextClassLoader(null));
    }

    @Test
    void testPreservesAndRestoresClassLoaderWhenDifferent() {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        ClassLoader custom = new URLClassLoader(new URL[0], original);

        Thread.currentThread().setContextClassLoader(custom);
        Runnable runnable = ContextClassLoaderRunnable.preservingContextClassLoader(() -> {
            assertEquals(custom, Thread.currentThread().getContextClassLoader());
        });

        // Change context classloader before running
        Thread.currentThread().setContextClassLoader(original);
        runnable.run();
        assertEquals(original, Thread.currentThread().getContextClassLoader());
    }

    @Test
    void testSameClassLoaderDoesNotReplace() {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable runnable = ContextClassLoaderRunnable.preservingContextClassLoader(() -> ran.set(true));

        runnable.run();
        assertTrue(ran.get());
        assertEquals(original, Thread.currentThread().getContextClassLoader());
    }

    @Test
    void testNullContextClassLoaderBranch() throws Exception {
        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable runnable = ContextClassLoaderRunnable.preservingContextClassLoader(() -> ran.set(true));
        ReflectionTestUtils.setField(runnable, "contextClassLoader", null);

        runnable.run();
        assertTrue(ran.get());
    }
}
