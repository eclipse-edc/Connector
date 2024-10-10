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

import java.util.List;

public abstract class ErrorMessage {

    protected String processId;
    protected String consumerPid;
    protected String providerPid;
    protected String code;
    protected List<String> messages;

    public abstract static class Builder<E extends ErrorMessage, B extends Builder<E, B>> {
        protected final E error;

        protected Builder(E error) {
            this.error = error;
        }

        public B processId(String processId) {
            this.error.processId = processId;

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

        protected abstract B self();

    }
}
