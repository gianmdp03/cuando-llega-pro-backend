package com.gianmdp03.cuando_llega_pro.domain.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorReportResponseDTO {

    private UUID id;
    private Instant createdAt;
    private String errorName;
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
    private Long userId;

    public static ErrorReportResponseDTO fromEntity(ErrorReportEntity entity) {
        return ErrorReportResponseDTO.builder()
                .id(entity.getId())
                .createdAt(entity.getCreatedAt())
                .errorName(entity.getErrorName())
                .errorMessage(entity.getErrorMessage())
                .errorStack(entity.getErrorStack())
                .httpStatus(entity.getHttpStatus())
                .screen(entity.getScreen())
                .lineCode(entity.getLineCode())
                .stopId(entity.getStopId())
                .bandera(entity.getBandera())
                .appVersion(entity.getAppVersion())
                .os(entity.getOs())
                .osVersion(entity.getOsVersion())
                .metadataJson(entity.getMetadataJson())
                .userId(entity.getUserId())
                .build();
    }
}
