package com.example.sonata_brain.access;

/** Areas of the API that permissions are granted on (REQ-011). */
public enum Resource {
    STUDENT,
    SESSION,
    PAYMENT,
    COURSE,
    FEE_STRUCTURE,
    DUES,
    USER;

    public String readAuthority() {
        return name() + "_READ";
    }

    public String writeAuthority() {
        return name() + "_WRITE";
    }
}
