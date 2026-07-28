package com.portal.procucev;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class ServletInitializerTest {

    @Test
    void testConfigure() {
        ServletInitializer initializer = new ServletInitializer();
        SpringApplicationBuilder builder = mock(SpringApplicationBuilder.class);
        when(builder.sources(ProcucevApplication.class)).thenReturn(builder);

        SpringApplicationBuilder result = initializer.configure(builder);
        assertNotNull(result);
        verify(builder).sources(ProcucevApplication.class);
    }
}
