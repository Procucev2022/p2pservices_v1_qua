package com.portal.procucev.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="subscription_plan")
public class SubscriptionPlan extends Procucev {
	    private String planName;    // Connect, Select, Elect
	    private boolean rfqClarification;
	    private boolean rfqExpiryAlert;
	    private boolean accumulatedRfqStatus;
	    private String analyticsLevel; // Basic, Regular, Advanced
	    private boolean automatedQuotation;
	    private String buyerVisibility; // e.g., "72 hours", "24 hours", "Instant"
	    private boolean catalogueCreation;
	    private boolean dedicatedSupport;
	    private boolean negotiationSupport;
	    private boolean industryInsights;
	    private double perRfqPrice;

	    // Getters and Setters
	
}
