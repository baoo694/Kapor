package com.kapor.techtalk.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RoleplayHintDto {
    private List<String> keywords;
    private String sentenceStructure;
    private String politenessTip;
}
