package com.nitelestate.facematch;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nitel.face-match")
public record FaceMatchProperties(boolean enabled, String region, String collectionId, float minSimilarity) {
}
