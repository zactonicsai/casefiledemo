package gov.fbi.casemgmt.config;

import gov.fbi.casemgmt.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class JpaAuditingConfig {

    @Bean
    AuditorAware<String> auditorProvider() {
        return () -> SecurityUtils.currentUsername().or(() -> Optional.of("system"));
    }
}
