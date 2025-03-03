package com.springboot.vitaltrail.domain.user;

import com.springboot.vitaltrail.api.user.UserDto;

public interface UserService {
    UserDto getCurrentUser();

    UserEntity getUserByUsername(final String username);
}
