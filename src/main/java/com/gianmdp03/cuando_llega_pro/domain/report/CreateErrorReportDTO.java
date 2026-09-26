package com.gianmdp03.cuando_llega_pro.domain.report;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateErrorReportDTO {

    @NotBlank(message = "errorName is required")
    private String errorName;

    @NotBlank(message = "errorMessage is required")
    private String errorMessage;

    private String errorStack;
    private Integer httpStatus;
    private String screen;
    private String lineCode;
    private String stopId;
    private String bandera;
    private String appVersion;
    private String os;
    private String osVersion;
    private String metadataJson;
}
