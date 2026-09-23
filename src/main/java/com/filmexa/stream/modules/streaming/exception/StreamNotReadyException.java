package com.filmexa.stream.modules.streaming.exception;

public class StreamNotReadyException extends RuntimeException {

    private final int retryAfterSeconds;

    public StreamNotReadyException(String message) {
        this(message, 5);
    }

    public StreamNotReadyException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
