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

package org.eclipse.edc.protocol.dsp.transferprocess.api.controller;

import jakarta.json.JsonObject;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.transfer.spi.types.protocol.TransferRemoteMessage;
import org.eclipse.edc.service.spi.result.ServiceResult;
import org.eclipse.edc.spi.iam.ClaimToken;

import java.util.function.BiFunction;

/**
 * Defines an incoming DSP message as a remote message type.
 */
public class MessageSpec<M extends TransferRemoteMessage> {
    private JsonObject message;
    private String processId;
    private String token;
    private String expectedMessageType;
    private Class<M> messageClass;
    private BiFunction<M, ClaimToken, ServiceResult<TransferProcess>> serviceCall;

    public JsonObject getMessage() {
        return message;
    }

    public String getProcessId() {
        return processId;
    }

    public String getToken() {
        return token;
    }

    public String getExpectedMessageType() {
        return expectedMessageType;
    }

    public Class<M> getMessageClass() {
        return messageClass;
    }

    public BiFunction<M, ClaimToken, ServiceResult<TransferProcess>> getServiceCall() {
        return serviceCall;
    }

    private MessageSpec(Class<M> messageClass) {
        this.messageClass = messageClass;
    }

    public static class Builder<M extends TransferRemoteMessage> {
        private MessageSpec<M> spec;

        /**
         * Creates a new message spec for the given remote message type.
         */
        public static <M extends TransferRemoteMessage> Builder<M> newInstance(Class<M> messageClass) {
            return new Builder<>(messageClass);
        }

        /**
         * Json-Ld message.
         */
        public Builder<M> message(JsonObject message) {
            spec.message = message;
            return this;
        }

        /**
         * Process id.
         */
        public Builder<M> processId(String processId) {
            spec.processId = processId;
            return this;
        }

        /**
         * Security token.
         */
        public Builder<M> token(String token) {
            spec.token = token;
            return this;
        }

        /**
         * Expected Json-Ld message @type.
         */
        public Builder<M> expectedMessageType(String expectedMessageType) {
            spec.expectedMessageType = expectedMessageType;
            return this;
        }

        public Builder<M> serviceCall(BiFunction<M, ClaimToken, ServiceResult<TransferProcess>> serviceCall) {
            spec.serviceCall = serviceCall;
            return this;
        }

        public MessageSpec<M> build() {
            return spec;
        }

        private Builder(Class<M> messageClass) {
            spec = new MessageSpec<>(messageClass);
        }

    }
}
