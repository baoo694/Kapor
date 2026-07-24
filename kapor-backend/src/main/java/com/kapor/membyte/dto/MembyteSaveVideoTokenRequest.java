package com.kapor.membyte.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MembyteSaveVideoTokenRequest {

    @NotBlank
    private String surface;
}
