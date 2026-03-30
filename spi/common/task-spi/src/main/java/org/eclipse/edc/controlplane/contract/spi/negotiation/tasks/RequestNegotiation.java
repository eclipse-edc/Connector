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

@JsonDeserialize(builder = RequestNegotiation.Builder.class)
@JsonTypeName("task:RequestNegotiation")
public class RequestNegotiation extends ContractNegotiationTaskPayload {

    private RequestNegotiation() {
    }

    @Override
    public String name() {
        return "negotiation.request.prepare";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<RequestNegotiation, Builder> {

        private Builder() {
            super(new RequestNegotiation());
        }

        @JsonCreator
        public static RequestNegotiation.Builder newInstance() {
            return new RequestNegotiation.Builder();
        }

        @Override
        public RequestNegotiation.Builder self() {
            return this;
        }
    }
}
