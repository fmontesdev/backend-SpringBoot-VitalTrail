package com.springboot.vitaltrail.api.security;

import com.springboot.vitaltrail.api.user.UserDto;
import com.springboot.vitaltrail.domain.blacklistToken.BlacklistTokenEntity;

public interface AuthService {
    UserDto login(final UserDto.Login login);

    BlacklistTokenEntity saveBlacklistToken();
}
