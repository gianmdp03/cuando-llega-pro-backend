package com.gianmdp03.cuando_llega_pro.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestErrorController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    record SamplePayload(@NotBlank(message = "Field name cannot be blank") String name) {}

    @RestController
    static class TestErrorController {

        @PostMapping("/test/validation")
        void triggerValidation(@Valid @RequestBody SamplePayload payload) {
        }

        @GetMapping("/test/not-found")
        void triggerNotFound() {
            throw new ResourceNotFoundException("Entity with id 42 was not found");
        }

        @GetMapping("/test/upstream-error")
        void triggerUpstreamError() {
            throw new UpstreamServiceException("Upstream proxy gateway timeout");
        }

        @GetMapping("/test/bad-request")
        void triggerBadRequest() {
            throw new BadRequestException("Invalid query parameter");
        }

        @GetMapping("/test/illegal-argument")
        void triggerIllegalArgument() {
            throw new IllegalArgumentException("Negative offset supplied");
        }

        @GetMapping("/test/access-denied")
        void triggerAccessDenied() {
            throw new AccessDeniedException("Insufficient role privileges");
        }

        @GetMapping("/test/bad-credentials")
        void triggerBadCredentials() {
            throw new BadCredentialsException("Invalid username or password");
        }

        @GetMapping("/test/generic-error")
        void triggerGenericError() {
            throw new RuntimeException("Unexpected database failure");
        }
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with 400 and validation errors")
    void handleMethodArgumentNotValid() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Validation Error")))
                .andExpect(jsonPath("$.detail", is("Invalid request payload")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field", is("name")))
                .andExpect(jsonPath("$.errors[0].message", is("Field name cannot be blank")));
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException with 404")
    void handleResourceNotFound() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.title", is("Resource Not Found")))
                .andExpect(jsonPath("$.detail", is("Entity with id 42 was not found")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle UpstreamServiceException with 502")
    void handleUpstreamService() throws Exception {
        mockMvc.perform(get("/test/upstream-error"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status", is(502)))
                .andExpect(jsonPath("$.title", is("Upstream Proxy Failure")))
                .andExpect(jsonPath("$.detail", is("Upstream proxy gateway timeout")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle BadRequestException with 400")
    void handleBadRequest() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("Invalid query parameter")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400")
    void handleIllegalArgument() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("Negative offset supplied")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle AccessDeniedException with 403")
    void handleAccessDenied() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.title", is("Access Denied")))
                .andExpect(jsonPath("$.detail", is("Insufficient role privileges")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle BadCredentialsException with 401")
    void handleBadCredentials() throws Exception {
        mockMvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.title", is("Authentication Failed")))
                .andExpect(jsonPath("$.detail", is("Invalid username or password")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle generic Exception with 500")
    void handleGenericException() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.title", is("Internal Server Error")))
                .andExpect(jsonPath("$.detail", is("An unexpected error occurred. Please contact support.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
