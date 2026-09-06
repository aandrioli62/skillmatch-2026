package com.skillmatch.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
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

    /**
     * Client-Credentials-authenticated client for calls with no caller JWT to relay
     * (the RabbitMQ event listener has no HTTP request context), e.g. looking up a
     * notification recipient's email. Registration "user-service" is configured under
     * spring.security.oauth2.client.registration in application.yml.
     */
    @Bean
    public RestClient internalUserServiceRestClient(
            @Value("${services.user-service.base-url}") String userServiceBaseUrl,
            OAuth2AuthorizedClientManager authorizedClientManager) {
        return RestClient.builder()
                .baseUrl(userServiceBaseUrl)
                .requestInterceptor((request, body, execution) -> {
                    OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(
                            OAuth2AuthorizeRequest.withClientRegistrationId("user-service")
                                    .principal("notification-service")
                                    .build());
                    if (authorizedClient == null) {
                        throw new IllegalStateException("Unable to obtain a client-credentials token for 'user-service'");
                    }
                    request.getHeaders().setBearerAuth(authorizedClient.getAccessToken().getTokenValue());
                    return execution.execute(request, body);
                })
                .build();
    }
}
