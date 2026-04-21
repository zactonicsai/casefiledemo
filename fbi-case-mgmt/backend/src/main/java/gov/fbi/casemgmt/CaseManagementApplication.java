package gov.fbi.casemgmt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Demo Only-style Case Management API.
 *
 * <p>This service implements electronic case-file management modeled after the FBI's
 * Demo Only system. It is UNCLASSIFIED reference code — not a CJIS-authorized system.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableCaching
@EnableAsync
public class CaseManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaseManagementApplication.class, args);
    }
}
