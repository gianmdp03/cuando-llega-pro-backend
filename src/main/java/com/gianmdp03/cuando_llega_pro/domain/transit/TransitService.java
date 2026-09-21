package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.config.CaffeineCacheConfig;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.LineaDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.LineaSentidoDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.ParadaDetalleDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.ParadaMapaDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.SentidoDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.Parada;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.ParadaLinea;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.LineaRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.ParadaLineaRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.ParadaRepository;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Query service for the persisted, static transport map catalogue. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransitService {

    private final LineaRepository lineaRepository;
    private final ParadaRepository paradaRepository;
    private final ParadaLineaRepository paradaLineaRepository;

    @Cacheable(cacheNames = CaffeineCacheConfig.TRANSIT_MAP_CACHE, key = "'lineas'")
    public List<LineaDTO> getLineas() {
        return lineaRepository.findAllByOrderByNombreAsc().stream()
                .map(linea -> new LineaDTO(linea.getCodigo(), linea.getNombre()))
                .toList();
    }

    @Cacheable(cacheNames = CaffeineCacheConfig.TRANSIT_MAP_CACHE, key = "'sentidos:' + #codigoLinea")
    public List<SentidoDTO> getSentidos(String codigoLinea) {
        requireLinea(codigoLinea);
        Map<String, SentidoDTO> sentidos = new LinkedHashMap<>();
        paradaLineaRepository.findDistinctSentidosByLineaCodigo(codigoLinea)
                .forEach(sentido -> sentidos.putIfAbsent(sentido.bandera(), sentido));
        return List.copyOf(sentidos.values());
    }

    @Cacheable(cacheNames = CaffeineCacheConfig.TRANSIT_MAP_CACHE, key = "'paradas:' + #codigoLinea + ':' + #bandera")
    public List<ParadaMapaDTO> getParadas(String codigoLinea, String bandera) {
        requireLinea(codigoLinea);
        return paradaRepository.findByCodigoLineaAndBandera(codigoLinea, bandera).stream()
                .map(this::toParadaMapa)
                .toList();
    }

    @Cacheable(cacheNames = CaffeineCacheConfig.TRANSIT_MAP_CACHE, key = "'parada:' + #identificador")
    public ParadaDetalleDTO getParadaDetalle(String identificador) {
        Parada parada = paradaRepository.findByIdentificadorWithLineas(identificador)
                .orElseThrow(() -> new ResourceNotFoundException("Parada no encontrada: " + identificador));
        List<LineaSentidoDTO> lineas = parada.getLineas().stream()
                .sorted((left, right) -> {
                    int byName = left.getLinea().getNombre().compareTo(right.getLinea().getNombre());
                    return byName != 0 ? byName : left.getBandera().compareTo(right.getBandera());
                })
                .map(this::toLineaSentido)
                .toList();
        return new ParadaDetalleDTO(parada.getIdentificador(), parada.getLatitud(), parada.getLongitud(), lineas);
    }

    private void requireLinea(String codigoLinea) {
        if (!lineaRepository.existsById(codigoLinea)) {
            throw new ResourceNotFoundException("Línea no encontrada: " + codigoLinea);
        }
    }

    private ParadaMapaDTO toParadaMapa(Parada parada) {
        return new ParadaMapaDTO(
                parada.getIdentificador(),
                parada.getCodigo(),
                parada.getDescripcion(),
                parada.getLatitud(),
                parada.getLongitud()
        );
    }

    private LineaSentidoDTO toLineaSentido(ParadaLinea paradaLinea) {
        return new LineaSentidoDTO(
                paradaLinea.getLinea().getCodigo(),
                paradaLinea.getLinea().getNombre(),
                paradaLinea.getBandera(),
                paradaLinea.getBanderaAmpliada()
        );
    }
}
