-- Buyer sourcing plans (Version 1 / 2 / 3).
--
-- Why this is needed: the buyer Sourcing Subscriptions page reads plan copy from
-- SubscriptionPlanCatalogue, but a payment link can only be created for a plan
-- that exists as a row in subscription_plan. PaymentLinkService looks the plan up
-- by id and prices from launch_offer_price / subscription_price, so without these
-- rows the Subscribe button has no payable plan behind it.
--
-- The uuid values must be numeric: PaymentLinkGenerateRequest.planId is a Long
-- and PaymentLinkService calls findById(String.valueOf(planId)). Seller plans
-- already occupy 2001-2003, so buyer plans use 1001-1003.
--
-- plan_name must begin with "Version 1", "Version 2" or "Version 3" so
-- SubscriptionPlanAudience classifies these as buyer plans and
-- SubscriptionPlanCatalogue attaches the approved copy.
--
-- Prices below are placeholders taken from the design deck. Replace them with the
-- agreed commercial figures before running in production. GST is added at payment
-- time by PaymentLinkService (18%), so store the pre-tax amount here.

INSERT INTO subscription_plan (
    uuid,
    plan_name,
    rfq_notification,
    category_wise_rfq_status,
    view_all_rfqs,
    whatsapp_ai_assistant,
    automated_ai_quotation,
    product_listing_with_price,
    catalogue_listing,
    max_catalogue_products,
    real_time_negotiation,
    product_listing_with_quantity,
    dedicated_account_manager,
    buyer_contact_details_time,
    rfq_bundle_size,
    subscription_period_months,
    subscription_price,
    launch_offer_price,
    plan_status,
    launched_status,
    created_by,
    created_ts
) VALUES
-- Version 1: roster-based chasing
('1001', 'Version 1 Sourcing',
 'YES', 'YES', 'YES', 'YES',
 'NO', 'NO', 'NO', 0,
 'NO', 'NO', 'NO', NULL,
 25, 1,
 16500.00, 0.00,
 'Active', 'YES',
 'seed-script', NOW()),

-- Version 2: hybrid sourced network
('1002', 'Version 2 Sourcing',
 'YES', 'YES', 'YES', 'YES',
 'YES', 'NO', 'NO', 0,
 'NO', 'NO', 'NO', NULL,
 75, 1,
 41500.00, 0.00,
 'Active', 'YES',
 'seed-script', NOW()),

-- Version 3: autonomous sourcing desk
('1003', 'Version 3 Sourcing',
 'YES', 'YES', 'YES', 'YES',
 'YES', 'YES', 'NO', 0,
 'YES', 'YES', 'YES', NULL,
 200, 1,
 83000.00, 0.00,
 'Active', 'YES',
 'seed-script', NOW())

ON DUPLICATE KEY UPDATE
    plan_name = VALUES(plan_name),
    rfq_bundle_size = VALUES(rfq_bundle_size),
    subscription_period_months = VALUES(subscription_period_months),
    subscription_price = VALUES(subscription_price),
    launch_offer_price = VALUES(launch_offer_price),
    plan_status = VALUES(plan_status),
    launched_status = VALUES(launched_status),
    last_modified_ts = NOW();

-- Verify
SELECT uuid, plan_name, subscription_price, launch_offer_price,
       rfq_bundle_size, subscription_period_months, plan_status, launched_status
FROM subscription_plan
ORDER BY uuid;
