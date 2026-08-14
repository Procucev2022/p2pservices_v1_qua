package com.portal.procucev.rfq;

import com.portal.procucev.rfq.config.RfqSchemaInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;

public class RfqSchemaInitializerTest {

    private JdbcTemplate jdbcTemplate;
    private RfqSchemaInitializer initializer;

    @BeforeEach
    void setUp() {
        jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        initializer = new RfqSchemaInitializer(jdbcTemplate);
    }

    @Test
    @DisplayName("Test initializeSchema successful execution")
    void testInitializeSchemaSuccess() {
        assertDoesNotThrow(() -> initializer.initializeSchema());
        Mockito.verify(jdbcTemplate, Mockito.atLeastOnce()).execute(anyString());
    }

    @Test
    @DisplayName("Test initializeSchema exception handling")
    void testInitializeSchemaException() {
        Mockito.doThrow(new RuntimeException("DB Connection Refused")).when(jdbcTemplate).execute(anyString());
        assertDoesNotThrow(() -> initializer.initializeSchema());
    }
}
