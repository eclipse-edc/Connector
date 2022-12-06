/*
 *  Copyright (c) 2020 - 2022 Fraunhofer Institute for Software and Systems Engineering
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

package org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Message;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.IdsMultipartParts;
import org.eclipse.edc.protocol.ids.api.multipart.dispatcher.sender.response.MultipartResponse;

import java.io.IOException;

/**
 * Utility class for processing IDS multipart responses.
 */
public class ResponseUtil {

    /**
     * Creates a {@link MultipartResponse} with payload type string from an {@link IdsMultipartParts}.
     *
     * @param parts contains input streams for response header and payload.
     * @param objectMapper object mapper for parsing the message header.
     * @return a {@link MultipartResponse} containing header and payload.
     * @throws IOException if reading header or payload fails.
     */
    public static MultipartResponse<String> parseMultipartStringResponse(IdsMultipartParts parts, ObjectMapper objectMapper) throws IOException {
        var header = objectMapper.readValue(parts.getHeader(), Message.class);

        String payload = null;
        if (parts.getPayload() != null) {
            payload = new String(parts.getPayload().readAllBytes());
        }

        return new MultipartResponse<>(header, payload);
    }

}
