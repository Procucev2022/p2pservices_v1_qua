package com.portal.procucev.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "subscription_plan")
public class SubscriptionPlan extends Procucev {

	private static final long serialVersionUID = 1L;

	@Column(name = "plan_name")
	private String planName; // Connect, Select, Elect

	@Column(name = "rfq_notification")
	private String rfqNotification; // YES / NO

	@Column(name = "category_wise_rfq_status")
	private String categoryWiseRfqStatus; // YES / NO

	@Column(name = "view_all_rfqs")
	private String viewAllRfqs; // YES / NO

	@Column(name = "whatsapp_ai_assistant")
	private String whatsappAiAssistant; // YES / NO

	@Column(name = "automated_ai_quotation")
	private String automatedAiQuotation; // YES / NO

	@Column(name = "product_listing_with_price")
	private String productListingWithPrice; // YES / NO

	@Column(name = "catalogue_listing")
	private String catalogueListing; // YES / NO

	@Column(name = "max_catalogue_products")
	private int maxCatalogueProducts;

	@Column(name = "real_time_negotiation")
	private String realTimeNegotiation; // YES / NO

	@Column(name = "product_listing_with_quantity")
	private String productListingWithQuantity; // YES / NO

	@Column(name = "dedicated_account_manager")
	private String dedicatedAccountManager; // YES / NO

	@Column(name = "buyer_contact_details_time")
	private String buyerContactDetailsTime;

	@Column(name = "rfq_bundle_size")
	private int rfqBundleSize;

	@Column(name = "subscription_period_months")
	private int subscriptionPeriodMonths;

	@Column(name = "subscription_price")
	private double subscriptionPrice;

	@Column(name = "launch_offer_price")
	private double launchOfferPrice;

	@Column(name = "launched_status")
	private String launchedStatus; // YES / NO

}