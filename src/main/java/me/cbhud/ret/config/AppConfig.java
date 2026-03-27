package me.cbhud.ret.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public RestClient workerRestClient(@Value("${worker.base-url}") String workerBaseUrl) {
        return RestClient.builder()
                .baseUrl(workerBaseUrl)
                .build();
    }
}
