/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.spi.types.domain.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract base class representing a protocol error message.
 * This class contains common properties and methods for error messages.
 */
public abstract class ErrorMessage {

    protected String processId;
    protected String consumerPid;
    protected String providerPid;
    protected String code;
    protected List<String> messages = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getConsumerPid() {
        return consumerPid;
    }

    public String getProviderPid() {
        return providerPid;
    }

    public String getProcessId() {
        return processId;
    }

    public abstract static class Builder<E extends ErrorMessage, B extends Builder<E, B>> {
        protected final E error;

        protected Builder(E error) {
            this.error = error;
        }

        public B processId(String processId) {
            this.error.processId = processId;

            return self();
        }

        public B consumerPid(String consumerPid) {
            this.error.consumerPid = consumerPid;
            return self();
        }

        public B providerPid(String providerPid) {
            this.error.providerPid = providerPid;
            return self();
        }

        public B code(String code) {
            this.error.code = code;
            return self();
        }

        public B messages(List<String> messages) {
            this.error.messages = messages;
            return self();
        }

        public E build() {
            Objects.requireNonNull(error.code);
            return error;
        }

        protected abstract B self();
    }
}
