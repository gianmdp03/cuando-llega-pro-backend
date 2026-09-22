package com.gianmdp03.cuando_llega_pro.domain.transit;

import com.gianmdp03.cuando_llega_pro.domain.transit.dto.StopLineArrivalsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Streams progressive live-arrival updates while a map stop panel remains open. */
@RestController
@RequestMapping("/api/v1/transit/map")
@RequiredArgsConstructor
public class StopArrivalsStreamController {

    private static final long REFRESH_INTERVAL_MILLIS = 20_000L;

    private final StopArrivalsService stopArrivalsService;

    @GetMapping(value = "/stops/{identifier}/arrivals/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStopArrivals(@PathVariable String identifier) {
        // Validate before committing HTTP 200 so an unknown stop remains a normal 404 response.
        stopArrivalsService.requireStop(identifier);

        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean active = new AtomicBoolean(true);
        AtomicReference<Thread> worker = new AtomicReference<>();
        Runnable stopWorker = () -> {
            active.set(false);
            Thread thread = worker.get();
            if (thread != null) {
                thread.interrupt();
            }
        };
        emitter.onCompletion(stopWorker);
        emitter.onTimeout(stopWorker);
        emitter.onError(ignored -> stopWorker.run());

        worker.set(Thread.ofVirtual().name("stop-arrivals-stream-" + identifier).start(() -> {
            try {
                while (active.get()) {
                    send(emitter, "cycle-start", Map.of("identifier", identifier));
                    stopArrivalsService.streamArrivals(identifier, line -> sendLine(emitter, line));
                    send(emitter, "cycle-complete", Map.of("identifier", identifier));

                    Thread.sleep(REFRESH_INTERVAL_MILLIS);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (UncheckedIOException exception) {
                // The client disconnected while an event was being sent.
            } catch (Exception exception) {
                if (active.get()) {
                    try {
                        send(emitter, "stream-error", Map.of("message", "Unable to refresh stop arrivals"));
                    } catch (IOException ignored) {
                        // The client already disconnected.
                    }
                }
            } finally {
                emitter.complete();
            }
        }));

        return emitter;
    }

    private void sendLine(SseEmitter emitter, StopLineArrivalsDto line) {
        try {
            send(emitter, "line-arrivals", line);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private void send(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }
}
