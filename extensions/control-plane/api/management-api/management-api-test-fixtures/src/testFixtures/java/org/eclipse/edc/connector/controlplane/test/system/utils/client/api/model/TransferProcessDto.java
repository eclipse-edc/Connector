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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO representation of a Transfer Process.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class TransferProcessDto extends Typed {

    private String state;
    private String correlationId;
    private String errorDetail;
    private Map<String, Object> privateProperties;
    @JsonProperty("@id")
    private String id;

    public TransferProcessDto() {
        super("TransferProcess");
    }

    public TransferProcessDto(String id,
                              String state,
                              String correlationId,
                              String errorDetail,
                              Map<String, Object> privateProperties) {
        super("TransferProcess");
        this.id = id;
        this.state = state;
        this.correlationId = correlationId;
        this.errorDetail = errorDetail;
        this.privateProperties = privateProperties;
    }

    @JsonProperty("@id")
    public String getId() {
        return id;
    }

    public String getState() {
        return state;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    public Map<String, Object> getPrivateProperties() {
        return privateProperties;
    }

}
