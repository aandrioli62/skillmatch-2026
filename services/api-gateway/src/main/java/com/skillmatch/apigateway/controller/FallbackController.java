package com.skillmatch.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;

/**
 * Reached via the CircuitBreaker gateway filter's fallbackUri when a downstream
 * service is unavailable or the circuit is open. Returns an RFC 7807 Problem Detail,
 * mirroring the response shape produced by each service's own GlobalExceptionHandler.
 */
@RestController
public class FallbackController {

    private static final Logger log = LoggerFactory.getLogger(FallbackController.class);
    private static final String PROBLEM_BASE_URI = "https://skillmatch.io/problems/";

    @RequestMapping("/fallback/{service}")
    public Mono<ProblemDetail> fallback(@PathVariable("service") String service) {
        log.warn("Circuit breaker fallback triggered for {}", service);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "The " + service + " is temporarily unavailable. Please try again later.");
        problem.setType(URI.create(PROBLEM_BASE_URI + "service-unavailable"));
        problem.setTitle("Service Unavailable");
        problem.setProperty("service", service);
        problem.setProperty("timestamp", Instant.now());
        return Mono.just(problem);
    }
}
