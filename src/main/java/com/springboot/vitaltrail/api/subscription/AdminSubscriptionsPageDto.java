package com.springboot.vitaltrail.api.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AdminSubscriptionsPageDto {
    private List<AdminSubscriptionItemDto> subscriptions;
    private long total;
    private int page;
    private int size;
}
