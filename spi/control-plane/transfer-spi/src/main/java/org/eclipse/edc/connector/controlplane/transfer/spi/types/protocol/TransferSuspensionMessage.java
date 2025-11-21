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

package org.eclipse.edc.connector.controlplane.transfer.spi.types.protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link TransferSuspensionMessage} is sent by the provider or consumer at any point except a terminal state to
 * indicate the data transfer process should be suspended. If the termination was due to an
 * error, the sender may include error information.
 */
public class TransferSuspensionMessage extends TransferRemoteMessage {

    private String code;
    private List<Object> reason = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public List<Object> getReason() {
        return reason;
    }

    public static class Builder extends TransferRemoteMessage.Builder<TransferSuspensionMessage, Builder> {

        private Builder() {
            super(new TransferSuspensionMessage());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder code(String code) {
            message.code = code;
            return this;
        }

        public Builder reason(String reason) {
            message.reason.add(reason);
            return this;
        }

        public Builder reason(List<?> reason) {
            message.reason.addAll(reason);
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }
    }
}
