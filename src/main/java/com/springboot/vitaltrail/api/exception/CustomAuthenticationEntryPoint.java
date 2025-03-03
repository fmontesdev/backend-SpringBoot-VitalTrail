package com.springboot.vitaltrail.api.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.vitaltrail.domain.exception.AppException;
import com.springboot.vitaltrail.domain.exception.Error;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        AppException appException = getAppException(request, authException);
        writeErrorResponse(response, appException);
    }

    private AppException getAppException(HttpServletRequest request, AuthenticationException authException) {
        Object exceptionAttribute = request.getAttribute("APP_EXCEPTION");
        if (exceptionAttribute instanceof AppException) {
            return (AppException) exceptionAttribute;
        } else if (authException.getCause() instanceof AppException) {
            return (AppException) authException.getCause();
        } else {
            return new AppException(Error.UNAUTHORIZED);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, AppException appException) throws IOException {
        ErrorMessages errorMessages = new ErrorMessages();
        errorMessages.addError(appException.getError().name(), appException.getMessage());
        response.setStatus(appException.getError().getStatus().value());
        response.setContentType("application/json");
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(response.getWriter(), errorMessages);
    }
}
