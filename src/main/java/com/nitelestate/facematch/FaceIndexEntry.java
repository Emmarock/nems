package com.nitelestate.facematch;

import com.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "face_index_entry")
public class FaceIndexEntry extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FaceSubjectType subjectType;

    @Column(nullable = false)
    private Long subjectId;

    @Column(nullable = false, unique = true)
    private String faceId;
}
