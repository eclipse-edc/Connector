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

package org.eclipse.edc.controlplane.transfer.spi.tasks;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.controlplane.tasks.ProcessTaskPayload;

@JsonDeserialize(builder = SignalDataflowStarted.Builder.class)
@JsonTypeName("task:SignalDataflowStarted")
public class SignalDataflowStarted extends TransferProcessTaskPayload {

    private SignalDataflowStarted() {
    }

    @Override
    public String name() {
        return "transfer.dataplane.started";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<SignalDataflowStarted, Builder> {

        private Builder() {
            super(new SignalDataflowStarted());
        }

        @JsonCreator
        public static SignalDataflowStarted.Builder newInstance() {
            return new SignalDataflowStarted.Builder();
        }

        @Override
        public SignalDataflowStarted.Builder self() {
            return this;
        }
    }
}
