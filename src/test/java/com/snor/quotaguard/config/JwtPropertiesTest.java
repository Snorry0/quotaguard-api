package com.snor.quotaguard.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void rejectsSecretsShorterThanThirtyTwoCharacters() {
        assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void defaultsExpirationToTwelveHoursWhenAbsent() {
        JwtProperties properties = new JwtProperties("a-secret-that-is-longer-than-thirty-two-chars", null);

        assertThat(properties.expiration()).isEqualTo(Duration.ofHours(12));
    }
}
