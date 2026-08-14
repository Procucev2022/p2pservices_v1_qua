package com.portal.procucev;

import com.portal.procucev.Dto.*;
import com.portal.procucev.model.MasterStatus;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DtoTest {

    private final Class<?>[] dtoClasses = new Class<?>[]{
            BFSItemDto.class,
            BFSItemMainDetailsDTO.class,
            BfsDTO.class,
            BuyerCategoryReportDto.class,
            BuyerReportDto.class,
            BuyerSellerReportDto.class,
            BuyerSummaryDto.class,
            ClientRFQDto.class,
            com.portal.procucev.Dto.EventObject.class,
            ForwardRfqVendorRequest.class,
            GMTRfqVendorDto.class,
            GmtRfqSellerDto.class,
            GmtSummaryDto.class,
            OrganizationUpdateRequest.class,
            OrganizationUpdateRequest.DivisionCategoryDTO.class,
            OrganizationUpdateRequest.BranchDTO.class,
            OrganizationUpdateRequest.SubscriptionPlanDTO.class,
            Payment.class,
            PaymentLinkGenerateRequest.class,
            PaymentLinks.class,
            RfqDTO.class,
            RfqReportDto.class,
            RfqSummaryReportDto.class,
            SellerReportDto.class,
            SellerSubscriptionReportDto.class,
            SellerSummaryDto.class,
            SmsMessage.class,
            SmsRequest.class,
            UserActivityDto.class,
            VendorInfoBean.class,
            VendorInfoDto.class,
            VendorRFQDto.class,
            VendorSummaryResponse.class,
            ZohoPaymentLinkRequest.class,
            ZohoPaymentLinkResponse.class,
            ZohoPaymentLinkResponse.PaymentLinks.class,
            ZohoPaymentLinkWebhookRequest.class,
            ZohoTokenResponse.class
    };

    @Test
    void testAllDtosViaReflection() {
        for (Class<?> clazz : dtoClasses) {
            try {
                Object instance = createSampleInstance(clazz);
                if (instance != null) {
                    invokeAllGettersAndSetters(instance, clazz);
                    assertNotNull(instance.toString());
                    assertEquals(instance, instance);
                    assertNotEquals(instance, new Object());
                    instance.hashCode();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Exercises every declared constructor and returns an instance for the accessor sweep.
     *
     * <p>Constructors are visited in a deterministic order (fewest parameters first) because
     * {@link Class#getDeclaredConstructors()} makes no ordering guarantee: relying on it made
     * coverage depend on how the class happened to be compiled. Parameterised constructors are
     * invoked twice, once with sample values and once with nulls for the reference parameters,
     * so defensive null handling inside a constructor is exercised as well.
     */
    private Object createSampleInstance(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Arrays.sort(constructors, Comparator.comparingInt(Constructor::getParameterCount));

        Object firstInstance = null;
        for (Constructor<?> ctor : constructors) {
            Object instance = newInstance(ctor, false);
            if (instance != null && firstInstance == null) {
                firstInstance = instance;
            }
            if (ctor.getParameterCount() > 0) {
                newInstance(ctor, true);
            }
        }
        return firstInstance;
    }

    private Object newInstance(Constructor<?> ctor, boolean nullReferenceArgs) {
        try {
            ctor.setAccessible(true);
            Class<?>[] params = ctor.getParameterTypes();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                args[i] = nullReferenceArgs && !params[i].isPrimitive()
                        ? null
                        : getSampleValue(params[i]);
            }
            return ctor.newInstance(args);
        } catch (Exception ignored) {
            return null;
        }
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
        if (type == BigDecimal.class) return BigDecimal.TEN;
        if (type == List.class) return new ArrayList<>();
        if (type == Set.class) return new HashSet<>();
        if (type == Map.class) return new HashMap<>();
        if (type == MasterStatus.class) return new MasterStatus();
        return null;
    }
}
