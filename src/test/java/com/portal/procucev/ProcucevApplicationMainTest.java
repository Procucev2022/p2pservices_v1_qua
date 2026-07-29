package com.portal.procucev;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class ProcucevApplicationMainTest {

    @Test
    void testInstantiation() {
        ProcucevApplication application = new ProcucevApplication();
        assertNotNull(application);
    }

    @Test
    void testMain() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(ProcucevApplication.class, new String[]{})).thenReturn(null);
            ProcucevApplication.main(new String[]{});
            mocked.verify(() -> SpringApplication.run(ProcucevApplication.class, new String[]{}));
        }
    }
}
