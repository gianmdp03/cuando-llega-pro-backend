package com.gianmdp03.cuando_llega_pro.domain.transit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransitLineResolver Unit Tests")
class TransitLineResolverTest {

    private TransitLineResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new TransitLineResolver();
    }

    @Test
    @DisplayName("Resolves commercial lines to upstream internal codes from HAR contract")
    void resolvesCommercialToInternal() {
        assertThat(resolver.toInternalCode("511")).isEqualTo("98");
        assertThat(resolver.toInternalCode("501")).isEqualTo("93");
        assertThat(resolver.toInternalCode("521")).isEqualTo("100");
        assertThat(resolver.toInternalCode("522")).isEqualTo("101");
        assertThat(resolver.toInternalCode("563")).isEqualTo("120");
        assertThat(resolver.toInternalCode("717")).isEqualTo("127");
        assertThat(resolver.toInternalCode("BATAN")).isEqualTo("344");
    }

    @Test
    @DisplayName("Resolves internal codes back to commercial codes")
    void resolvesInternalToCommercial() {
        assertThat(resolver.toCommercialCode("98")).isEqualTo("511");
        assertThat(resolver.toCommercialCode("93")).isEqualTo("501");
        assertThat(resolver.toCommercialCode("101")).isEqualTo("522");
        assertThat(resolver.toCommercialCode("344")).isEqualTo("BATAN");
    }

    @Test
    @DisplayName("Passes through already converted or unknown line codes")
    void passThroughUnknownOrAlreadyInternal() {
        assertThat(resolver.toInternalCode("98")).isEqualTo("98");
        assertThat(resolver.toCommercialCode("511")).isEqualTo("511");
        assertThat(resolver.toInternalCode("UNKNOWN_999")).isEqualTo("UNKNOWN_999");
        assertThat(resolver.toInternalCode(null)).isNull();
    }

    @Test
    @DisplayName("Allows dynamic registration of new line mappings")
    void dynamicRegistration() {
        resolver.register("LINEA_TEST", "999");
        assertThat(resolver.toInternalCode("LINEA_TEST")).isEqualTo("999");
        assertThat(resolver.toCommercialCode("999")).isEqualTo("LINEA_TEST");
    }
}
