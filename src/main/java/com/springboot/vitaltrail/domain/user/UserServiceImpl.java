package com.springboot.vitaltrail.domain.user;

import com.springboot.vitaltrail.api.user.UserDto;
import com.springboot.vitaltrail.api.user.UserAssembler;
import com.springboot.vitaltrail.api.security.AuthUtils;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserAssembler userAssembler;
    private final UserRepository userRepository;
    private final AuthUtils authUtils;

    @Transactional(readOnly = true)
    @Override
    public UserDto getCurrentUser() {
        var user = getByEmail(authUtils.getCurrentUserEmail());
        return userAssembler.toUserResponse(user, null, null);
    }

    @Transactional(readOnly = true)
    // @Override
    public UserEntity getByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new AppException(Error.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Override
    public UserEntity getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(() -> new AppException(Error.USER_NOT_FOUND));
    }
}
