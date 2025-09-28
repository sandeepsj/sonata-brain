package com.example.sonata_brain.service.dues.commands;

public interface DuesTrackerCommand {
    void execute();
    String getCommandType();
}
