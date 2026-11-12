package com.nitelestate.qrtag;

import com.nitelestate.audit.AuditService;
import com.nitelestate.common.exception.BadRequestException;
import com.nitelestate.common.exception.ConflictException;
import com.nitelestate.common.exception.NotFoundException;
import com.nitelestate.property.Property;
import com.nitelestate.property.PropertyRepository;
import com.nitelestate.qrtag.dto.QrTagBatchResponse;
import com.nitelestate.qrtag.dto.QrTagResponse;
import com.nitelestate.vehicle.Vehicle;
import com.nitelestate.vehicle.VehicleRepository;
import com.nitelestate.vehicle.VehicleStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pre-generated QR tags: print a batch of stickers up front, then link each sticker to a vehicle or
 * property when it's handed out. Linking copies the tag's token onto the subject's qr_token, so
 * every existing gate scan / lookup path works without knowing tags exist.
 *
 * <p>A linked tag is never re-linked (see V45__qr_tag.sql): a sticker already stuck on one car must
 * never start identifying another.
 */
@Service
public class QrTagService {

    /** Printed code typed by hand: "V-000123", "v123", "P 42" ... */
    private static final Pattern CODE = Pattern.compile("(?i)^\\s*([VP])\\s*-?\\s*0*(\\d{1,9})\\s*$");
    /** A scanned tag: the full gate URL it encodes. */
    private static final Pattern SCAN_URL = Pattern.compile("/scan/(vehicle|property)/([^/?#\\s]+)");

    private final QrTagRepository qrTagRepository;
    private final VehicleRepository vehicleRepository;
    private final PropertyRepository propertyRepository;
    private final AuditService auditService;
    private final String appBaseUrl;

    public QrTagService(QrTagRepository qrTagRepository, VehicleRepository vehicleRepository,
                        PropertyRepository propertyRepository, AuditService auditService,
                        @Value("${nitel.app.base-url}") String appBaseUrl) {
        this.qrTagRepository = qrTagRepository;
        this.vehicleRepository = vehicleRepository;
        this.propertyRepository = propertyRepository;
        this.auditService = auditService;
        this.appBaseUrl = appBaseUrl.replaceAll("/+$", "");
    }

    /** Creates {@code count} new tags with the next serial numbers and returns the batch number. */
    @Transactional
    public int generateBatch(QrTagKind kind, int count) {
        int batchNo = qrTagRepository.maxBatchNo() + 1;
        int serial = qrTagRepository.maxSerial(kind);
        List<QrTag> tags = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            QrTag tag = new QrTag();
            tag.setKind(kind);
            tag.setSerial(++serial);
            tag.setCode(kind.code(serial));
            tag.setToken(UUID.randomUUID().toString());
            tag.setBatchNo(batchNo);
            tags.add(tag);
        }
        try {
            qrTagRepository.saveAllAndFlush(tags);
        } catch (DataIntegrityViolationException e) {
            // Two batches generated at the same moment raced for the same serials.
            throw new ConflictException("Another batch was being generated at the same time - please try again");
        }
        auditService.record("QrTag", null, "GENERATE_BATCH",
                "batch " + batchNo + ": " + count + " " + kind + " tag(s), " + tags.get(0).getCode() + " to " + tags.get(count - 1).getCode());
        return batchNo;
    }

    public byte[] batchPdf(int batchNo) {
        List<QrTag> tags = qrTagRepository.findByBatchNoOrderBySerial(batchNo);
        if (tags.isEmpty()) {
            throw new NotFoundException("No QR tag batch " + batchNo);
        }
        QrTagKind kind = tags.get(0).getKind();
        return QrTagSheetPdf.render(kind, batchNo, tags.stream()
                .map(t -> new QrTagSheetPdf.Tag(t.getCode(), scanUrl(t)))
                .toList());
    }

    public List<QrTagBatchResponse> batches() {
        return qrTagRepository.batches().stream()
                .map(r -> new QrTagBatchResponse(r.getBatchNo(), r.getKind(), r.getTotal(), r.getLinked(),
                        r.getFirstCode(), r.getLastCode(), r.getCreatedAt()))
                .toList();
    }

    /** The printed tag behind this vehicle/property's current pass, if it came from one. */
    public Optional<QrTagResponse> currentTag(QrTagKind kind, Long subjectId) {
        String token = currentToken(kind, subjectId);
        if (token == null) {
            return Optional.empty();
        }
        return qrTagRepository.findByKindAndToken(kind, token).map(this::toResponse);
    }

    /**
     * Links a printed tag to a vehicle or property. The subject's previous pass (an in-app QR or an
     * earlier sticker) stops working, the same way re-issuing a pass always has.
     */
    @Transactional
    public QrTagResponse assign(QrTagKind kind, Long subjectId, String input, Long assignedByUserId) {
        QrTag tag = resolve(input, kind);
        if (tag.getKind() != kind) {
            throw new BadRequestException(tag.getCode() + " is a " + tag.getKind().label().toLowerCase()
                    + " tag - it can't be linked to a " + kind.label().toLowerCase());
        }
        if (tag.getSubjectId() != null) {
            if (tag.getSubjectId().equals(subjectId) && statusOf(tag) == QrTagStatus.ASSIGNED) {
                return toResponse(tag);
            }
            throw new ConflictException(tag.getCode() + " has already been used for " + describe(kind, tag.getSubjectId())
                    + " - a tag can only ever be linked once. Use a fresh tag.");
        }

        String previousToken;
        if (kind == QrTagKind.VEHICLE) {
            Vehicle vehicle = vehicleRepository.findById(subjectId)
                    .orElseThrow(() -> new NotFoundException("Vehicle not found"));
            if (vehicle.getStatus() == VehicleStatus.PENDING_APPROVAL) {
                throw new BadRequestException("This vehicle is awaiting the landlord's approval - approve it before linking a tag");
            }
            if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
                throw new BadRequestException("This vehicle has been deactivated - reactivate it before linking a tag");
            }
            previousToken = vehicle.getQrToken();
            vehicle.setQrToken(tag.getToken());
            vehicleRepository.save(vehicle);
        } else {
            Property property = propertyRepository.findById(subjectId)
                    .orElseThrow(() -> new NotFoundException("Property not found"));
            previousToken = property.getQrToken();
            property.setQrToken(tag.getToken());
            propertyRepository.save(property);
        }

        tag.setSubjectId(subjectId);
        tag.setAssignedAt(Instant.now());
        tag.setAssignedByUserId(assignedByUserId);
        qrTagRepository.save(tag);

        String replaced = previousToken == null ? "no previous pass"
                : qrTagRepository.findByKindAndToken(kind, previousToken).map(t -> "replaces tag " + t.getCode())
                .orElse("replaces the previous in-app pass");
        auditService.record(kind.label(), subjectId, "LINK_QR_TAG", tag.getCode() + " (" + replaced + ")");
        return toResponse(tag);
    }

    private QrTag resolve(String input, QrTagKind kind) {
        String text = input == null ? "" : input.trim();
        Matcher url = SCAN_URL.matcher(text);
        if (url.find()) {
            return qrTagRepository.findByToken(url.group(2))
                    .orElseThrow(() -> new NotFoundException("That QR code isn't a pre-printed tag. Scan a tag from a printed sheet."));
        }
        Matcher code = CODE.matcher(text);
        if (code.matches()) {
            String normalised = code.group(1).toUpperCase() + "-" + String.format("%06d", Long.parseLong(code.group(2)));
            return qrTagRepository.findByCodeIgnoreCase(normalised)
                    .orElseThrow(() -> new NotFoundException("No QR tag " + normalised + " has been generated"));
        }
        return qrTagRepository.findByToken(text)
                .orElseThrow(() -> new NotFoundException("No QR tag matches \"" + text + "\". Enter the code printed under the QR, e.g. "
                        + kind.code(123) + "."));
    }

    QrTagStatus statusOf(QrTag tag) {
        if (tag.getSubjectId() == null) {
            return QrTagStatus.AVAILABLE;
        }
        return tag.getToken().equals(currentToken(tag.getKind(), tag.getSubjectId())) ? QrTagStatus.ASSIGNED : QrTagStatus.VOID;
    }

    private String currentToken(QrTagKind kind, Long subjectId) {
        return kind == QrTagKind.VEHICLE
                ? vehicleRepository.findById(subjectId).map(Vehicle::getQrToken).orElse(null)
                : propertyRepository.findById(subjectId).map(Property::getQrToken).orElse(null);
    }

    private String describe(QrTagKind kind, Long subjectId) {
        return kind == QrTagKind.VEHICLE
                ? vehicleRepository.findById(subjectId).map(v -> "vehicle " + v.getPlateNumber()).orElse("another vehicle")
                : propertyRepository.findById(subjectId).map(p -> "property " + p.getHouseNumber()).orElse("another property");
    }

    private String scanUrl(QrTag tag) {
        return appBaseUrl + "/scan/" + tag.getKind().scanPath() + "/" + tag.getToken();
    }

    private QrTagResponse toResponse(QrTag tag) {
        return new QrTagResponse(tag.getId(), tag.getKind(), tag.getCode(), tag.getBatchNo(), statusOf(tag),
                tag.getSubjectId(), tag.getAssignedAt());
    }
}
