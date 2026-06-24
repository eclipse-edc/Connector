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

@JsonDeserialize(builder = SendTransferRequest.Builder.class)
@JsonTypeName("task:SendTransferRequest")
public class SendTransferRequest extends TransferProcessTaskPayload {

    private SendTransferRequest() {
    }

    @Override
    public String name() {
        return "transfer.request.send";
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder extends ProcessTaskPayload.Builder<SendTransferRequest, Builder> {

        private Builder() {
            super(new SendTransferRequest());
        }

        @JsonCreator
        public static SendTransferRequest.Builder newInstance() {
            return new SendTransferRequest.Builder();
        }

        @Override
        public SendTransferRequest.Builder self() {
            return this;
        }
    }
}
