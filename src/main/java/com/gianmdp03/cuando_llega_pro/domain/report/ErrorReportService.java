package com.gianmdp03.cuando_llega_pro.domain.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ErrorReportService {

    private final ErrorReportRepository errorReportRepository;

    public ErrorReportService(ErrorReportRepository errorReportRepository) {
        this.errorReportRepository = errorReportRepository;
    }

    @Transactional
    public ErrorReportResponseDTO recordErrorReport(CreateErrorReportDTO dto, Long userId) {
        ErrorReportEntity entity = ErrorReportEntity.builder()
                .errorName(dto.getErrorName())
                .errorMessage(dto.getErrorMessage())
                .errorStack(dto.getErrorStack())
                .httpStatus(dto.getHttpStatus())
                .screen(dto.getScreen())
                .lineCode(dto.getLineCode())
                .stopId(dto.getStopId())
                .bandera(dto.getBandera())
                .appVersion(dto.getAppVersion())
                .os(dto.getOs())
                .osVersion(dto.getOsVersion())
                .metadataJson(dto.getMetadataJson())
                .userId(userId)
                .build();

        ErrorReportEntity saved = errorReportRepository.save(entity);
        return ErrorReportResponseDTO.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<ErrorReportResponseDTO> getRecentReports(Pageable pageable) {
        return errorReportRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ErrorReportResponseDTO::fromEntity);
    }

    @Transactional
    public void deleteReport(UUID id) {
        errorReportRepository.deleteById(id);
    }
}
