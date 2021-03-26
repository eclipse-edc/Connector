package com.microsoft.dagx.spi.transfer.response;

/**
 * An operation response status.
 */
public enum ResponseStatus {
    /**
     * The operation completed successfully.
     */
    OK,

    /**
     * The operation errored and should be retried.
     */
    ERROR_RETRY,

    /**
     * The operation errored and should not be retried.
     */
    FATAL_ERROR
}
