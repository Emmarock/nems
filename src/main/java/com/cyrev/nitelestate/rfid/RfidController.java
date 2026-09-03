package com.cyrev.nitelestate.rfid;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.rfid.dto.RfidTagRequest;
import com.cyrev.nitelestate.rfid.dto.RfidTagResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rfid")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'SECURITY')")
public class RfidController {

    private final RfidService rfidService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RfidTagResponse issue(@Valid @RequestBody RfidTagRequest request) {
        return rfidService.issue(request);
    }

    @PutMapping("/{id}/revoke")
    public RfidTagResponse revoke(@PathVariable Long id) {
        return rfidService.setStatus(id, RfidStatus.REVOKED);
    }

    @PutMapping("/{id}/lost")
    public RfidTagResponse markLost(@PathVariable Long id) {
        return rfidService.setStatus(id, RfidStatus.LOST);
    }

    @GetMapping("/verify/{tagId}")
    public RfidTagResponse verify(@PathVariable String tagId) {
        return rfidService.verify(tagId);
    }

    @GetMapping
    public PageResponse<RfidTagResponse> findAll(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size) {
        return rfidService.search(Paging.of(page, size, Sort.by("tagId")));
    }
}
