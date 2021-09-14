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

package org.eclipse.dataspaceconnector.ids.core.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class MessageFunctions {

    private MessageFunctions() {
    }

    /**
     * Creates a request, writing the object as a JSON message body
     */
    public static RequestBody writeJson(Object body, ObjectMapper mapper) {
        try {
            return RequestBody.create(mapper.writeValueAsString(body), MediaType.get("application/json"));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
