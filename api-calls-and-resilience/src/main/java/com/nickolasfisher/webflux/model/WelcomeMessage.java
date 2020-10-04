package com.nickolasfisher.webflux.model;

public class WelcomeMessage {
    private String message;

    public WelcomeMessage() {}

    public WelcomeMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
