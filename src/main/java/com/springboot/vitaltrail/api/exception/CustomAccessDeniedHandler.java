package com.springboot.vitaltrail.api.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        AppException appException = new AppException(Error.FORBIDDEN);

        ErrorMessages errorMessages = new ErrorMessages();
        errorMessages.addError(appException.getError().name(), appException.getMessage());

        response.setStatus(appException.getError().getStatus().value());
        response.setContentType("application/json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(response.getWriter(), errorMessages);
    }
}
