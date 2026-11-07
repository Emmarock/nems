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

/**
 * CCTV face search - searching is admins only, since results are biometric matches on named
 * people. The whole feature sits behind nitel.face-match.enabled (NITEL_FACE_MATCH_ENABLED).
 */
@RestController
@RequestMapping("/api/v1/face-search")
@RequiredArgsConstructor
public class FaceSearchController {

    private final FaceSearchService faceSearchService;
    private final FaceEngine faceEngine;

    /** Readable by any signed-in user: the frontend hides every face-search mention when this is false. */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Boolean> status() {
        return Map.of("enabled", faceEngine.isEnabled());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN')")
    public FaceSearchResponse search(@Valid @RequestBody FaceSearchRequest request) {
        return faceSearchService.search(request.image(), request.note());
    }
}
