package com.worksync.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReissueRequest {

    @NotBlank
    private String refreshToken;
}
