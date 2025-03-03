package com.springboot.vitaltrail.api.user.admin;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class AdminDto {
    private Long idAdmin;
    @NotNull
    private UUID user;
}
