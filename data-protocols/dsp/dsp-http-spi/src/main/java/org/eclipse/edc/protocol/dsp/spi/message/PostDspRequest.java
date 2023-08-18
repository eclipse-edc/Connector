/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.message;

import jakarta.json.JsonObject;
import org.eclipse.edc.spi.types.domain.message.RemoteMessage;

/**
 * Defines an incoming DSP message as a remote message type.
 */
public class PostDspRequest<I extends RemoteMessage, R> extends DspRequest<I, R> {

    private JsonObject message;
    private String processId;
    private String expectedMessageType;

    private PostDspRequest(Class<I> messageClass, Class<R> resultClass) {
        super(messageClass, resultClass);
    }

    public JsonObject getMessage() {
        return message;
    }

    public String getProcessId() {
        return processId;
    }

    public String getExpectedMessageType() {
        return expectedMessageType;
    }

    public static class Builder<I extends RemoteMessage, R> extends DspRequest.Builder<I, R, PostDspRequest<I, R>, Builder<I, R>> {

        public static <I extends RemoteMessage, R> Builder<I, R> newInstance(Class<I> inputClass, Class<R> resultClass) {
            return new Builder<>(inputClass, resultClass);
        }

        private Builder(Class<I> inputClass, Class<R> resultClass) {
            super(new PostDspRequest<>(inputClass, resultClass));
        }

        public Builder<I, R> message(JsonObject message) {
            super.message.message = message;
            return this;
        }

        public Builder<I, R> processId(String processId) {
            super.message.processId = processId;
            return this;
        }

        public Builder<I, R> expectedMessageType(String expectedMessageType) {
            super.message.expectedMessageType = expectedMessageType;
            return this;
        }

        @Override
        protected Builder<I, R> self() {
            return this;
        }
    }
}
