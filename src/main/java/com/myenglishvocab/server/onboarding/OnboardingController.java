package com.myenglishvocab.server.onboarding;

import com.myenglishvocab.server.auth.jwt.JwtPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingCatalog catalog;
    private final OnboardingService service;

    @GetMapping("/status")
    public StatusResponse status(@AuthenticationPrincipal JwtPrincipal principal) {
        return new StatusResponse(service.isEligible(principal.userId()));
    }

    @GetMapping("/catalog")
    public OnboardingCatalog.Catalog catalog() {
        return catalog.get();
    }

    @PostMapping("/complete")
    public CompleteResponse complete(@AuthenticationPrincipal JwtPrincipal principal,
                                     @Valid @RequestBody CompleteRequest request) {
        return service.complete(principal.userId(), request);
    }

    public record StatusResponse(boolean eligible) {}
    public record CompleteRequest(@Min(1) int version, @NotBlank @Size(max = 30) String trackId,
                                  @NotNull @Size(max = 10) List<@NotBlank @Size(max = 50) String> wordIds) {}
    public record CompleteResponse(int addedCount, int existingCount) {}
}
