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

@JsonDeserialize(builder = SuspendDataFlow.Builder.class)
@JsonTypeName("task:SuspendDataFlow")
public class SuspendDataFlow extends TransferProcessTaskPayload {

    private SuspendDataFlow() {
    }

    @Override
    public String name() {
        return "transfer.suspend";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<SuspendDataFlow, Builder> {

        private Builder() {
            super(new SuspendDataFlow());
        }

        @JsonCreator
        public static SuspendDataFlow.Builder newInstance() {
            return new SuspendDataFlow.Builder();
        }

        @Override
        public SuspendDataFlow.Builder self() {
            return this;
        }
    }
}
