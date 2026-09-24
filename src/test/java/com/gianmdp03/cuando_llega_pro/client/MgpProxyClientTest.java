package com.gianmdp03.cuando_llega_pro.client;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.MgpRequestPacer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MgpProxyClientTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("app.proxy.enabled=true")
            .withUserConfiguration(MgpClientConfig.class)
            .withBean(MgpRequestPacer.class, () -> new MgpRequestPacer(0, 1));

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private MgpProxyClient proxyClient;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder()
                .baseUrl("http://localhost:8080");

        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        proxyClient = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(MgpProxyClient.class);
    }

    @Test
    @DisplayName("Context loads with default configuration beans")
    void contextLoadsDefaultBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(MgpClientConfig.class);
            assertThat(context).hasSingleBean(RestClient.class);
            assertThat(context).hasSingleBean(MgpProxyClient.class);
        });
    }

    @Test
    @DisplayName("Beans are not loaded when app.proxy.enabled is false")
    void beansNotLoadedWhenDisabled() {
        contextRunner
                .withPropertyValues("app.proxy.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(MgpClientConfig.class);
                    assertThat(context).doesNotHaveBean(MgpProxyClient.class);
                });
    }

    @Test
    @DisplayName("Context loads with custom property overrides")
    void contextLoadsCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "app.proxy.base-url=http://custom-proxy:9090",
                        "app.proxy.connect-timeout-seconds=10",
                        "app.proxy.read-timeout-seconds=30",
                        "app.proxy.origin=http://custom-origin:8400"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(RestClient.class);
                    assertThat(context).hasSingleBean(MgpProxyClient.class);
                });
    }

    @Test
    @DisplayName("queryProxy sends POST to /proxy with form urlencoded body and headers")
    void queryProxySendsCorrectRequest() {
        String requestId = UUID.randomUUID().toString();
        String expectedResponse = "[{\"id\":1,\"descripcion\":\"511 A\"}]";

        mockServer.expect(requestTo("http://localhost:8080/proxy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Request-ID", requestId))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(content().string("accion=RecuperarLineaPorCuandoLlega"))
                .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("accion", "RecuperarLineaPorCuandoLlega");

        String actualResponse = proxyClient.queryProxy(requestId, formData);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        mockServer.verify();
    }

    @Test
    @DisplayName("getArrivals sends POST to /proxy with individual params and headers")
    void getArrivalsSendsCorrectRequest() {
        String requestId = UUID.randomUUID().toString();
        String expectedResponse = "[{\"linea\":\"511\",\"arribo\":\"5 min\"}]";

        mockServer.expect(requestTo("http://localhost:8080/proxy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Request-ID", requestId))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(content().string("accion=RecuperarCuandoLlega&identificadorParada=1024&codigoLineaParada=511"))
                .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));

        String actualResponse = proxyClient.getArrivals(requestId, "RecuperarCuandoLlega", "1024", "511");

        assertThat(actualResponse).isEqualTo(expectedResponse);
        mockServer.verify();
    }

    @Test
    @DisplayName("queryProxy succeeds when optional requestId is null")
    void queryProxyWithoutRequestId() {
        String expectedResponse = "{\"status\":\"ok\"}";

        mockServer.expect(requestTo("http://localhost:8080/proxy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
                .andExpect(content().string("param=value"))
                .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("param", "value");

        String actualResponse = proxyClient.queryProxy(null, formData);

        assertThat(actualResponse).isEqualTo(expectedResponse);
        mockServer.verify();
    }
}
