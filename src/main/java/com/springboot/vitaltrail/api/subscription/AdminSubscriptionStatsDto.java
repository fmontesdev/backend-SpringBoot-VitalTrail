package com.springboot.vitaltrail.api.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminSubscriptionStatsDto {
    private long active;
    private long monthly;
    private long annual;
    private long newThisMonth;
    private long canceledThisMonth;
    private double churnRate;
}
