package com.portal.procucev;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class ProcucevApplicationMainTest {

    @Test
    void testInstantiation() {
        ProcucevApplication application = new ProcucevApplication();
        assertNotNull(application);
    }

    @Test
    @DisplayName("Test loadDotEnvIfPresent when .env does not exist")
    void testMain_NoEnvFile() throws Exception {
        Path tempEnv = Path.of(".env");
        Path backup = Path.of(".env.bak.test");
        boolean backedUp = false;
        if (Files.exists(tempEnv)) {
            Files.move(tempEnv, backup);
            backedUp = true;
        }

        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(ProcucevApplication.class, new String[]{})).thenReturn(null);
            ProcucevApplication.main(new String[]{});
            mocked.verify(() -> SpringApplication.run(ProcucevApplication.class, new String[]{}));
        } finally {
            if (backedUp) {
                Files.move(backup, tempEnv);
            }
        }
    }

    @Test
    @DisplayName("Test main and loadDotEnvIfPresent with full quote, key already present, comments, and edge cases")
    void testMain_WithDotEnvParsingVariations() throws Exception {
        Path tempEnv = Path.of(".env");
        Path backup = Path.of(".env.bak.test2");
        boolean backedUp = false;
        if (Files.exists(tempEnv)) {
            Files.move(tempEnv, backup);
            backedUp = true;
        }

        System.setProperty("EXISTING_KEY", "already_set");

        String content = "# Comment line\n\n"
                + "TEST_CUSTOM_KEY_1=\"double_quoted_val\"\n"
                + "TEST_CUSTOM_KEY_2='single_quoted_val'\n"
                + "TEST_CUSTOM_KEY_3=plain_val\n"
                + "TEST_CUSTOM_KEY_4=\"unmatched_double\n"
                + "TEST_CUSTOM_KEY_5='unmatched_single\n"
                + "EXISTING_KEY=new_val\n"
                + "INVALID_LINE_NO_EQUALS\n"
                + "=NO_KEY\n";
        Files.writeString(tempEnv, content);

        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(ProcucevApplication.class, new String[]{})).thenReturn(null);
            ProcucevApplication.main(new String[]{});
            mocked.verify(() -> SpringApplication.run(ProcucevApplication.class, new String[]{}));
        } finally {
            Files.deleteIfExists(tempEnv);
            if (backedUp) {
                Files.move(backup, tempEnv);
            }
        }
    }

    @Test
    @DisplayName("Test loadDotEnvIfPresent when .env is a directory or throws exception")
    void testMain_EnvIsDirectory() throws Exception {
        Path tempEnv = Path.of(".env");
        Path backup = Path.of(".env.bak.test3");
        boolean backedUp = false;
        if (Files.exists(tempEnv)) {
            Files.move(tempEnv, backup);
            backedUp = true;
        }

        Files.createDirectory(tempEnv);

        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(ProcucevApplication.class, new String[]{})).thenReturn(null);
            ProcucevApplication.main(new String[]{});
            mocked.verify(() -> SpringApplication.run(ProcucevApplication.class, new String[]{}));
        } finally {
            Files.deleteIfExists(tempEnv);
            if (backedUp) {
                Files.move(backup, tempEnv);
            }
        }
    }
}
