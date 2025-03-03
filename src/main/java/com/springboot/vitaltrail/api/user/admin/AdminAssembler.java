package com.springboot.vitaltrail.api.user.admin;

import com.springboot.vitaltrail.domain.user.admin.AdminEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminAssembler {
    public AdminDto toAdminResponse(AdminEntity adminEntity) {
        return AdminDto.builder()
            .idAdmin(adminEntity.getIdAdmin())
            .user(adminEntity.getUser().getIdUser())
            .build();
    }
}
