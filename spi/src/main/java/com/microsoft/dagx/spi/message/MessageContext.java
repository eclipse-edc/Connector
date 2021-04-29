package com.microsoft.dagx.spi.message;

/**
 * A context for sending remote messages.
 */
public interface MessageContext {

    /**
     * Returns the process id associated with the current request, or null if not applicable.
     */
    String getProcessId();

}
