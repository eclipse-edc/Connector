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

package org.eclipse.dataspaceconnector.cosmos.policy.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.eclipse.dataspaceconnector.azure.cosmos.CosmosDocument;
import org.eclipse.dataspaceconnector.policy.model.Policy;

@JsonTypeName("dataspaceconnector:policydocument")
public class PolicyDocument extends CosmosDocument<Policy> {

    @JsonCreator
    public PolicyDocument(@JsonProperty("wrappedInstance") Policy policy,
                          @JsonProperty("partitionKey") String partitionKey) {
        super(policy, partitionKey);
    }


    @Override
    public String getId() {
        return getWrappedInstance().getUid();
    }
}
