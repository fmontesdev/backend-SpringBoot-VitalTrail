package com.springboot.vitaltrail.api.user;

import lombok.*;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.springboot.vitaltrail.api.user.admin.AdminDto;
import com.springboot.vitaltrail.api.user.client.ClientDto;
import com.springboot.vitaltrail.domain.user.UserEntity.Role;
import java.util.UUID;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class UserDto {
    private UUID idUser;
    @NotNull
    private String email;
    @NotNull
    private String username;
    @NotNull
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String password;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshToken;
    @NotNull
    private String name;
    @NotNull
    private String surname;
    private LocalDateTime birthday;
    private String bio;
    private String imgUser;
    @NotNull
    private Role rol;
    private Boolean isActive;
    private Boolean isDeleted;
    private Boolean isPremium;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private AdminDto admin;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ClientDto client;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Login {
        @NotNull(message = "El email no puede ser nulo")
        @Email(message = "El email debe tener un formato válido")
        private String email;

        @NotBlank(message = "La contraseña no puede ser nula o vacía")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,32}$",
            message = "La contraseña debe tener al menos 8 caracteres, incluyendo mayúsculas, minúsculas y números"
        )
        private String password;
    }
}
