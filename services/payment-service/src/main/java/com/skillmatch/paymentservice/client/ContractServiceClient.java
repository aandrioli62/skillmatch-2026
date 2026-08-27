package com.skillmatch.paymentservice.client;

import com.skillmatch.paymentservice.exception.ContractServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

/**
 * Reads a contract's amount/status from Contract Service at payment time. Relays the
 * current caller's bearer token (the company paying is already authenticated) rather
 * than provisioning a separate service-account client, since both services sit behind
 * the same trusted network / gateway.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractServiceClient {

    private final RestClient contractServiceRestClient;

    @CircuitBreaker(name = "default", fallbackMethod = "getContractFallback")
    public ContractSummaryResponse getContract(UUID contractId) {
        return contractServiceRestClient.get()
                .uri("/api/v1/contracts/{contractId}", contractId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentBearerToken())
                .retrieve()
                .body(ContractSummaryResponse.class);
    }

    @SuppressWarnings("unused")
    private ContractSummaryResponse getContractFallback(UUID contractId, Throwable ex) {
        log.error("Contract Service unavailable while fetching contract id={}: {}", contractId, ex.getMessage());
        throw new ContractServiceUnavailableException(
                "Unable to verify contract id=" + contractId, ex);
    }

    private String currentBearerToken() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        throw new IllegalStateException("No authenticated JWT found in security context to relay to Contract Service");
    }
}
