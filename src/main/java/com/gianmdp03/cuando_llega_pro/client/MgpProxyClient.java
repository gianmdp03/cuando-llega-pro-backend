package com.gianmdp03.cuando_llega_pro.client;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Declarative HTTP client interface for interacting with the Go TLS-spoofing sidecar proxy.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
public interface MgpProxyClient {

    /**
     * Executes a generic query against the Go sidecar proxy endpoint using arbitrary form parameters.
     *
     * @param requestId optional correlation ID for request tracing
     * @param formData  form-encoded parameters to forward to the upstream municipal service
     * @return raw response body from upstream service as a JSON string
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String queryProxy(
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            @RequestParam MultiValueMap<String, String> formData
    );

    /**
     * Retrieves arrivals for a specific bus stop and line.
     *
     * @param requestId optional correlation ID for request tracing
     * @param accion    upstream action name (e.g. RecuperarCuandoLlega)
     * @param stopId    bus stop identifier
     * @param lineCode  line code identifier
     * @return raw response body from upstream service as a JSON string
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String getArrivals(
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            @RequestParam("accion") String accion,
            @RequestParam("identificadorParada") String stopId,
            @RequestParam("codigoLineaParada") String lineCode
    );

    /**
     * Retrieves all available transit lines from the upstream service.
     *
     * @param id     optional correlation ID for request tracing
     * @param accion upstream action name (e.g. RecuperarLineas)
     * @return raw response body from upstream service as a JSON string
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String getLines(
            @RequestHeader(name = "X-Request-ID", required = false) String id,
            @RequestParam("accion") String accion
    );

    /**
     * Retrieves all bus stops for a specific line from the upstream service.
     *
     * @param id       optional correlation ID for request tracing
     * @param accion   upstream action name (e.g. RecuperarParadasPorLinea)
     * @param lineCode transit line identifier
     * @return raw response body from upstream service as a JSON string
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String getStopsByLine(
            @RequestHeader(name = "X-Request-ID", required = false) String id,
            @RequestParam("accion") String accion,
            @RequestParam("codigoLinea") String lineCode
    );

    /**
     * Retrieves the geographic route polyline trace for a specific line from the upstream service.
     *
     * @param id       optional correlation ID for request tracing
     * @param accion   upstream action name (e.g. RecuperarRecorridosPorLinea)
     * @param lineCode transit line identifier
     * @return raw response body from upstream service as a JSON string
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String getRouteByLine(
            @RequestHeader(name = "X-Request-ID", required = false) String id,
            @RequestParam("accion") String accion,
            @RequestParam("codigoLinea") String lineCode
    );
}
