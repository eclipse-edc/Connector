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

package org.eclipse.edc.connector.contract.spi.types.protocol;

import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.message.ProcessRemoteMessage;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static java.util.UUID.randomUUID;

/**
 * A remote message related to the ContractNegotiation context.
 *
 * The {@link #processId} represent the ID of the process on the recipient part.
 */
public abstract class ContractRemoteMessage implements ProcessRemoteMessage {

    protected String id;
    protected String processId;
    protected String consumerPid;
    protected String providerPid;
    protected String protocol = "unknown";
    protected String counterPartyAddress;

    @Override
    public @NotNull String getId() {
        return id;
    }

    public abstract Policy getPolicy();

    public String getConsumerPid() {
        return consumerPid;
    }

    public String getProviderPid() {
        return providerPid;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public void setProtocol(String protocol) {
        Objects.requireNonNull(protocol);
        this.protocol = protocol;
    }

    @Override
    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        Objects.requireNonNull(processId);
        this.processId = processId;
    }

    @Override
    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    /**
     * Verifies if the passed processId can be considered a valid one.
     *
     * @param processId the processId.
     * @return succees if it is valid, failure otherwise.
     */
    public Result<Void> isValidProcessId(String processId) {
        if (processId.equals(consumerPid) || processId.equals(providerPid)) {
            return Result.success();
        } else {
            return Result.failure("Expected processId to be one of [%s, %s] but it was %s".formatted(consumerPid, providerPid, processId));
        }
    }

    protected static class Builder<M extends ContractRemoteMessage, B extends Builder<M, B>> {
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

        public B processId(String processId) {
            this.message.processId = processId;
            return (B) this;
        }

        public B counterPartyAddress(String counterPartyAddress) {
            this.message.counterPartyAddress = counterPartyAddress;
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
