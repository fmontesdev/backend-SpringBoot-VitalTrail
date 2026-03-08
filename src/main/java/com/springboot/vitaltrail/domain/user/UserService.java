package com.springboot.vitaltrail.domain.user;

import com.springboot.vitaltrail.api.user.UserDto;
import java.util.UUID;

public interface UserService {
    UserDto getCurrentUser();

    UserEntity getUserByUsername(final String username);

    UserEntity getUserByIdUser(final UUID idUser);

    void saveIsPremium(final UserEntity userEntity, final boolean isPremium);
}
