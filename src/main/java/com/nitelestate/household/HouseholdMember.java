package com.nitelestate.household;

import com.nitelestate.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A tenant/resident's spouse or other family member, living at the same property long-term —
 * distinct from a short-lived Visitor pass and from a Worker's own tenant-set date range: here
 * the tenant only sponsors the request, and the property's landlord approves it. There's no
 * validity window to set — once approved, the pass is as good as the home owner's own, valid
 * indefinitely until the sponsor, the landlord, or an admin revokes it (see
 * HouseholdMemberService.approve/revoke).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "household_member")
public class HouseholdMember extends BaseEntity {

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Relationship relationship;

    /** The property this household member lives at — resolved from the sponsor's own property. */
    @Column(nullable = false)
    private Long propertyId;

    /** The resident (tenant or owner) who requested this pass. */
    @Column(nullable = false)
    private Long sponsorResidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private HouseholdMemberStatus status = HouseholdMemberStatus.PENDING_APPROVAL;

    @Column(unique = true)
    private String qrToken;

    /**
     * A photo submitted at request time — lets security visually confirm identity against the QR
     * pass at the gate. Stored as a base64 data URI, same convention as Worker/Visitor photos.
     */
    @Column(columnDefinition = "text")
    private String photo;
}
