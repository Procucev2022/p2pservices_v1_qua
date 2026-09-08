package com.portal.procucev.rfq;

import com.portal.procucev.rfq.service.GeminiPricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeminiPricingServiceTest {

    private GeminiPricingService service;

    @BeforeEach
    void setUp() {
        service = new GeminiPricingService();
    }

    @Test
    void testRatesForFlashLiteModels() {
        assertEquals(0.0375, GeminiPricingService.getPromptRate("gemini-3.5-flash-lite"));
        assertEquals(0.15, GeminiPricingService.getCandidateRate("gemini-3.5-flash-lite"));

        assertEquals(0.0375, GeminiPricingService.getPromptRate("gemini-1.5-flash-8b"));
        assertEquals(0.15, GeminiPricingService.getCandidateRate("gemini-1.5-flash-8b"));
    }

    @Test
    void testRatesForGemini37Flash() {
        assertEquals(0.10, GeminiPricingService.getPromptRate("gemini-3.7-flash"));
        assertEquals(0.40, GeminiPricingService.getCandidateRate("gemini-3.7-flash"));
    }

    @Test
    void testRatesForStandardFlash() {
        assertEquals(0.075, GeminiPricingService.getPromptRate("gemini-2.5-flash"));
        assertEquals(0.30, GeminiPricingService.getCandidateRate("gemini-2.5-flash"));

        assertEquals(0.075, GeminiPricingService.getPromptRate("gemini-3.6-flash"));
        assertEquals(0.30, GeminiPricingService.getCandidateRate("gemini-3.6-flash"));

        assertEquals(0.075, GeminiPricingService.getPromptRate(null));
        assertEquals(0.30, GeminiPricingService.getCandidateRate(null));

        assertEquals(0.075, GeminiPricingService.getPromptRate("unknown-model"));
        assertEquals(0.30, GeminiPricingService.getCandidateRate("unknown-model"));
    }

    @Test
    void testRatesForProModels() {
        assertEquals(1.25, GeminiPricingService.getPromptRate("gemini-1.5-pro"));
        assertEquals(5.00, GeminiPricingService.getCandidateRate("gemini-1.5-pro"));

        assertEquals(1.25, GeminiPricingService.getPromptRate("gemini-2.5-pro"));
        assertEquals(5.00, GeminiPricingService.getCandidateRate("gemini-2.5-pro"));
    }

    @Test
    void testCalculateCostUsd() {
        double costLite = GeminiPricingService.calculateCostUsd("gemini-3.5-flash-lite", 1000, 500);
        assertEquals(0.0001125, costLite, 0.0000001);

        double costStandard = GeminiPricingService.calculateCostUsd("gemini-2.5-flash", 1430, 475);
        assertEquals(0.00024975, costStandard, 0.0000001);

        double cost37 = GeminiPricingService.calculateCostUsd("gemini-3.7-flash", 1000, 500);
        assertEquals(0.000300, cost37, 0.0000001);

        double costPro = GeminiPricingService.calculateCostUsd("gemini-1.5-pro", 1000, 500);
        assertEquals(0.00375, costPro, 0.0000001);
    }

    @Test
    void testCalculateCostInrAndExchangeRates() {
        double inrDefault = GeminiPricingService.calculateCostInr(1.0, 0);
        assertEquals(86.50, inrDefault, 0.001);

        double inrCustom = GeminiPricingService.calculateCostInr(10.0, 85.0);
        assertEquals(850.0, inrCustom, 0.001);

        assertEquals(86.50, service.getUsdToInrRate());
        service.setUsdToInrRate(87.0);
        assertEquals(87.0, service.getUsdToInrRate());
        assertEquals(87.0, service.toInr(1.0), 0.001);

        service.setUsdToInrRate(-5.0);
        assertEquals(86.50, service.getUsdToInrRate());
    }

    @Test
    void testFormatting() {
        assertEquals("$0.0000", GeminiPricingService.formatCostUsd(0.0));
        assertEquals("$0.0002", GeminiPricingService.formatCostUsd(0.00024975));

        assertEquals("₹0.00", GeminiPricingService.formatCostInr(0.0));
        assertEquals("₹0.00", GeminiPricingService.formatCostInr(-0.01));
        assertEquals("₹0.0043", GeminiPricingService.formatCostInr(0.004325));
        assertEquals("₹0.02", GeminiPricingService.formatCostInr(0.0216));
        assertEquals("₹86.50", GeminiPricingService.formatCostInr(86.50));
    }
}
