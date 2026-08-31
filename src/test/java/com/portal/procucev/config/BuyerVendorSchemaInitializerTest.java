package com.portal.procucev.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuyerVendorSchemaInitializerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BuyerVendorSchemaInitializer initializer;

    @Test
    void testInitializeSchemaSuccess() {
        doNothing().when(jdbcTemplate).execute(anyString());

        assertDoesNotThrow(() -> initializer.initializeSchema());

        verify(jdbcTemplate, atLeast(3)).execute(anyString());
    }

    @Test
    void testInitializeSchemaAlterFailureIgnored() {
        doAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.startsWith("ALTER TABLE")) {
                throw new DataAccessException("Column already exists or cannot alter") {};
            }
            return null;
        }).when(jdbcTemplate).execute(anyString());

        assertDoesNotThrow(() -> initializer.initializeSchema());
    }

    @Test
    void testInitializeSchemaTableCreationFailureHandled() {
        doThrow(new RuntimeException("DB Connection failed")).when(jdbcTemplate).execute(anyString());

        assertDoesNotThrow(() -> initializer.initializeSchema());
    }
}
