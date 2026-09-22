package com.gianmdp03.cuando_llega_pro.client;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Declarative HTTP client interface for interacting with the Go TLS-spoofing sidecar proxy.
 * Maps exact municipal upstream actions extracted from real HAR telemetry.
 */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
public interface MgpProxyClient {

    /**
     * Executes a generic query against the Go sidecar proxy endpoint using arbitrary form parameters.
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String queryProxy(
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            @RequestParam MultiValueMap<String, String> formData
    );

    /**
     * Action: RecuperarProximosArribosW
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String getArrivals(
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            @RequestParam("accion") String accion,
            @RequestParam("identificadorParada") String stopId,
            @RequestParam("codigoLineaParada") String lineCode
    );

    /**
     * Action: RecuperarLineaPorCuandoLlega / RecuperarLineas
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String getLines(
            @RequestHeader(name = "X-Request-ID", required = false) String id,
            @RequestParam("accion") String accion
    );

    /**
     * Action: RecuperarCallesPrincipalPorLinea
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String getMainStreetsByLine(
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            @RequestParam("accion") String accion,
            @RequestParam("codLinea") String codLinea
    );

    /**
     * Action: RecuperarInterseccionPorLineaYCalle
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String getIntersectionsByLineAndStreet(
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            @RequestParam("accion") String accion,
            @RequestParam("codLinea") String codLinea,
            @RequestParam("codCalle") String codCalle
    );

    /**
     * Action: RecuperarParadasConBanderaPorLineaCalleEInterseccion
     */
    @PostExchange(value = "/proxy", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String getStopsWithFlag(
            @RequestHeader(name = "X-Request-ID", required = false) String requestId,
            @RequestParam("accion") String accion,
            @RequestParam("codLinea") String codLinea,
            @RequestParam("codCalle") String codCalle,
            @RequestParam("codInterseccion") String codInterseccion
    );

}
