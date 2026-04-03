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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO representation of an Offer.
 */
public final class OfferDto extends PolicyDto {

    @JsonProperty("@id")
    private String id;

    public OfferDto() {
        super("Offer");
    }

    public OfferDto(String id) {
        super("Offer");
        this.id = id;
    }

    public OfferDto(String id, String assigner,
                    String target,
                    List<PermissionDto> permissions) {
        super("Offer", assigner, target, permissions);
        this.id = id;
    }


    @JsonProperty("@id")
    public String getId() {
        return id;
    }


}
