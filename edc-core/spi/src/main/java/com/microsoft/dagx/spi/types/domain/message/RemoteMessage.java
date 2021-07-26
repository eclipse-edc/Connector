/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.spi.types.domain.message;

/**
 * A remote message that is to be sent to another system. Dispatchers are responsible for binding the remote message to the specific transport protocol specified by the message.
 */
public interface RemoteMessage {

    /**
     * Returns the transport protocol this message must be sent over.
     */
    String getProtocol();

}
