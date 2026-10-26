package com.nitelestate.resident.dto;

import com.nitelestate.property.dto.PropertyResponse;

public record ResidentWithPropertyResponse(ResidentResponse resident, PropertyResponse property) {
}
