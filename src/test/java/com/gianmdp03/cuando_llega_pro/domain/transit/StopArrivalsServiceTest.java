package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.ArrivalResponseDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.dto.BusArrivalItemDTO;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.model.TelemetryStatus;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.ArrivalsService;
import com.gianmdp03.cuando_llega_pro.domain.telemetry.service.TelemetryMapper;
import com.gianmdp03.cuando_llega_pro.domain.transit.dto.StopArrivalsDto;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.StopLineDirection;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitLine;
import com.gianmdp03.cuando_llega_pro.domain.transit.model.TransitStop;
import com.gianmdp03.cuando_llega_pro.domain.transit.repository.TransitStopRepository;
import com.gianmdp03.cuando_llega_pro.exception.UpstreamServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StopArrivalsServiceTest {

    @Mock
    private TransitStopRepository stopRepository;

    @Mock
    private ArrivalsService arrivalsService;

    @Mock
    private TelemetryMapper telemetryMapper;

    @InjectMocks
    private StopArrivalsService stopArrivalsService;

    @Test
    void getArrivals_queriesMgpOncePerCommercialLineAndGroupsByDirection() {
        TransitStop stop = stopWithTwoDirectionsFor521AndOneFor522();
        ArrivalResponseDTO live521 = liveResponse("521", stop.getIdentifier());
        ArrivalResponseDTO live522 = liveResponse("522", stop.getIdentifier());
        BusArrivalItemDTO bus = new BusArrivalItemDTO("521", "AL BOSQUE", 3, null, null, "42", null, TelemetryStatus.LIVE);

        when(stopRepository.findByIdentifierWithDirections("P3613")).thenReturn(Optional.of(stop));
        when(arrivalsService.getArrivals("521", "P3613")).thenReturn(live521);
        when(arrivalsService.getArrivals("522", "P3613")).thenReturn(live522);
        when(telemetryMapper.filterBusArrivalsByBranch(anyList(), eq("AL BOSQUE"))).thenReturn(List.of(bus));
        when(telemetryMapper.filterBusArrivalsByBranch(anyList(), eq("AL PUERTO"))).thenReturn(List.of());
        when(telemetryMapper.filterBusArrivalsByBranch(anyList(), eq("AL FARO"))).thenReturn(List.of());

        StopArrivalsDto response = stopArrivalsService.getArrivals("P3613");

        verify(arrivalsService, times(1)).getArrivals("521", "P3613");
        verify(arrivalsService, times(1)).getArrivals("522", "P3613");
        assertThat(response.lines()).hasSize(2);
        assertThat(response.lines().getFirst().lineCode()).isEqualTo("521");
        assertThat(response.lines().getFirst().directions()).hasSize(2);
        assertThat(response.lines().getFirst().directions().getFirst().arrivals()).containsExactly(bus);
    }

    @Test
    void getArrivals_keepsOtherLinesWhenOneUpstreamRequestFails() {
        TransitStop stop = stopWithTwoDirectionsFor521AndOneFor522();

        when(stopRepository.findByIdentifierWithDirections("P3613")).thenReturn(Optional.of(stop));
        when(arrivalsService.getArrivals("521", "P3613"))
                .thenThrow(new UpstreamServiceException("MGP unavailable"));
        when(arrivalsService.getArrivals("522", "P3613")).thenReturn(liveResponse("522", "P3613"));
        when(telemetryMapper.filterBusArrivalsByBranch(anyList(), eq("AL FARO"))).thenReturn(List.of());

        StopArrivalsDto response = stopArrivalsService.getArrivals("P3613");

        assertThat(response.lines().getFirst().status()).isEqualTo("UNAVAILABLE");
        assertThat(response.lines().getFirst().directions()).allSatisfy(direction -> assertThat(direction.arrivals()).isEmpty());
        assertThat(response.lines().getLast().status()).isEqualTo("LIVE");
    }

    @Test
    void streamArrivals_emitsOneResultPerCommercialLine() {
        TransitStop stop = stopWithTwoDirectionsFor521AndOneFor522();
        List<String> emittedLineCodes = new CopyOnWriteArrayList<>();

        when(stopRepository.findByIdentifierWithDirections("P3613")).thenReturn(Optional.of(stop));
        when(arrivalsService.getArrivals("521", "P3613")).thenReturn(liveResponse("521", "P3613"));
        when(arrivalsService.getArrivals("522", "P3613")).thenReturn(liveResponse("522", "P3613"));
        when(telemetryMapper.filterBusArrivalsByBranch(anyList(), eq("AL BOSQUE"))).thenReturn(List.of());
        when(telemetryMapper.filterBusArrivalsByBranch(anyList(), eq("AL PUERTO"))).thenReturn(List.of());
        when(telemetryMapper.filterBusArrivalsByBranch(anyList(), eq("AL FARO"))).thenReturn(List.of());

        stopArrivalsService.streamArrivals("P3613", line -> emittedLineCodes.add(line.lineCode()));

        assertThat(emittedLineCodes).containsExactlyInAnyOrder("521", "522");
        verify(arrivalsService, times(1)).getArrivals("521", "P3613");
        verify(arrivalsService, times(1)).getArrivals("522", "P3613");
    }

    private TransitStop stopWithTwoDirectionsFor521AndOneFor522() {
        TransitStop stop = new TransitStop("P3613", "17211", "Test", -38.0, -57.0);
        TransitLine line521 = new TransitLine("100", "521");
        stop.addTransitLine(new StopLineDirection(line521, "AL BOSQUE", "AL BOSQUE"));
        stop.addTransitLine(new StopLineDirection(line521, "AL PUERTO", "AL PUERTO"));
        stop.addTransitLine(new StopLineDirection(new TransitLine("101", "522"), "AL FARO", "AL FARO"));
        return stop;
    }

    private ArrivalResponseDTO liveResponse(String lineCode, String stopIdentifier) {
        return new ArrivalResponseDTO(
                lineCode,
                stopIdentifier,
                null,
                TelemetryStatus.LIVE,
                Instant.parse("2026-09-21T22:00:00Z"),
                0L,
                List.of()
        );
    }
}
