package com.springboot.vitaltrail.domain.refreshToken;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public RefreshTokenEntity getRefreshToken(UUID idUser) {
        return refreshTokenRepository.findByIdUser(idUser).orElseThrow(() -> new AppException(Error.REFRESH_TOKEN_NOT_FOUND));
    }
}
