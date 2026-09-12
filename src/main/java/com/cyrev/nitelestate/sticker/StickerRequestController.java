package com.cyrev.nitelestate.sticker;

import com.cyrev.nitelestate.common.dto.PageResponse;
import com.cyrev.nitelestate.common.search.Paging;
import com.cyrev.nitelestate.security.CurrentUser;
import com.cyrev.nitelestate.sticker.dto.StickerRequestCreateRequest;
import com.cyrev.nitelestate.sticker.dto.StickerRequestResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sticker-requests")
@RequiredArgsConstructor
public class StickerRequestController {

    private final StickerRequestService stickerRequestService;
    private final CurrentUser currentUser;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('RESIDENT')")
    public StickerRequestResponse request(@Valid @RequestBody StickerRequestCreateRequest request) {
        return stickerRequestService.request(currentUser.residentId(), request.vehicleId());
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('RESIDENT')")
    public PageResponse<StickerRequestResponse> mine(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return stickerRequestService.search(currentUser.residentId(), Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CDA_ADMIN', 'TREASURER', 'FINANCIAL_SECRETARY', 'SECURITY')")
    public PageResponse<StickerRequestResponse> findAll(@RequestParam(required = false) Long residentId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return stickerRequestService.search(residentId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
