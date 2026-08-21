package com.portal.procucev.rfq;

import com.portal.procucev.rfq.config.RfqSchemaInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;

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
    @DisplayName("Test initializeSchema with ignored alter table exceptions")
    void testInitializeSchemaWithIgnoredExceptions() {
        doThrow(new RuntimeException("Duplicate column")).when(jdbcTemplate).execute(contains("ALTER TABLE"));
        assertDoesNotThrow(() -> initializer.initializeSchema());
    }

    @Test
    @DisplayName("Test initializeSchema outer exception handling")
    void testInitializeSchemaException() {
        doThrow(new RuntimeException("DB Connection Refused")).when(jdbcTemplate).execute(contains("CREATE TABLE IF NOT EXISTS rfq_buyers"));
        assertDoesNotThrow(() -> initializer.initializeSchema());
    }
}
