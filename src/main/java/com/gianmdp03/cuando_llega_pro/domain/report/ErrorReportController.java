package com.gianmdp03.cuando_llega_pro.domain.report;

import com.gianmdp03.cuando_llega_pro.domain.user.User;
import com.gianmdp03.cuando_llega_pro.domain.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ErrorReportController {

    private final ErrorReportService errorReportService;
    private final UserRepository userRepository;

    public ErrorReportController(ErrorReportService errorReportService, UserRepository userRepository) {
        this.errorReportService = errorReportService;
        this.userRepository = userRepository;
    }

    /**
     * Public endpoint to submit an error report with raw stack traces and diagnostics.
     * Accessible without authentication so failed network/auth requests can be reported.
     */
    @PostMapping("/error-reports")
    public ResponseEntity<ErrorReportResponseDTO> submitErrorReport(
            @Valid @RequestBody CreateErrorReportDTO request,
            Principal principal
    ) {
        Long userId = null;
        if (principal != null && principal.getName() != null) {
            userId = userRepository.findByEmail(principal.getName())
                    .map(User::getId)
                    .orElse(null);
        }

        ErrorReportResponseDTO response = errorReportService.recordErrorReport(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Admin-only endpoint to list recent error reports with full details.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/error-reports")
    public ResponseEntity<Page<ErrorReportResponseDTO>> getRecentReports(
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ErrorReportResponseDTO> reports = errorReportService.getRecentReports(pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * Admin-only endpoint to delete a resolved or inspected error report.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/error-reports/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID id) {
        errorReportService.deleteReport(id);
        return ResponseEntity.noContent().build();
    }
}
