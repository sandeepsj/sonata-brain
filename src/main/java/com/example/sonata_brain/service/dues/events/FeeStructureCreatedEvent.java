package com.example.sonata_brain.service.dues.events;

import com.example.sonata_brain.model.FeeStructure;

public class FeeStructureCreatedEvent extends DuesTrackerEvent {

    private final FeeStructure feeStructure;

    public FeeStructureCreatedEvent(Object source, FeeStructure feeStructure) {
        super(source);
        this.feeStructure = feeStructure;
    }

    public FeeStructure getFeeStructure() {
        return feeStructure;
    }

    @Override
    public String getEventType() {
        return "FEE_STRUCTURE_CREATED";
    }
}
