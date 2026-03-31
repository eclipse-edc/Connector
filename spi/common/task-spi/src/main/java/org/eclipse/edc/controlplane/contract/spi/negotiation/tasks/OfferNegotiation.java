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

@JsonDeserialize(builder = OfferNegotiation.Builder.class)
@JsonTypeName("task:OfferNegotiation")
public class OfferNegotiation extends ContractNegotiationTaskPayload {

    private OfferNegotiation() {
    }

    @Override
    public String name() {
        return "negotiation.offer.prepare";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<OfferNegotiation, Builder> {

        private Builder() {
            super(new OfferNegotiation());
        }

        @JsonCreator
        public static OfferNegotiation.Builder newInstance() {
            return new OfferNegotiation.Builder();
        }

        @Override
        public OfferNegotiation.Builder self() {
            return this;
        }
    }
}
