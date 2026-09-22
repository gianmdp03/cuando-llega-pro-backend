package com.gianmdp03.cuando_llega_pro.domain.telemetry.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MgpRequestPacerTest {

    @Test
    void spacesDifferentRequestsByConfiguredGlobalInterval() {
        long minimumIntervalMillis = 30;
        MgpRequestPacer pacer = new MgpRequestPacer(minimumIntervalMillis, 1);

        pacer.execute(() -> "first");
        long startedAt = System.nanoTime();
        String result = pacer.execute(() -> "second");
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        assertThat(result).isEqualTo("second");
        assertThat(elapsedMillis).isGreaterThanOrEqualTo(minimumIntervalMillis - 5);
    }

    @Test
    void executesARequestWithoutAnArtificialDelayWhenNoIntervalIsConfigured() {
        MgpRequestPacer pacer = new MgpRequestPacer(0, 1);

        String result = pacer.execute(() -> "single physical request");

        assertThat(result).isEqualTo("single physical request");
    }

    @Test
    void honorsCooldownAfterAnUpstreamRateLimit() {
        MgpRequestPacer pacer = new MgpRequestPacer(0, 1);
        pacer.registerRateLimit(java.time.Duration.ofMillis(50));

        long startedAt = System.nanoTime();
        pacer.execute(() -> "after cooldown");
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;

        assertThat(elapsedMillis).isGreaterThanOrEqualTo(40);
    }
}
