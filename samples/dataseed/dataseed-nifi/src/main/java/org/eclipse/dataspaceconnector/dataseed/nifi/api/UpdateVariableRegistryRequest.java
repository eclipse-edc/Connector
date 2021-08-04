/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors: 1
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.dataseed.nifi.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.stream.Collectors;

public class UpdateVariableRegistryRequest {
    @JsonProperty
    private final Revision processGroupRevision;
    @JsonProperty
    private final VariableRegistry variableRegistry;
    @JsonProperty
    private final boolean disconnectedNodeAcknowledged = true;

    public UpdateVariableRegistryRequest(String processGroupId, List<Variable> variables, Revision version) {
        processGroupRevision = version;
        variableRegistry = new VariableRegistry(processGroupId);
        variableRegistry.variables = variables.stream().map(VariableWrapper::new).collect(Collectors.toList());
    }

    public static class VariableWrapper {
        @JsonProperty
        private final Variable variable;
        @JsonProperty
        private final boolean canWrite = true;

        public VariableWrapper(Variable var) {
            variable = var;
        }

        public boolean isCanWrite() {
            return canWrite;
        }

        public Variable getVariable() {
            return variable;
        }
    }

    public static class VariableRegistry {
        @JsonProperty
        private final String processGroupId;
        @JsonProperty
        private List<VariableWrapper> variables;

        public VariableRegistry(String processGroupId) {
            this.processGroupId = processGroupId;
        }

        public String getProcessGroupId() {
            return processGroupId;
        }

        public List<VariableWrapper> getVariables() {
            return variables;
        }
    }
}






