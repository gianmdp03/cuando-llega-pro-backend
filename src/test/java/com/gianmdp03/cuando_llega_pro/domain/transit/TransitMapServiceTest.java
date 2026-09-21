package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.transit.dto.StopDetailDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.DirectionDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitLine;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitStop;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.StopLineDirection;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitLineRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.StopLineDirectionRepository;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitStopRepository;
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
class TransitMapServiceTest {

    @Mock
    private TransitLineRepository lineRepository;

    @Mock
    private TransitStopRepository stopRepository;

    @Mock
    private StopLineDirectionRepository stopTransitLineRepository;

    @InjectMocks
    private TransitService transitService;

    @Test
    void getDirections_returnsOneEntryPerBandera() {
        when(lineRepository.existsById("100")).thenReturn(true);
        when(stopTransitLineRepository.findDistinctSentidosByTransitLineCodigo("100")).thenReturn(List.of(
                new DirectionDto("AL BOSQUE", "AL BOSQUE"),
                new DirectionDto("AL BOSQUE", "AL BOSQUE VÍA CENTRO"),
                new DirectionDto("A BERUTI", "A BERUTI")
        ));

        assertThat(transitService.getDirections("100"))
                .containsExactly(
                        new DirectionDto("AL BOSQUE", "AL BOSQUE"),
                        new DirectionDto("A BERUTI", "A BERUTI")
                );
    }

    @Test
    void getStop_returnsAllConvergingLines() {
        TransitStop stop = new TransitStop("P3613", "17211", "3613", -37.999541, -57.544504);
        stop.addTransitLine(new StopLineDirection(new TransitLine("100", "521"), "AL BOSQUE", "AL BOSQUE"));
        stop.addTransitLine(new StopLineDirection(new TransitLine("101", "522"), "AL FARO", "AL FARO"));
        when(stopRepository.findByIdentificadorWithTransitLines("P3613")).thenReturn(Optional.of(stop));

        StopDetailDto detalle = transitService.getStop("P3613");

        assertThat(detalle.identifier()).isEqualTo("P3613");
        assertThat(detalle.directions()).hasSize(2);
        assertThat(detalle.directions().getFirst().codeTransitLine()).isEqualTo("100");
        assertThat(detalle.directions().getLast().codeTransitLine()).isEqualTo("101");
    }

    @Test
    void getStops_rejectsUnknownLine() {
        when(lineRepository.existsById("unknown")).thenReturn(false);

        assertThatThrownBy(() -> transitService.getStops("unknown", "AL BOSQUE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Línea no encontrada: unknown");
    }
}
