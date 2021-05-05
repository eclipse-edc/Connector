/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.ids.core.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 *
 */
public class MessageFunctions {

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

    private MessageFunctions() {
    }
}
