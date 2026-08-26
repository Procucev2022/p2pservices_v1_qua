package com.portal.procucev.utils;

/**
 * Runs a task on another thread with the web application's class loader restored.
 *
 * <p>Startup jobs are dispatched with {@code CompletableFuture.runAsync}, which uses
 * {@code ForkJoinPool.commonPool()}. Those workers are created by the JVM rather than by the
 * container, so in a WAR deployment they do not inherit the web application's class loader. Any
 * lookup that goes through the thread context class loader then resolves against the system class
 * loader and cannot see anything packaged under {@code WEB-INF/classes} or {@code WEB-INF/lib}.
 *
 * <p>That is not a theoretical concern: it is why the RFQ extraction prompt failed to load with
 * "class path resource [prompts/rfq_prompt.txt] cannot be opened because it does not exist" on a
 * {@code ForkJoinPool.commonPool-worker} thread while loading normally on the {@code scheduling-*}
 * threads. It also affects {@code ServiceLoader} discovery, which Apache POI, PDFBox and Jakarta
 * Mail all rely on when parsing attachments.
 *
 * <p>The class loader is captured on the dispatching thread and always restored afterwards, because
 * common-pool workers are shared across the whole JVM and must not be left reassigned.
 */
public final class ContextClassLoaderRunnable implements Runnable {

    private final ClassLoader contextClassLoader;
    private final Runnable delegate;

    private ContextClassLoaderRunnable(ClassLoader contextClassLoader, Runnable delegate) {
        this.contextClassLoader = contextClassLoader;
        this.delegate = delegate;
    }

    /**
     * Wraps {@code delegate} so it runs with the calling thread's context class loader.
     *
     * <p>Call this on the thread that owns the correct class loader, normally the thread handling
     * application startup, and hand the result to the executor.
     */
    public static Runnable preservingContextClassLoader(Runnable delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        return new ContextClassLoaderRunnable(Thread.currentThread().getContextClassLoader(), delegate);
    }

    @Override
    public void run() {
        Thread current = Thread.currentThread();
        ClassLoader original = current.getContextClassLoader();
        boolean replaced = false;
        try {
            if (contextClassLoader != null && contextClassLoader != original) {
                current.setContextClassLoader(contextClassLoader);
                replaced = true;
            }
            delegate.run();
        } finally {
            if (replaced) {
                current.setContextClassLoader(original);
            }
        }
    }
}
