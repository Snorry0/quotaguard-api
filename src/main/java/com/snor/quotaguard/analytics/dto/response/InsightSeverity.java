package com.snor.quotaguard.analytics.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Severity of a behavioural insight.
 */
@Schema(
        description = """
                Severity of a behavioural insight.

                Possible values:
                - `INFO` — Informational insight; no action required.
                - `WARN` — Behavioural pattern worth monitoring; consider adjusting usage.
                - `RISK` — Behavioural pattern that risks quota exhaustion or penalty application; consider action.
                """
)
public enum InsightSeverity {
    INFO,
    WARN,
    RISK
}
