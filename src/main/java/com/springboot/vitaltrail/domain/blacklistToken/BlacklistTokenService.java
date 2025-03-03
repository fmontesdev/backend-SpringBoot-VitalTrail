package com.springboot.vitaltrail.domain.blacklistToken;

public interface BlacklistTokenService {
    boolean isBlacklisted(final String refreshToken);

    BlacklistTokenEntity saveBlacklistToken(final String refreshToken);
}
