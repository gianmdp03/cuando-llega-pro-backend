package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.transit.dto.LineaDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.ParadaDetalleDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.ParadaMapaDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.SentidoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** REST API used by the interactive map against the persisted catalogue. */
@RestController
@RequestMapping("/api/v1/transit")
@RequiredArgsConstructor
public class TransitController {

    private final TransitService transitService;

    @GetMapping("/lineas")
    public ResponseEntity<List<LineaDTO>> getLineas() {
        return ResponseEntity.ok(transitService.getLineas());
    }

    @GetMapping("/lineas/{codigoLinea}/sentidos")
    public ResponseEntity<List<SentidoDTO>> getSentidos(@PathVariable String codigoLinea) {
        return ResponseEntity.ok(transitService.getSentidos(codigoLinea));
    }

    @GetMapping("/lineas/{codigoLinea}/paradas")
    public ResponseEntity<List<ParadaMapaDTO>> getParadas(
            @PathVariable String codigoLinea,
            @RequestParam String bandera
    ) {
        return ResponseEntity.ok(transitService.getParadas(codigoLinea, bandera));
    }

    @GetMapping("/paradas/{identificador}")
    public ResponseEntity<ParadaDetalleDTO> getParadaDetalle(@PathVariable String identificador) {
        return ResponseEntity.ok(transitService.getParadaDetalle(identificador));
    }
}
