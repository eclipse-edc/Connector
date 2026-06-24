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

@JsonDeserialize(builder = SendAgreement.Builder.class)
@JsonTypeName("task:SendAgreement")
public class SendAgreement extends ContractNegotiationTaskPayload {

    private SendAgreement() {
    }

    @Override
    public String name() {
        return "negotiation.agreement.send";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<SendAgreement, Builder> {

        private Builder() {
            super(new SendAgreement());
        }

        @JsonCreator
        public static SendAgreement.Builder newInstance() {
            return new SendAgreement.Builder();
        }

        @Override
        public SendAgreement.Builder self() {
            return this;
        }
    }
}
