package com.nitelestate.facematch;

import java.util.List;

/**
 * The face-recognition backend. RekognitionFaceEngine in production; tests swap in a stub so
 * nothing calls AWS.
 */
public interface FaceEngine {

    boolean isEnabled();

    /** Indexes the single most prominent face in the image; returns its face id, or null if no usable face. */
    String index(byte[] image, String externalId);

    void delete(List<String> faceIds);

    /** Bounding boxes of every face in the image, as fractions of width/height. */
    List<Box> detect(byte[] image);

    /** Searches the largest face in the image against the collection. Empty if no face could be read. */
    List<Hit> search(byte[] image, float minSimilarity, int maxResults);

    record Box(float left, float top, float width, float height) {
    }

    record Hit(String faceId, String externalId, float similarity) {
    }
}
