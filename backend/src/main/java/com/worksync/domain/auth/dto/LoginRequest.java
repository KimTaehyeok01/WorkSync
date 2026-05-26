package com.worksync.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {

    @NotBlank
    private String empNo;

    @NotBlank
    private String password;
}
