/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.contract.definition.store.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDocument;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

@JsonTypeName("dataspaceconnector:contractddefinitiondocument")
public class ContractDefinitionDocument extends CosmosDocument<ContractDefinition> {

    @JsonCreator
    public ContractDefinitionDocument(@JsonProperty("wrappedInstance") ContractDefinition contractDefinition,
                                      @JsonProperty("partitionKey") String partitionKey) {
        super(contractDefinition, partitionKey);
    }


    @Override
    public String getId() {
        return getWrappedInstance().getId();
    }
}
