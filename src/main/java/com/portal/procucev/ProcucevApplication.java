package com.portal.procucev;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableCaching
@SpringBootApplication
public class ProcucevApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcucevApplication.class);

	public static void main(String[] args) {
		loadDotEnvIfPresent();
		SpringApplication.run(ProcucevApplication.class, args);
	}

	private static void loadDotEnvIfPresent() {
		File envFile = new File(".env");
		if (!envFile.exists() || !envFile.isFile()) {
			return;
		}
		try {
			List<String> lines = Files.readAllLines(envFile.toPath());
			int loadedCount = 0;
			for (String line : lines) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				int eqIdx = trimmed.indexOf('=');
				if (eqIdx > 0) {
					String key = trimmed.substring(0, eqIdx).trim();
					String value = trimmed.substring(eqIdx + 1).trim();
					if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
						value = value.substring(1, value.length() - 1);
					}
					if (System.getProperty(key) == null && System.getenv(key) == null) {
						System.setProperty(key, value);
						loadedCount++;
					}
				}
			}
			LOGGER.info("Loaded {} configuration properties from .env file", loadedCount);
		} catch (Exception e) {
			LOGGER.warn("Failed to load .env file: {}", e.getMessage());
		}
	}

}

