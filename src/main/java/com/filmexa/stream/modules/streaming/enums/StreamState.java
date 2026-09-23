package com.filmexa.stream.modules.streaming.enums;

/**
 * What a play request can tell the frontend.
 */
public enum StreamState {

    /**
     * The download has been started (or was already running) but there is not yet enough
     * on disk to play. The client should poll the same endpoint again.
     */
    PREPARING,

    /** Enough of the movie is downloaded and probed; the manifest is ready to open. */
    READY
}
