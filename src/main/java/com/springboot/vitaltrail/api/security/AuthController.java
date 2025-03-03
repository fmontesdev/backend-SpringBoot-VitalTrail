package com.springboot.vitaltrail.api.security;


import com.springboot.vitaltrail.api.user.UserDto;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.springboot.vitaltrail.api.security.authorization.CheckSecurity;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import java.util.Map;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @CheckSecurity.Public.canRead
    public UserDto login(@RequestBody @Valid UserDto.Login login) {
        return authService.login(login);
    }

    @PostMapping("/logout")
    @CheckSecurity.Logout.canBlacklisted
    public ResponseEntity<Map<String, String>> logout() {
        var blacklistToken = authService.saveBlacklistToken();

        if (blacklistToken != null) {
            Map<String, String> response = Map.of(
                "message", "RefreshToken guardado en la Blacklist"
            );
            return ResponseEntity.ok(response);
        }

        throw new AppException(Error.BLACKLISTED_TOKEN);
    }
}
