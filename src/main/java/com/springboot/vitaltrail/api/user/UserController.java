package com.springboot.vitaltrail.api.user;

import com.springboot.vitaltrail.domain.user.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.springboot.vitaltrail.api.security.authorization.CheckSecurity;
// import org.springframework.validation.annotation.Validated;
// import jakarta.validation.Valid;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
// @Validated
public class UserController {
    private final UserService userService;

    @GetMapping
    @CheckSecurity.Protected.canManage
    public UserDto getCurrentUser() {
        return userService.getCurrentUser();
        
    }
}
