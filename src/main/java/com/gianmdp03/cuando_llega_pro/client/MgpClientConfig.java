package com.gianmdp03.cuando_llega_pro.client;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.MgpRequestPacer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Configuration
public class MgpClientConfig {

    @Value("${app.proxy.base-url:${MGP_PROXY_URL:http://localhost:8079}}")
    private String baseUrl;

    @Value("${app.proxy.connect-timeout-seconds:5}")
    private long connectTimeoutSeconds;

    @Value("${app.proxy.read-timeout-seconds:15}")
    private long readTimeoutSeconds;

    @Bean
    public RestClient mgpProxyRestClient(MgpRequestPacer mgpRequestPacer) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    try {
                        var response = mgpRequestPacer.execute(() -> {
                            try {
                                return execution.execute(request, body);
                            } catch (IOException exception) {
                                throw new MgpProxyTransportException(exception);
                            }
                        });
                        if (response.getStatusCode().value() == 429) {
                            mgpRequestPacer.registerRateLimit(retryAfter(response.getHeaders().getFirst("Retry-After")));
                        }
                        return response;
                    } catch (MgpProxyTransportException exception) {
                        throw exception.getCause();
                    }
                })
                .build();
    }

    private static Duration retryAfter(String retryAfterHeader) {
        if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
            return null;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(retryAfterHeader.trim()));
        } catch (NumberFormatException ignored) {
            try {
                long seconds = ChronoUnit.SECONDS.between(Instant.now(), java.time.ZonedDateTime.parse(retryAfterHeader).toInstant());
                return seconds > 0 ? Duration.ofSeconds(seconds) : null;
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    @Bean
    public MgpProxyClient mgpProxyClient(RestClient mgpProxyRestClient) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(mgpProxyRestClient))
                .build()
                .createClient(MgpProxyClient.class);
    }
}

final class MgpProxyTransportException extends RuntimeException {
    MgpProxyTransportException(IOException cause) {
        super(cause);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
