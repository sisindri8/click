package com.click.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API Gateway - currently a minimal orchestrator.
 * <p>
 * Today it does ONE thing: call Query Expansion Service, take its 5 query
 * variations, call YouTube Search Service with them, return the combined
 * result. This proves the two services can actually talk to each other
 * over real HTTP calls before we add Transcript/Summarization/Curation
 * services into the chain.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
