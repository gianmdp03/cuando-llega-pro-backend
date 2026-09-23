package com.gianmdp03.cuando_llega_pro.domain.geo;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReverseGeocodingServiceTest {

    @Test
    void readsThePermanentAddressCache() {
        GeoCacheRepository repository = mock(GeoCacheRepository.class);
        GeoCacheEntity cached = new GeoCacheEntity("-38.0000:-57.5000", "Juramento (esq Roca)");
        when(repository.findById("-38.0000:-57.5000")).thenReturn(Optional.of(cached));
        ReverseGeocodingService service = new ReverseGeocodingService(repository, null, null);

        assertThat(service.resolveAddress(-38.0, -57.5)).isEqualTo("Juramento");

        verify(repository).findById("-38.0000:-57.5000");
        verify(repository).save(cached);
    }
}
