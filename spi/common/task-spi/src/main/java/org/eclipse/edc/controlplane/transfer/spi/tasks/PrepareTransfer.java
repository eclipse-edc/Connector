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

@JsonDeserialize(builder = PrepareTransfer.Builder.class)
@JsonTypeName("task:PrepareTransfer")
public class PrepareTransfer extends TransferProcessTaskPayload {

    private PrepareTransfer() {
    }

    @Override
    public String name() {
        return "transfer.prepare";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<PrepareTransfer, Builder> {

        private Builder() {
            super(new PrepareTransfer());
        }

        @JsonCreator
        public static PrepareTransfer.Builder newInstance() {
            return new PrepareTransfer.Builder();
        }

        @Override
        public PrepareTransfer.Builder self() {
            return this;
        }
    }
}
