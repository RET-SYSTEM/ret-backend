package me.cbhud.ret.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public RestClient workerRestClient(
            @Value("${worker.base-url}") String workerBaseUrl,
            @Value("${worker.api-key}") String workerApiKey) {
            
        return RestClient.builder()
                .baseUrl(workerBaseUrl)
                .defaultHeader("x-internal-api-key", workerApiKey)
                .build();
    }
}
