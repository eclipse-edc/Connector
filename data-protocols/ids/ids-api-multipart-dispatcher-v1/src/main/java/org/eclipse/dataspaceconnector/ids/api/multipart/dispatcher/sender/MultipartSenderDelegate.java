/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender;

import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.dataspaceconnector.ids.api.multipart.dispatcher.sender.response.MultipartResponse;
import org.eclipse.dataspaceconnector.spi.types.domain.message.RemoteMessage;

import java.io.InputStream;
import java.util.List;

/**
 * Handles message type specific operations when sending an IDS Multipart message.
 *
 * @param <M> the type of {@link RemoteMessage} this delegate can handle
 * @param <R> the expected response type
 */
public interface MultipartSenderDelegate<M extends RemoteMessage, R> {

    /**
     * Builds the IDS multipart header for the request.
     *
     * @param request the request.
     * @param token   the dynamic attribute token.
     * @return the message header.
     * @throws Exception if building the message header fails.
     */
    Message buildMessageHeader(M request, DynamicAttributeToken token) throws Exception;

    /**
     * Builds the IDS multipart payload for the request.
     *
     * @param request the request.
     * @return the message payload.
     * @throws Exception if building the message payload fails.
     */
    default String buildMessagePayload(M request) throws Exception {
        return null;
    }

    /**
     * Reads and parses the IDS multipart response.
     *
     * @param parts container object for response header and payload {@link InputStream}s.
     * @return an instance of the sub class's return type.
     * @throws Exception if parsing the response fails.
     */
    MultipartResponse<R> getResponseContent(IdsMultipartParts parts) throws Exception;

    /**
     * Return expected response type.
     *
     * @return the response type class.
     */
    List<Class<? extends Message>> getAllowedResponseTypes();

    Class<M> getMessageType();
}
