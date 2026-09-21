package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.transit.dto.ParadaDetalleDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.SentidoDTO;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.Linea;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.Parada;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.ParadaLinea;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.LineaRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.ParadaLineaRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.ParadaRepository;
import com.gianmdp03.cuando_llega_pro.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransitServiceTest {

    @Mock
    private LineaRepository lineaRepository;

    @Mock
    private ParadaRepository paradaRepository;

    @Mock
    private ParadaLineaRepository paradaLineaRepository;

    @InjectMocks
    private TransitService transitService;

    @Test
    void getSentidos_returnsOneEntryPerBandera() {
        when(lineaRepository.existsById("100")).thenReturn(true);
        when(paradaLineaRepository.findDistinctSentidosByLineaCodigo("100")).thenReturn(List.of(
                new SentidoDTO("AL BOSQUE", "AL BOSQUE"),
                new SentidoDTO("AL BOSQUE", "AL BOSQUE VÍA CENTRO"),
                new SentidoDTO("A BERUTI", "A BERUTI")
        ));

        assertThat(transitService.getSentidos("100"))
                .containsExactly(
                        new SentidoDTO("AL BOSQUE", "AL BOSQUE"),
                        new SentidoDTO("A BERUTI", "A BERUTI")
                );
    }

    @Test
    void getParadaDetalle_returnsAllConvergingLines() {
        Parada parada = new Parada("P3613", "17211", "3613", -37.999541, -57.544504);
        parada.addLinea(new ParadaLinea(new Linea("100", "521"), "AL BOSQUE", "AL BOSQUE"));
        parada.addLinea(new ParadaLinea(new Linea("101", "522"), "AL FARO", "AL FARO"));
        when(paradaRepository.findByIdentificadorWithLineas("P3613")).thenReturn(Optional.of(parada));

        ParadaDetalleDTO detalle = transitService.getParadaDetalle("P3613");

        assertThat(detalle.identificador()).isEqualTo("P3613");
        assertThat(detalle.lineas()).hasSize(2);
        assertThat(detalle.lineas().getFirst().codigoLinea()).isEqualTo("100");
        assertThat(detalle.lineas().getLast().codigoLinea()).isEqualTo("101");
    }

    @Test
    void getParadas_rejectsUnknownLine() {
        when(lineaRepository.existsById("unknown")).thenReturn(false);

        assertThatThrownBy(() -> transitService.getParadas("unknown", "AL BOSQUE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Línea no encontrada: unknown");
    }
}
