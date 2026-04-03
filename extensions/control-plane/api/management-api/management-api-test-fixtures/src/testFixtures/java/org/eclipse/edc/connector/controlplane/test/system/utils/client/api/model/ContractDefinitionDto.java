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

package org.eclipse.edc.connector.controlplane.test.system.utils.client.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * DTO representation of a Contract Definition.
 */
public final class ContractDefinitionDto extends Typed {
    @JsonProperty("@id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String id;
    private final String accessPolicyId;
    private final String contractPolicyId;
    private final List<CriterionDto> assetsSelector;
    private final Map<String, Object> privateProperties;

    public ContractDefinitionDto(String id,
                                 String accessPolicyId, String contractPolicyId, List<CriterionDto> assetsSelector,
                                 Map<String, Object> privateProperties) {
        super("ContractDefinition");
        this.id = id;
        this.accessPolicyId = accessPolicyId;
        this.contractPolicyId = contractPolicyId;
        this.assetsSelector = assetsSelector;
        this.privateProperties = privateProperties;
    }

    public ContractDefinitionDto(String accessPolicyId, String contractPolicyId, List<CriterionDto> assetsSelector) {
        this(null, accessPolicyId, contractPolicyId, assetsSelector, Map.of());
    }

    public String getId() {
        return id;
    }

    public String getAccessPolicyId() {
        return accessPolicyId;
    }

    public String getContractPolicyId() {
        return contractPolicyId;
    }

    public List<CriterionDto> getAssetsSelector() {
        return assetsSelector;
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }
}
