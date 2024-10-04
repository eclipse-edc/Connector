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

package org.eclipse.edc.spi.types.domain.message;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.util.UUID.randomUUID;

/**
 * A remote message that conveys state modifications of a process. These messages are idempotent.
 * <p>
 * The {@link #processId} represent the ID of the process on the recipient part.
 */
public abstract class ProcessRemoteMessage extends ProtocolRemoteMessage {

    protected String id;
    protected String processId;
    protected String consumerPid;
    protected String providerPid;
    protected String counterPartyAddress;
    protected String counterPartyId;

    /**
     * Returns the {@link Policy} associated with the process.
     *
     * @return the process {@link Policy}.
     */
    public abstract Policy getPolicy();

    /**
     * Returns the unique message id.
     *
     * @return the id;
     */
    public @NotNull String getId() {
        return id;
    }
    
    /**
     * Returns the process id for this instance, that could be consumerPid or providerPid.
     *
     * @return the processId.
     */
    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        Objects.requireNonNull(processId);
        this.processId = processId;
    }

    public String getConsumerPid() {
        return consumerPid;
    }

    public String getProviderPid() {
        return providerPid;
    }

    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    @Override
    public String getCounterPartyId() {
        return counterPartyId;
    }

    /**
     * Verifies if the passed processId can be considered a valid one.
     *
     * @param processId the processId.
     * @return success if it is valid, failure otherwise.
     */
    public Result<Void> isValidProcessId(String processId) {
        if (processId.equals(consumerPid) || processId.equals(providerPid)) {
            return Result.success();
        } else {
            return Result.failure("Expected processId to be one of [%s, %s] but it was %s".formatted(consumerPid, providerPid, processId));
        }
    }

    public static class Builder<M extends ProcessRemoteMessage, B extends Builder<M, B>> {
        protected final M message;

        protected Builder(M message) {
            this.message = message;
        }

        public B id(String id) {
            message.id = id;
            return (B) this;
        }

        public B consumerPid(String consumerPid) {
            message.consumerPid = consumerPid;
            return (B) this;
        }

        public B providerPid(String providerPid) {
            message.providerPid = providerPid;
            return (B) this;
        }

        public B protocol(String protocol) {
            this.message.protocol = protocol;
            return (B) this;
        }

        /**
         * Represent the processId of the recipient
         *
         * @param processId the processId.
         * @return the builder.
         */
        public B processId(String processId) {
            this.message.processId = processId;
            return (B) this;
        }

        public B counterPartyAddress(String counterPartyAddress) {
            this.message.counterPartyAddress = counterPartyAddress;
            return (B) this;
        }

        public B counterPartyId(String counterPartyId) {
            this.message.counterPartyId = counterPartyId;
            return (B) this;
        }

        public M build() {
            if (message.id == null) {
                message.id = randomUUID().toString();
            }
            return message;
        }
    }
}
