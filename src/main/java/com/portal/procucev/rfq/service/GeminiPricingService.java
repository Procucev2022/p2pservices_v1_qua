package com.portal.procucev.rfq.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to calculate estimated token consumption costs based on model-specific market rates
 * and convert costs to Indian Rupees (INR).
 */
@Service
public class GeminiPricingService {

    public static final double DEFAULT_USD_TO_INR_RATE = 86.50;

    // Flash-Lite tier (gemini-3.5-flash-lite, gemini-3.1-flash-lite, gemini-2.0-flash-lite, gemini-1.5-flash-8b)
    public static final double FLASH_LITE_PROMPT_RATE = 0.0375;
    public static final double FLASH_LITE_CANDIDATE_RATE = 0.15;

    // Gemini 3.7 Flash tier
    public static final double GEMINI_37_FLASH_PROMPT_RATE = 0.10;
    public static final double GEMINI_37_FLASH_CANDIDATE_RATE = 0.40;

    // Standard Flash tier (gemini-2.5-flash, gemini-3.6-flash, gemini-3.5-flash, gemini-2.0-flash, gemini-1.5-flash)
    public static final double STANDARD_FLASH_PROMPT_RATE = 0.075;
    public static final double STANDARD_FLASH_CANDIDATE_RATE = 0.30;

    // Pro tier (gemini-1.5-pro, gemini-2.5-pro, gemini-pro)
    public static final double PRO_PROMPT_RATE = 1.25;
    public static final double PRO_CANDIDATE_RATE = 5.00;

    @Value("${app.gemini.usd-to-inr-rate:86.50}")
    private double usdToInrRate = DEFAULT_USD_TO_INR_RATE;

    public double getUsdToInrRate() {
        return usdToInrRate > 0 ? usdToInrRate : DEFAULT_USD_TO_INR_RATE;
    }

    public void setUsdToInrRate(double usdToInrRate) {
        this.usdToInrRate = usdToInrRate;
    }

    /**
     * Obtains the prompt rate per 1M tokens for the given model name.
     *
     * @param modelName the Gemini model identifier
     * @return prompt rate in USD per 1M tokens
     */
    public static double getPromptRate(String modelName) {
        if (modelName == null) {
            return STANDARD_FLASH_PROMPT_RATE;
        }
        String lower = modelName.toLowerCase(Locale.ROOT);
        if (lower.contains("lite") || lower.contains("8b")) {
            return FLASH_LITE_PROMPT_RATE;
        }
        if (lower.contains("3.7-flash")) {
            return GEMINI_37_FLASH_PROMPT_RATE;
        }
        if (lower.contains("pro")) {
            return PRO_PROMPT_RATE;
        }
        return STANDARD_FLASH_PROMPT_RATE;
    }

    /**
     * Obtains the candidate (output) rate per 1M tokens for the given model name.
     *
     * @param modelName the Gemini model identifier
     * @return candidate rate in USD per 1M tokens
     */
    public static double getCandidateRate(String modelName) {
        if (modelName == null) {
            return STANDARD_FLASH_CANDIDATE_RATE;
        }
        String lower = modelName.toLowerCase(Locale.ROOT);
        if (lower.contains("lite") || lower.contains("8b")) {
            return FLASH_LITE_CANDIDATE_RATE;
        }
        if (lower.contains("3.7-flash")) {
            return GEMINI_37_FLASH_CANDIDATE_RATE;
        }
        if (lower.contains("pro")) {
            return PRO_CANDIDATE_RATE;
        }
        return STANDARD_FLASH_CANDIDATE_RATE;
    }

    /**
     * Calculates the estimated cost in USD based on model-specific prompt and candidate rates.
     *
     * @param modelName the model used for extraction
     * @param promptTokens prompt tokens consumed
     * @param candidateTokens candidate tokens generated
     * @return total estimated cost in USD
     */
    public static double calculateCostUsd(String modelName, int promptTokens, int candidateTokens) {
        double promptRate = getPromptRate(modelName);
        double candidateRate = getCandidateRate(modelName);
        return ((promptTokens * promptRate) + (candidateTokens * candidateRate)) / 1_000_000.0;
    }

    /**
     * Converts USD cost to INR using specified exchange rate.
     *
     * @param costUsd cost in USD
     * @param exchangeRate exchange rate (USD to INR)
     * @return cost in INR
     */
    public static double calculateCostInr(double costUsd, double exchangeRate) {
        double rate = exchangeRate > 0 ? exchangeRate : DEFAULT_USD_TO_INR_RATE;
        return costUsd * rate;
    }

    /**
     * Converts USD cost to INR using the configured instance exchange rate.
     *
     * @param costUsd cost in USD
     * @return cost in INR
     */
    public double toInr(double costUsd) {
        return calculateCostInr(costUsd, getUsdToInrRate());
    }

    /**
     * Formats USD cost as a standard currency string (e.g., "$0.0002").
     *
     * @param costUsd cost in USD
     * @return formatted USD string
     */
    public static String formatCostUsd(double costUsd) {
        return String.format(Locale.US, "$%.4f", costUsd);
    }

    /**
     * Formats INR cost as a standard currency string (e.g., "₹0.02").
     *
     * @param costInr cost in INR
     * @return formatted INR string
     */
    public static String formatCostInr(double costInr) {
        if (costInr <= 0.0) {
            return "₹0.00";
        }
        if (costInr < 0.01) {
            return String.format(Locale.US, "₹%.4f", costInr);
        }
        return String.format(Locale.US, "₹%.2f", costInr);
    }
}
