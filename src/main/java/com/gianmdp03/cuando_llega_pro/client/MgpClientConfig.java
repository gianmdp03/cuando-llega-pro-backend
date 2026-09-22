package com.gianmdp03.cuando_llega_pro.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class MgpClientConfig {

    @Value("${app.proxy.base-url:${MGP_PROXY_URL:http://localhost:8079}}")
    private String baseUrl;

    @Value("${app.proxy.connect-timeout-seconds:5}")
    private long connectTimeoutSeconds;

    @Value("${app.proxy.read-timeout-seconds:15}")
    private long readTimeoutSeconds;

    @Bean
    public RestClient mgpProxyRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public MgpProxyClient mgpProxyClient(RestClient mgpProxyRestClient) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(mgpProxyRestClient))
                .build()
                .createClient(MgpProxyClient.class);
    }
}
