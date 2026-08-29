package com.portal.procucev.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.portal.procucev.model.SubscriptionPlan;

/**
 * Splits subscription_plan rows into buyer and seller catalogues.
 *
 * The table carries no column identifying the audience for a plan, so the
 * seller plan names are configured and matched by prefix. Plan names in the
 * data carry trailing qualifiers such as
 * "CONNECT  [ Taxes & Other convenience charges extra]", which is why this
 * matches on the leading word rather than on equality.
 */
@Component
public class SubscriptionPlanAudience {

    private final List<String> sellerPlanNames;

    public SubscriptionPlanAudience(
            @Value("${procucev.subscription.seller-plan-names:CONNECT,SELECT,ELECT}") String configured) {
        this.sellerPlanNames = Arrays.stream(configured.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    /** True when the plan is sold to vendors. */
    public boolean isSellerPlan(SubscriptionPlan plan) {
        if (plan == null || plan.getPlanName() == null) {
            return false;
        }
        String name = plan.getPlanName().trim().toUpperCase();
        return sellerPlanNames.stream().anyMatch(name::startsWith);
    }

    /** True when the plan is sold to buyers. */
    public boolean isBuyerPlan(SubscriptionPlan plan) {
        return plan != null && plan.getPlanName() != null && !isSellerPlan(plan);
    }

    /**
     * Strips the trailing bracketed qualifier from a plan name so cards can show
     * "CONNECT" rather than "CONNECT [ Taxes & Other convenience charges extra]".
     * The qualifier is surfaced separately as a note.
     */
    public String displayName(SubscriptionPlan plan) {
        if (plan == null || plan.getPlanName() == null) {
            return "";
        }
        String name = plan.getPlanName().trim();
        int bracket = name.indexOf('[');
        if (bracket > 0) {
            return name.substring(0, bracket).trim();
        }
        return name;
    }

    /** The bracketed qualifier from the plan name, or empty when absent. */
    public String priceNote(SubscriptionPlan plan) {
        if (plan == null || plan.getPlanName() == null) {
            return "";
        }
        String name = plan.getPlanName().trim();
        int open = name.indexOf('[');
        int close = name.lastIndexOf(']');
        if (open >= 0 && close > open) {
            return name.substring(open + 1, close).trim();
        }
        return "";
    }
}
