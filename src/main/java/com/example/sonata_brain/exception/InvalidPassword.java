package com.example.sonata_brain.exception;

public class InvalidPassword extends RuntimeException  {
    public InvalidPassword() {
        super("Invalid password");
    }
}
