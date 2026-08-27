package com.skillmatch.paymentservice.config;

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

    @Bean
    public RestClient contractServiceRestClient(
            @Value("${services.contract-service.base-url}") String contractServiceBaseUrl) {
        return RestClient.builder()
                .baseUrl(contractServiceBaseUrl)
                .build();
    }
}
