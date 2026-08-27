package com.portofolio.medical_app.dto;

public record RegisterRequest(
        String email,
        String password,
        String role
) {

}
