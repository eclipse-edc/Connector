/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.spi.types.domain.message;

/**
 * A remote message that is to be sent to another system. Dispatchers are responsible for binding the remote message to the specific transport protocol specified by the message.
 */
public interface RemoteMessage {

    /**
     * Returns the transport protocol this message must be sent over.
     */
    String getProtocol();

    /**
     * Returns the recipient's callback address.
     */
    String getCounterPartyAddress();
    
    /**
     * Returns the recipient's id.
     */
    String getCounterPartyId();

}
