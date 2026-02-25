package com.example.userauth.dto;

public record EmployerAccessDto(
        Long employerId,
        String establishmentName,
        String registrationNumber) {
}
