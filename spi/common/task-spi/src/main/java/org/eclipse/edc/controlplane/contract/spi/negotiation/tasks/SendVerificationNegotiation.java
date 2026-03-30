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

@JsonDeserialize(builder = SendVerificationNegotiation.Builder.class)
@JsonTypeName("task:SendVerificationNegotiation")
public class SendVerificationNegotiation extends ContractNegotiationTaskPayload {

    private SendVerificationNegotiation() {
    }

    @Override
    public String name() {
        return "negotiation.verification.send";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<SendVerificationNegotiation, Builder> {

        private Builder() {
            super(new SendVerificationNegotiation());
        }

        @JsonCreator
        public static SendVerificationNegotiation.Builder newInstance() {
            return new SendVerificationNegotiation.Builder();
        }

        @Override
        public SendVerificationNegotiation.Builder self() {
            return this;
        }
    }
}
