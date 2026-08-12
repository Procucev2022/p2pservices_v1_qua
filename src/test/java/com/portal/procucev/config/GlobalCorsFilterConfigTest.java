package com.portal.procucev.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.filter.CorsFilter;

class GlobalCorsFilterConfigTest {

    @Test
    void testCustomCorsFilterRegistration() {
        GlobalCorsFilterConfig config = new GlobalCorsFilterConfig();
        FilterRegistrationBean<CorsFilter> registrationBean = config.customCorsFilter();

        assertNotNull(registrationBean);
        assertNotNull(registrationBean.getFilter());
        assertEquals(Ordered.HIGHEST_PRECEDENCE, registrationBean.getOrder());
    }
}
