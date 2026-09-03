package com.cyrev.nitelestate.gate.dto;

import com.cyrev.nitelestate.gate.Gate;
import com.cyrev.nitelestate.gate.GateStatus;
import com.cyrev.nitelestate.gate.GateType;

public record GateResponse(Long id, String name, String code, String location, GateType type, GateStatus status) {
    public static GateResponse from(Gate g) {
        return new GateResponse(g.getId(), g.getName(), g.getCode(), g.getLocation(), g.getType(), g.getStatus());
    }
}
