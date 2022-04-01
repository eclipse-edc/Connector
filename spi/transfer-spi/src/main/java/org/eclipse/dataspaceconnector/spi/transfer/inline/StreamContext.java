/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.spi.transfer.inline;

/**
 * A context for creating stream sessions.
 */
public interface StreamContext {

    /**
     * Creates a stream session.
     *
     * @param uri        the stream endpoint uri
     * @param topicName  the topic name data is to be sent to
     * @param secretName the topic secret to be resolved by the context
     * @return the session
     */
    StreamSession createSession(String uri, String topicName, String secretName);

}
