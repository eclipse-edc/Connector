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

@JsonDeserialize(builder = SendOffer.Builder.class)
@JsonTypeName("task:SendOffer")
public class SendOffer extends ContractNegotiationTaskPayload {

    private SendOffer() {
    }

    @Override
    public String name() {
        return "negotiation.offer.send";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<SendOffer, Builder> {

        private Builder() {
            super(new SendOffer());
        }

        @JsonCreator
        public static SendOffer.Builder newInstance() {
            return new SendOffer.Builder();
        }

        @Override
        public SendOffer.Builder self() {
            return this;
        }
    }
}
