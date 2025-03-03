package com.springboot.vitaltrail.domain.blacklistToken;

// import com.springboot.entrename.domain.exception.AppException;
// import com.springboot.entrename.domain.exception.Error;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlacklistTokenServiceImpl implements BlacklistTokenService {
    private final BlacklistTokenRepository blacklistTokenRepository;

    @Override
    public boolean isBlacklisted(String refreshToken) {
        if(blacklistTokenRepository.findByRefreshToken(refreshToken).isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public BlacklistTokenEntity saveBlacklistToken(String refreshToken) {
        var blacklistToken = BlacklistTokenEntity.builder()
            .refreshToken(refreshToken)
            .build();
        return blacklistTokenRepository.save(blacklistToken);
    }
}
