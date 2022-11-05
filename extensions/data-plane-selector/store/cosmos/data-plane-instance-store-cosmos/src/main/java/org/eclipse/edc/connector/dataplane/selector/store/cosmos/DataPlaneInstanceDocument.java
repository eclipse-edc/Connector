/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.selector.store.cosmos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.edc.azure.cosmos.CosmosDocument;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;

@JsonTypeName("dataspaceconnector:dataplanedocument")
public class DataPlaneInstanceDocument extends CosmosDocument<DataPlaneInstance> {

    @JsonCreator
    public DataPlaneInstanceDocument(@JsonProperty("wrappedInstance") DataPlaneInstance dataPlaneInstance,
                                     @JsonProperty("partitionKey") String partitionKey) {
        super(dataPlaneInstance, partitionKey);
    }

    @Override
    public String getId() {
        return getWrappedInstance().getId();
    }
}
