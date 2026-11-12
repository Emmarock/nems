package com.nitelestate.qrtag;

import com.nitelestate.qrtag.dto.QrTagAssignRequest;
import com.nitelestate.qrtag.dto.QrTagBatchRequest;
import com.nitelestate.qrtag.dto.QrTagBatchResponse;
import com.nitelestate.qrtag.dto.QrTagResponse;
import com.nitelestate.security.CurrentUser;
import com.nitelestate.user.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/** Pre-generated QR tags: print in bulk, link to a vehicle or property later (see QrTagService). */
@RestController
@RequestMapping("/api/v1/qr-tags")
@RequiredArgsConstructor
public class QrTagController {

    /** Property tags: the admins plus the treasurer and financial secretary (secretary and security link vehicle tags only). */
    private static final Set<Role> PROPERTY_TAG_ROLES = Set.of(Role.SUPER_ADMIN, Role.CDA_ADMIN, Role.TREASURER, Role.FINANCIAL_SECRETARY);

    private final QrTagService qrTagService;
    private final CurrentUser currentUser;

    /** Generates a new batch of tags and returns its printable sheet straight away. */
    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public ResponseEntity<byte[]> generate(@Valid @RequestBody QrTagBatchRequest request) {
        int batchNo = qrTagService.generateBatch(request.kind(), request.count());
        return pdf(batchNo);
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public List<QrTagBatchResponse> batches() {
        return qrTagService.batches();
    }

    /** Reprints an earlier batch - the same codes and QRs, nothing new is generated. */
    @GetMapping("/batches/{batchNo}/pdf")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY')")
    public ResponseEntity<byte[]> batchPdf(@PathVariable int batchNo) {
        return pdf(batchNo);
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECRETARY', 'SECURITY')")
    public QrTagResponse assign(@Valid @RequestBody QrTagAssignRequest request) {
        requireRoleFor(request.kind());
        return qrTagService.assign(request.kind(), request.subjectId(), request.code(), currentUser.userId());
    }

    /** The printed tag behind a vehicle/property's current pass; 204 when its pass isn't from a printed tag. */
    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECRETARY', 'SECURITY')")
    public ResponseEntity<QrTagResponse> current(@RequestParam QrTagKind kind, @RequestParam Long subjectId) {
        requireRoleFor(kind);
        return qrTagService.currentTag(kind, subjectId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private void requireRoleFor(QrTagKind kind) {
        if (kind == QrTagKind.PROPERTY && !PROPERTY_TAG_ROLES.contains(currentUser.role())) {
            throw new AccessDeniedException("Only the super admin, CDA admin, treasurer or financial secretary can link property tags");
        }
    }

    private ResponseEntity<byte[]> pdf(int batchNo) {
        byte[] pdf = qrTagService.batchPdf(batchNo);
        String filename = "qr-tags-batch-" + batchNo + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .header("X-Batch-No", String.valueOf(batchNo))
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Batch-No")
                .body(pdf);
    }
}
