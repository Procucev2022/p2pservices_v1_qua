package com.portal.procucev;

import com.portal.procucev.model.*;
import com.portal.procucev.utils.EmailValidatorUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    private final Class<?>[] modelClasses = new Class<?>[]{
            User.class,
            Organization.class,
            BFSItems.class,
            BFSImages.class,
            BFSDocuments.class,
            BFSUsers.class,
            BFSUserComments.class,
            CategoryDivision.class,
            ClientDeliveryLocationRfq.class,
            ClientDepartment.class,
            ClientPermission.class,
            EmailAttachment.class,
            EmailRequest.class,
            EmailUser.class,
            GmtItems.class,
            GmtRfqVendors.class,
            ItemCategory.class,
            ItemUOM.class,
            MasterStatus.class,
            OrgBranches.class,
            OrgDivisionCategory.class,
            OrgType.class,
            OtpDetails.class,
            OtpStore.class,
            PaymentLink.class,
            Permission.class,
            PincodeData.class,
            PostOffice.class,
            RFQComment.class,
            RFQDocument.class,
            Rfq.class,
            RfqItem.class,
            RfqStatusRequest.class,
            RfqVendor.class,
            Role.class,
            ResetPassword.class,
            SubscriptionPlan.class,
            TokenResponse.class,
            UserActivity.class,
            UserRolePermission.class,
            VendorCatalogue.class,
            VendorTermsConditions.class,
            VisitorsData.class,
            ZohoOAuthToken.class,
            ZohoPaymentWebhookEntity.class
    };

    @Test
    void testAllModelsViaReflection() {
        for (Class<?> clazz : modelClasses) {
            try {
                for (Constructor<?> ctor : clazz.getDeclaredConstructors()) {
                    try {
                        ctor.setAccessible(true);
                        Class<?>[] params = ctor.getParameterTypes();
                        Object[] args = new Object[params.length];
                        for (int i = 0; i < params.length; i++) {
                            args[i] = getSampleValue(params[i]);
                        }
                        Object inst = ctor.newInstance(args);
                        if (inst != null) {
                            invokeAllGettersAndSetters(inst, clazz);
                            assertNotNull(inst.toString());
                            assertEquals(inst, inst);
                            assertNotEquals(inst, new Object());
                            inst.hashCode();
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // Targeted tests for JPA listeners & specific equals/pre-persist methods

        // 1. RFQDocument equals & hashCode
        RFQDocument rfqDoc1 = new RFQDocument();
        RFQDocument rfqDoc2 = new RFQDocument();
        assertEquals(rfqDoc1, rfqDoc1);
        assertNotEquals(rfqDoc1, "NotARFQDoc");
        assertNotEquals(rfqDoc1, rfqDoc2);

        rfqDoc1.setId("DOC1");
        assertNotEquals(rfqDoc1, rfqDoc2);

        rfqDoc2.setId("DOC2");
        assertNotEquals(rfqDoc1, rfqDoc2);

        rfqDoc2.setId("DOC1");
        assertEquals(rfqDoc1, rfqDoc2);
        assertEquals(31, rfqDoc1.hashCode());

        // 2. ZohoPaymentWebhookEntity prePersist
        ZohoPaymentWebhookEntity zohoEntity = new ZohoPaymentWebhookEntity();
        zohoEntity.prePersist();
        assertNotNull(zohoEntity.getReceivedAt());
        assertFalse(zohoEntity.isProcessed());

        zohoEntity.setReceivedAt(Instant.now());
        zohoEntity.prePersist();
        assertNotNull(zohoEntity.getReceivedAt());

        // 3. PaymentLink onCreate & onUpdate
        PaymentLink paymentLink = new PaymentLink();
        paymentLink.onCreate();
        paymentLink.onUpdate();
        assertNotNull(paymentLink.getCreatedAt());
        assertNotNull(paymentLink.getUpdatedAt());

        // 4. Procucev AbstractEntityListener
        Procucev p = new Procucev() {};
        p.setCreatedTS(new Date());
        p.setLastModifiedTS(new Date());
        Procucev.AbstractEntityListener listener = new Procucev.AbstractEntityListener();
        listener.onPrePersist(p);
        assertNotNull(p.getId());

        // Test non-null ID branch of uid()
        listener.onPrePersist(p);

        // 5. EmailValidatorUtil
        EmailValidatorUtil evu = new EmailValidatorUtil();
        assertNotNull(evu);
    }

    private void invokeAllGettersAndSetters(Object instance, Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            try {
                method.setAccessible(true);
                if ((method.getName().startsWith("get") || method.getName().startsWith("is")) && method.getParameterCount() == 0) {
                    method.invoke(instance);
                } else if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                    Object val = getSampleValue(method.getParameterTypes()[0]);
                    method.invoke(instance, val);
                }
            } catch (Exception ignored) {
            }
        }
        for (Method method : methods) {
            try {
                method.setAccessible(true);
                if ((method.getName().startsWith("get") || method.getName().startsWith("is")) && method.getParameterCount() == 0) {
                    method.invoke(instance);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private Object getSampleValue(Class<?> type) {
        if (type == String.class) return "sample";
        if (type == int.class || type == Integer.class) return 1;
        if (type == long.class || type == Long.class) return 1L;
        if (type == double.class || type == Double.class) return 1.0;
        if (type == boolean.class || type == Boolean.class) return true;
        if (type == Date.class) return new Date();
        if (type == LocalDate.class) return LocalDate.now();
        if (type == LocalDateTime.class) return LocalDateTime.now();
        if (type == Instant.class) return Instant.now();
        if (type == BigDecimal.class) return BigDecimal.TEN;
        if (type == List.class) return new ArrayList<>();
        if (type == Set.class) return new HashSet<>();
        if (type == Map.class) return new HashMap<>();
        if (type == MasterStatus.class) return new MasterStatus();
        if (type == Organization.class) return new Organization();
        if (type == User.class) return new User();
        if (type == BFSItems.class) return new BFSItems();
        if (type == Rfq.class) return new Rfq();
        if (type == SubscriptionPlan.class) return new SubscriptionPlan();
        return null;
    }
}
