package com.example.sonata_brain.exception;

public class NotHavingEnoughPermission extends RuntimeException{
    public NotHavingEnoughPermission() {
        super("You are not having enough permissions, you need to contact the admin");
    }
}
