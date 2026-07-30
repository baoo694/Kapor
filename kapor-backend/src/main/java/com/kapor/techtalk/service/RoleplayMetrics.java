package com.kapor.techtalk.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoleplayMetrics {
    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    public RoleplayMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void increment(String name) {
        counters.computeIfAbsent(name, key -> Counter.builder("kapor.techtalk." + key).register(registry)).increment();
    }

    public void record(String name, Duration duration) {
        Timer.builder("kapor.techtalk." + name).publishPercentileHistogram().register(registry).record(duration);
    }
}
