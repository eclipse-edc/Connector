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

package org.eclipse.dataspaceconnector.contract.negotiation.store.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.azure.cosmos.LeaseableCosmosDocument;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractNegotiation;

@JsonTypeName("dataspaceconnector:contractdnegotiationdocument")
public class ContractNegotiationDocument extends LeaseableCosmosDocument<ContractNegotiation> {

    @JsonCreator
    public ContractNegotiationDocument(@JsonProperty("wrappedInstance") ContractNegotiation contractNegotiation, @JsonProperty("partitionKey") String partitionKey) {
        //todo: lets think about whether this a good partition key
        super(contractNegotiation, partitionKey);
    }

    @Override
    public String getId() {
        return getWrappedInstance().getId();
    }


}
