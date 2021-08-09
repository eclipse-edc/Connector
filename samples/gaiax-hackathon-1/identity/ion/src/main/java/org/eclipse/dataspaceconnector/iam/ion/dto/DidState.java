/*
 *  Copyright (c) 2020, 2020-2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.iam.ion.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.dataspaceconnector.iam.ion.model.DidOperation;

import java.util.List;

@JsonDeserialize(builder = DidState.Builder.class)
public class DidState {
    private String shortForm;
    private String longForm;
    private List<DidOperation> operations;

    public String getShortForm() {
        return shortForm;
    }

    public String getLongForm() {
        return longForm;
    }

    public List<DidOperation> getOperations() {
        return operations;
    }


    @JsonPOJOBuilder
    public static final class Builder {
        private String shortForm;
        private String longForm;
        private List<DidOperation> operations;

        private Builder() {
        }

        @JsonCreator
        public static Builder create() {
            return new Builder();
        }

        public Builder shortForm(String shortForm) {
            this.shortForm = shortForm;
            return this;
        }

        public Builder longForm(String longForm) {
            this.longForm = longForm;
            return this;
        }

        public Builder operations(List<DidOperation> operations) {
            this.operations = operations;
            return this;
        }

        public DidState build() {
            DidState didState = new DidState();
            didState.operations = operations;
            didState.shortForm = shortForm;
            didState.longForm = longForm;
            return didState;
        }
    }
}
