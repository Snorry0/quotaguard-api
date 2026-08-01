package com.snor.quotaguard.domain.enums;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class ActionTypeTest {

    @Test
    void documentedActionTypesAreSupported() {
        assertThat(EnumSet.allOf(ActionType.class)).containsExactly(
                ActionType.API_CALL,
                ActionType.RESOURCE_ACCESS,
                ActionType.BACKGROUND_JOB,
                ActionType.SESSION_ACTION,
                ActionType.MANUAL_ADJUSTMENT
        );
    }
}
