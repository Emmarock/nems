package com.nitelestate.facematch;

import com.nitelestate.facematch.dto.FaceSearchRequest;
import com.nitelestate.facematch.dto.FaceSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** CCTV face search - admins only, since results are biometric matches on named people. */
@RestController
@RequestMapping("/api/v1/face-search")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
public class FaceSearchController {

    private final FaceSearchService faceSearchService;
    private final FaceEngine faceEngine;

    @GetMapping("/status")
    public Map<String, Boolean> status() {
        return Map.of("enabled", faceEngine.isEnabled());
    }

    @PostMapping
    public FaceSearchResponse search(@Valid @RequestBody FaceSearchRequest request) {
        return faceSearchService.search(request.image(), request.note());
    }
}
