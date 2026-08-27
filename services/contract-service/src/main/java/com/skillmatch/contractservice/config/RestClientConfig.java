package com.skillmatch.contractservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient userServiceRestClient(
            @Value("${services.user-service.base-url}") String userServiceBaseUrl) {
        return RestClient.builder()
                .baseUrl(userServiceBaseUrl)
                .build();
    }
}
