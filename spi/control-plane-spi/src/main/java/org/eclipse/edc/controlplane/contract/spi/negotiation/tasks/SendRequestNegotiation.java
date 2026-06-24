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

@JsonDeserialize(builder = SendRequestNegotiation.Builder.class)
@JsonTypeName("task:SendRequestNegotiation")
public class SendRequestNegotiation extends ContractNegotiationTaskPayload {

    private SendRequestNegotiation() {
    }

    @Override
    public String name() {
        return "negotiation.request.send";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<SendRequestNegotiation, Builder> {

        private Builder() {
            super(new SendRequestNegotiation());
        }

        @JsonCreator
        public static SendRequestNegotiation.Builder newInstance() {
            return new SendRequestNegotiation.Builder();
        }

        @Override
        public SendRequestNegotiation.Builder self() {
            return this;
        }
    }
}
