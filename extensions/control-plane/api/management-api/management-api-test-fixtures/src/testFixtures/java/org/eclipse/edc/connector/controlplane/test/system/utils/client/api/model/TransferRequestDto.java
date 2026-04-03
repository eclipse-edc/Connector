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

/**
 * DTO representation of a Transfer Request.
 */
public final class TransferRequestDto extends Typed {
    @JsonProperty("@id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String id;
    private final String protocol;
    private final String counterPartyAddress;
    private final String transferType;
    private final String contractId;

    public TransferRequestDto(String id,
                              String protocol,
                              String counterPartyAddress,
                              String transferType,
                              String contractId) {
        super("TransferRequest");
        this.id = id;
        this.protocol = protocol;
        this.counterPartyAddress = counterPartyAddress;
        this.transferType = transferType;
        this.contractId = contractId;
    }

    public TransferRequestDto(String protocol, String counterPartyAddress, String transferType, String contractId) {
        this(null, protocol, counterPartyAddress, transferType, contractId);
    }


    @JsonProperty("@id")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getId() {
        return id;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getCounterPartyAddress() {
        return counterPartyAddress;
    }

    public String getTransferType() {
        return transferType;
    }

    public String getContractId() {
        return contractId;
    }

}
