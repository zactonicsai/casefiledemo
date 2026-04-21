package gov.fbi.casemgmt.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "gov.fbi.casemgmt.search")
public class ElasticsearchConfig {
    // Spring Boot auto-configures the ES client from spring.elasticsearch.uris.
    // This bean just enables the repositories in the search package.
}
