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

@JsonDeserialize(builder = ResumeDataFlow.Builder.class)
@JsonTypeName("task:ResumeDataFlow")
public class ResumeDataFlow extends TransferProcessTaskPayload {

    private ResumeDataFlow() {
    }

    @Override
    public String name() {
        return "transfer.resume";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<ResumeDataFlow, Builder> {

        private Builder() {
            super(new ResumeDataFlow());
        }

        @JsonCreator
        public static ResumeDataFlow.Builder newInstance() {
            return new ResumeDataFlow.Builder();
        }

        @Override
        public ResumeDataFlow.Builder self() {
            return this;
        }
    }
}
