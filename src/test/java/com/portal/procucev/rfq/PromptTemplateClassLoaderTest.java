package com.portal.procucev.rfq;

import com.portal.procucev.rfq.client.GeminiApiClient;
import com.portal.procucev.rfq.model.EmailData;
import com.portal.procucev.rfq.model.ExtractedRFQ;
import com.portal.procucev.rfq.service.AIExtractionService;
import com.portal.procucev.utils.ContextClassLoaderRunnable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Regression cover for the prompt template failing to load on a {@code ForkJoinPool.commonPool}
 * worker.
 *
 * <p>Observed in production as "class path resource [prompts/rfq_prompt.txt] cannot be opened
 * because it does not exist" on {@code onPool-worker-2}, while the same resource loaded normally on
 * the container's {@code scheduling-*} threads. The resource was packaged correctly; the lookup was
 * going through the thread context class loader, which those JVM-created workers do not inherit from
 * the web application in a WAR deployment.
 */
class PromptTemplateClassLoaderTest {

    /** Sees no classpath at all, standing in for a worker thread outside the webapp class loader. */
    private static final ClassLoader ISOLATED = new URLClassLoader(new URL[0], null);

    private ClassLoader originalContextClassLoader;
    private GeminiApiClient geminiApiClient;
    private AIExtractionService aiExtractionService;

    @BeforeEach
    void setUp() {
        originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        geminiApiClient = Mockito.mock(GeminiApiClient.class);
        aiExtractionService = new AIExtractionService(geminiApiClient);
    }

    @AfterEach
    void tearDown() {
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
    }

    @Test
    @DisplayName("The isolated loader genuinely cannot see the prompt, so the test below is meaningful")
    void isolatedClassLoaderCannotSeeThePrompt() {
        assertNull(ISOLATED.getResource(AIExtractionService.PROMPT_TEMPLATE_PATH),
                "the stand-in loader must not see the resource, otherwise it proves nothing");
        assertNotNull(AIExtractionService.class.getClassLoader().getResource(AIExtractionService.PROMPT_TEMPLATE_PATH),
                "the prompt must be packaged on the class's own loader");
    }

    /**
     * Before the fix this threw, because the single-argument {@code ClassPathResource} constructor
     * resolves through the thread context class loader.
     */
    @Test
    @DisplayName("Extraction loads the prompt even when the thread context class loader cannot see it")
    void extractionLoadsPromptWithUnrelatedContextClassLoader() {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"buyerEmail\":\"buyer@test.com\",\"items\":"
                        + "[{\"itemDescription\":\"Aluminum Washer\",\"quantity\":40000.0,\"uom\":\"nos\"}]}");

        Thread.currentThread().setContextClassLoader(ISOLATED);

        ExtractedRFQ rfq = aiExtractionService.extractRFQFromEmail(EmailData.builder()
                .subject("Requirement for Aluminum washers")
                .body("Our monthly requirement is 40,000 nos.")
                .senderEmail("vegasystems.bgm@gmail.com")
                .build());

        assertNotNull(rfq);
        assertEquals(1, rfq.getItems().size());
        assertEquals(40000.0, rfq.getItems().get(0).getQuantity());
    }

    @Test
    @DisplayName("Prompt still loads on a real ForkJoinPool.commonPool worker, as the startup run uses")
    void extractionLoadsPromptOnCommonPoolWorker() throws Exception {
        Mockito.when(geminiApiClient.generateContent(anyString(), anyList()))
                .thenReturn("{\"buyerEmail\":\"buyer@test.com\",\"items\":"
                        + "[{\"itemDescription\":\"Washer\",\"quantity\":10.0,\"uom\":\"nos\"}]}");

        EmailData email = EmailData.builder().subject("Washers").senderEmail("buyer@test.com").build();

        ExtractedRFQ rfq = CompletableFuture
                .supplyAsync(() -> aiExtractionService.extractRFQFromEmail(email))
                .get();

        assertNotNull(rfq);
        assertEquals(1, rfq.getItems().size());
    }

    @Test
    @DisplayName("preservingContextClassLoader applies the dispatching loader inside the worker")
    void wrapperAppliesDispatchingClassLoader() throws Exception {
        Thread.currentThread().setContextClassLoader(ISOLATED);
        AtomicReference<ClassLoader> seenInsideWorker = new AtomicReference<>();

        Runnable task = ContextClassLoaderRunnable.preservingContextClassLoader(
                () -> seenInsideWorker.set(Thread.currentThread().getContextClassLoader()));

        CompletableFuture.runAsync(task).get();

        assertSame(ISOLATED, seenInsideWorker.get(),
                "the task must observe the class loader captured at dispatch time");
    }

    @Test
    @DisplayName("preservingContextClassLoader restores the worker's original loader, even on failure")
    void wrapperRestoresOriginalClassLoader() throws Exception {
        Thread.currentThread().setContextClassLoader(ISOLATED);

        Runnable throwing = ContextClassLoaderRunnable.preservingContextClassLoader(() -> {
            throw new IllegalStateException("boom");
        });

        // Common-pool workers are shared JVM-wide, so a leaked class loader would corrupt unrelated
        // tasks. Run on a thread whose loader is known and assert it is put back.
        AtomicReference<ClassLoader> afterRun = new AtomicReference<>();
        ClassLoader workerOriginal = AIExtractionService.class.getClassLoader();
        Thread worker = new Thread(() -> {
            Thread.currentThread().setContextClassLoader(workerOriginal);
            try {
                throwing.run();
            } catch (IllegalStateException expected) {
                // the delegate is expected to fail; the finally block must still restore
            }
            afterRun.set(Thread.currentThread().getContextClassLoader());
        });
        worker.start();
        worker.join();

        assertSame(workerOriginal, afterRun.get());
    }

    @Test
    @DisplayName("preservingContextClassLoader rejects a null delegate")
    void wrapperRejectsNullDelegate() {
        assertThrows(IllegalArgumentException.class,
                () -> ContextClassLoaderRunnable.preservingContextClassLoader(null));
    }
}
