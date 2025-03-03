package com.springboot.vitaltrail.api.security;

import com.springboot.vitaltrail.api.user.UserDto;
import com.springboot.vitaltrail.api.user.UserAssembler;
import com.springboot.vitaltrail.domain.user.UserRepository;
import com.springboot.vitaltrail.domain.refreshToken.RefreshTokenRepository;
import com.springboot.vitaltrail.domain.blacklistToken.BlacklistTokenEntity;
import com.springboot.vitaltrail.domain.blacklistToken.BlacklistTokenRepository;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;
import com.springboot.vitaltrail.api.security.jwt.JWTUtils;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final UserAssembler userAssembler;
    private final BlacklistTokenRepository blacklistTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final AuthUtils authUtils;

    @Transactional()
    @Override
    public UserDto login(final UserDto.Login login) {
        
        var user = userRepository.findByEmail(login.getEmail())
            .orElseThrow(() -> new AppException(Error.USER_NOT_FOUND));

        if (!passwordEncoder.matches(login.getPassword(), user.getPassword())) {
            throw new AppException(Error.PASSWORD_INVALID);
        }

        // // Si el usuario no está activo, lo activa
        // if (user.getIs_active() == 0) {
        //     user.setIs_active(1);
        //     userRepository.save(user);
        // }

        var accessToken = jwtUtils.generateJWT(user.getIdUser(), user.getEmail(), user.getUsername(), user.getRol(), "access");
        var refreshToken = jwtUtils.generateJWT(user.getIdUser(), user.getEmail(), user.getUsername(), user.getRol(), "refresh");
        // System.out.println("AccessToken ========================================================\n" + accessToken);
        // System.out.println("RefreshToken ========================================================\n" + refreshToken);

        // // Inserta o actualiza el refreshToken en la base de datos
        // if (refreshToken != null && refreshToken != "") {
        //     var existingToken = refreshTokenRepository.findByIdUser(user.getIdUser());
        //     RefreshTokenEntity refreshTokenEntity = existingToken.orElse(
        //         RefreshTokenEntity.builder()
        //             .idUser(user.getIdUser())
        //             .build()
        //     );
        //     refreshTokenEntity.setRefreshToken(refreshToken);
        //     refreshTokenRepository.saveAndFlush(refreshTokenEntity);
        // }

        return userAssembler.toUserResponse(user, accessToken, refreshToken);
    }

    @Transactional()
    @Override
    public BlacklistTokenEntity saveBlacklistToken() {
        UUID idUser = authUtils.getCurrentUserId();

        var refreshTokenEntity = refreshTokenRepository.findByIdUser(idUser).orElseThrow(() -> new AppException(Error.REFRESH_TOKEN_NOT_FOUND));
        String refresToken = refreshTokenEntity.getRefreshToken();

        var blacklistToken  = blacklistTokenRepository.findByRefreshToken(refresToken);
        if (blacklistToken.isEmpty()) {
            BlacklistTokenEntity blacklistTokenEntity = BlacklistTokenEntity.builder()
                .refreshToken(refresToken)
                .build();
            return blacklistTokenRepository.save(blacklistTokenEntity);
        }

        return null;
    }
}
