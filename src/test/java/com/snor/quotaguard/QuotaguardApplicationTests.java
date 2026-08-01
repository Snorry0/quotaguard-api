package com.snor.quotaguard;

import com.snor.quotaguard.config.JwtProperties;
import com.snor.quotaguard.config.QuotaGuardProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuotaGuardApplicationTests {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private QuotaGuardProperties quotaGuardProperties;

    @Test
    void contextLoads() {
    }

    @Test
    void jwtExpirationBindsFromConfiguration() {
        assertThat(jwtProperties.expiration()).isEqualTo(Duration.ofHours(1));
    }

    @Test
    void quotaGuardPropertiesBindFromConfiguration() {
        assertThat(quotaGuardProperties.defaultDailyLimit()).isEqualTo(100);
        assertThat(quotaGuardProperties.penaltyDecayPerReset()).isEqualTo(1);
        assertThat(quotaGuardProperties.penalties().shortCooldown()).isEqualTo(Duration.ofMinutes(15));
        assertThat(quotaGuardProperties.penalties().longCooldown()).isEqualTo(Duration.ofHours(4));
        assertThat(quotaGuardProperties.sessions().unitsPerMinute()).isEqualTo(1);
        assertThat(quotaGuardProperties.sessions().minimumCharge()).isEqualTo(1);
    }
}
