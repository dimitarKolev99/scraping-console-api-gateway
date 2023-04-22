package com.scrapingconsole.gateway.api.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Value("${scraper.url}")
    private String scraperUrl;

    @Bean
    public WebClient scraperClient() {
        return WebClient.builder()
                .exchangeStrategies(this.createExchangeStrategies())
                .baseUrl(scraperUrl)
                .clientConnector(this.createClientHttpConnector())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Utility to create the client http connector. Needed to plugin the HttpClient into the
     * ClientHttpConnector.
     *
     * @return The created connector.
     */
    private ClientHttpConnector createClientHttpConnector() {
        HttpClient client = HttpClient.create()
                .doOnRequest(((httpClientRequest, connection) -> {
                    connection.addHandlerLast(new CustomLoggingHandler());
                }));

        ClientHttpConnector connector = new ReactorClientHttpConnector(client);

        return connector;
    }

    private ExchangeStrategies createExchangeStrategies() {
        return ExchangeStrategies.builder()
                .codecs(clientCodecConfigurer -> clientCodecConfigurer
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}
