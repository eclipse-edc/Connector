/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
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

package org.eclipse.dataspaceconnector.transfer.store.cosmos.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.azure.cosmos.Lease;
import org.eclipse.dataspaceconnector.azure.cosmos.LeaseableCosmosDocument;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;

/**
 * This is a wrapper solely used to store {@link TransferProcess} objects in an Azure CosmosDB. Some features or requirements
 * of CosmosDB don't fit into a {@link TransferProcess}'s data model, such as a "partition key" or a "lease".
 * The former is required by CosmosDB to achieve a better distribution of read/write load and the latter was implemented
 * to guard against multiple connectors processing the same TransferProcess.
 *
 * @see Lease
 * @see TransferProcess
 */
@JsonTypeName("dataspaceconnector:transferprocessdocument")
public class TransferProcessDocument extends LeaseableCosmosDocument<TransferProcess> {


    @JsonCreator
    public TransferProcessDocument(@JsonProperty("wrappedInstance") TransferProcess wrappedInstance, @JsonProperty("partitionKey") String partitionKey) {
        super(wrappedInstance, partitionKey);
    }

    @Override
    public String getId() {
        return getWrappedInstance().getId();
    }

}
