package com.nitelestate.facematch;

/** The kinds of registered people whose photos are indexed for CCTV matching. */
public enum FaceSubjectType {
    WORKER,
    HOUSEHOLD_MEMBER;

    /** Rekognition's externalImageId for a subject, e.g. "WORKER_12". */
    public String externalId(Long id) {
        return name() + "_" + id;
    }
}
