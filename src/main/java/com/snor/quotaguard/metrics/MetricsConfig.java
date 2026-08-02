package com.snor.quotaguard.metrics;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability wiring: enables the {@code @Timed} annotations via the
 * {@link TimedAspect} (not auto-registered by Spring Boot) and adds a common
 * {@code application=quotaguard} tag to every meter.
 */
@Configuration
public class MetricsConfig {

    @Bean
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config().commonTags("application", "quotaguard");
    }
}
