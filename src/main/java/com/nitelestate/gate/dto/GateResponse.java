package com.nitelestate.gate.dto;

import com.nitelestate.gate.Gate;
import com.nitelestate.gate.GateStatus;
import com.nitelestate.gate.GateType;

public record GateResponse(Long id, String name, String code, String location, GateType type, GateStatus status) {
    public static GateResponse from(Gate g) {
        return new GateResponse(g.getId(), g.getName(), g.getCode(), g.getLocation(), g.getType(), g.getStatus());
    }
}
