package com.nitelestate.qrtag;

/** What a pre-generated QR tag can be linked to. The prefix starts its printed code (V-000001), the scan path is the gate URL segment. */
public enum QrTagKind {
    VEHICLE("V", "vehicle", "Vehicle"),
    PROPERTY("P", "property", "Property");

    private final String prefix;
    private final String scanPath;
    private final String label;

    QrTagKind(String prefix, String scanPath, String label) {
        this.prefix = prefix;
        this.scanPath = scanPath;
        this.label = label;
    }

    public String prefix() {
        return prefix;
    }

    public String scanPath() {
        return scanPath;
    }

    public String label() {
        return label;
    }

    public String code(int serial) {
        return prefix + "-" + String.format("%06d", serial);
    }
}
