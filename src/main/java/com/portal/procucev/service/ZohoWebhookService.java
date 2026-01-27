package com.portal.procucev.service;

import com.portal.procucev.Dto.ZohoPaymentLinkWebhookRequest;

public interface ZohoWebhookService {

    void processWebhook(ZohoPaymentLinkWebhookRequest payload);
}
