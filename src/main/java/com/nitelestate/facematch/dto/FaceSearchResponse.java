package com.nitelestate.facematch.dto;

import com.nitelestate.facematch.FaceSubjectType;

import java.util.List;

/** One entry per face found in the uploaded still, each with its candidate matches (best first). */
public record FaceSearchResponse(int facesFound, List<FaceResult> faces) {

    /** faceImage is the crop that was searched, so the admin can see which face each result is for. */
    public record FaceResult(String faceImage, List<Candidate> candidates) {
    }

    public record Candidate(
            FaceSubjectType subjectType,
            Long subjectId,
            String fullName,
            String phone,
            String photo,
            String status,
            /** Worker: work type / contractor. Household member: relationship. */
            String role,
            /** The resident who registered them (employer / sponsor). */
            String sponsorName,
            String sponsorPhone,
            String propertyHouseNumber,
            float similarity
    ) {
    }
}
