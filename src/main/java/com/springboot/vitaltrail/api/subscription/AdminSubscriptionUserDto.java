package com.springboot.vitaltrail.api.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AdminSubscriptionUserDto {
    private String username;
    private String email;
    private String imgUser;
    private String name;
    private String surname;
}
