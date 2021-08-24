/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.iam.did.spi.hub.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

/**
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonIgnoreProperties(value = {"@context"}, allowGetters = true)
public class HubMessage {

    @JsonProperty(value = "@context")
    public String getContext() {
        return HubMessageConstants.SCHEMA;
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(value = {"@context"}, allowGetters = true)
    public static class Builder {

        protected Builder() {
        }
    }
}
