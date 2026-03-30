/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.controlplane.contract.spi.negotiation.tasks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;

@JsonDeserialize(builder = SendTerminateNegotiation.Builder.class)
@JsonTypeName("task:SendTerminateNegotiation")
public class SendTerminateNegotiation extends ContractNegotiationTaskPayload {

    private SendTerminateNegotiation() {
    }

    @Override
    public String name() {
        return "negotiation.termination.send";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<SendTerminateNegotiation, Builder> {

        private Builder() {
            super(new SendTerminateNegotiation());
        }

        @JsonCreator
        public static SendTerminateNegotiation.Builder newInstance() {
            return new SendTerminateNegotiation.Builder();
        }

        @Override
        public SendTerminateNegotiation.Builder self() {
            return this;
        }
    }
}
