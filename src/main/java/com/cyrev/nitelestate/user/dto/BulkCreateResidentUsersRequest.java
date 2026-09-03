package com.cyrev.nitelestate.user.dto;

import java.util.List;

/** Omitted/empty {@code residentIds} means "every resident who doesn't already have a login account". */
public record BulkCreateResidentUsersRequest(List<Long> residentIds) {
}
