package com.springboot.vitaltrail.api.user;

import com.springboot.vitaltrail.domain.user.UserEntity;
import com.springboot.vitaltrail.api.user.admin.AdminAssembler;
import com.springboot.vitaltrail.api.user.client.ClientAssembler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAssembler {
    private final AdminAssembler adminAssembler;
    private final ClientAssembler clientAssembler;

    public UserDto toUserResponse(UserEntity userEntity, String token, String refreshToken) {
        return buildUser(userEntity, token, refreshToken);
    }

    private UserDto buildUser(UserEntity userEntity, String token, String refreshToken) {
        UserDto.UserDtoBuilder builder = UserDto.builder()
            .idUser(userEntity.getIdUser())
            .email(userEntity.getEmail())
            .username(userEntity.getUsername())
            .token(token)
            .refreshToken(refreshToken)
            .name(userEntity.getName())
            .surname(userEntity.getSurname())
            .birthday(userEntity.getBirthday())
            .bio(userEntity.getBio())
            .imgUser(userEntity.getImgUser())
            .rol(userEntity.getRol())
            .isActive(userEntity.getIsActive())
            .isDeleted(userEntity.getIsDeleted())
            .isPremium(userEntity.getIsPremium())
            .admin(userEntity.getAdmin() != null ? adminAssembler.toAdminResponse(userEntity.getAdmin()) : null)
            .client(userEntity.getClient() != null ? clientAssembler.toClientResponse(userEntity.getClient()) : null);

        return builder.build();
    }
}
