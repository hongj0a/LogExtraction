package org.sooil.autologextraction;

import org.sooil.autologextraction.service.FileService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.Map;

@SpringBootApplication
public class AutoLogExtractionApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutoLogExtractionApplication.class, args);
	}

	@Bean
	public CommandLineRunner run(FileService fileService) {
		return args -> {
			try {
				Map<String, String> logEntries = fileService.extractLogEntries();
				System.out.println("logEntries = " + logEntries);
				fileService.updateExcelFile(logEntries);
				System.out.println("Excel file updated successfully.");
			} catch (IOException e) {
				e.printStackTrace();
			}
		};
	}

}
