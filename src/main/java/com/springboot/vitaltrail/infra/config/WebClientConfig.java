package com.springboot.vitaltrail.infra.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import reactor.core.publisher.Mono;
import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@AllArgsConstructor
public class WebClientConfig {
    private final Dotenv dotenv;

    // @Bean
    // public WebClient laravelWebClient() {
    //     return buildWebClient(dotenv.get("LARAVEL_API_ENDPOINT"));
    // }

    @Bean
    public WebClient mailgunWebClient() {
        return buildWebClient(dotenv.get("MAILGUN_BACKEND_URL"));
    }

    private WebClient buildWebClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 4000)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(4000, TimeUnit.MILLISECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(4000, TimeUnit.MILLISECONDS));
                });

        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(retryFilter())
                .build();
    }

    private ExchangeFilterFunction retryFilter() {
        return (request, next) -> next.exchange(request)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .doAfterRetry(retrySignal -> System.out.println("Reintento número: " + (retrySignal.totalRetriesInARow() + 1))))
                .onErrorResume(e -> {
                    System.err.println("Error de URL " + request.url() + " después de reintentos: " + e.getMessage());
                    return Mono.error(e);
                });
    }
}