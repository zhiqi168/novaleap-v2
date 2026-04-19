package com.novaleap.api.module.quota.support;

import com.novaleap.api.module.quota.config.AiQuotaProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiQuotaPolicyTest {

    @Test
    void shouldApplyConfiguredTokenThresholds() {
        AiQuotaPolicy policy = new AiQuotaPolicy(defaultProperties());

        assertEquals(0, policy.degradeLevel(6_999_999L));
        assertEquals(1, policy.degradeLevel(7_000_000L));
        assertEquals(2, policy.degradeLevel(8_500_000L));
        assertEquals(3, policy.degradeLevel(9_500_000L));
    }

    @Test
    void shouldRejectInvalidThresholdOrder() {
        AiQuotaProperties props = defaultProperties();
        props.setWarningRatio(0.90D);
        props.setProtectRatio(0.85D);

        assertThrows(IllegalStateException.class, () -> new AiQuotaPolicy(props));
    }

    private AiQuotaProperties defaultProperties() {
        AiQuotaProperties props = new AiQuotaProperties();
        props.setDailyTokenLimit(10_000_000L);
        props.setWarningRatio(0.70D);
        props.setProtectRatio(0.85D);
        props.setLimitRatio(0.95D);
        return props;
    }
}
