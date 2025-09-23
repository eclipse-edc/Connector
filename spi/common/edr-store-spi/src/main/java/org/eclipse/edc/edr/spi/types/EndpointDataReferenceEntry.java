/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.edr.spi.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.eclipse.edc.participantcontext.spi.types.AbstractParticipantResource;

import static java.util.Objects.requireNonNull;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Represents metadata associated with an EDR
 */
public class EndpointDataReferenceEntry extends AbstractParticipantResource {

    public static final String EDR_ENTRY_TYPE_TERM = "EndpointDataReferenceEntry";
    public static final String EDR_ENTRY_TYPE = EDC_NAMESPACE + EDR_ENTRY_TYPE_TERM;

    public static final String ASSET_ID = "assetId";
    public static final String EDR_ENTRY_ASSET_ID = EDC_NAMESPACE + ASSET_ID;
    public static final String AGREEMENT_ID = "agreementId";
    public static final String EDR_ENTRY_AGREEMENT_ID = EDC_NAMESPACE + AGREEMENT_ID;
    public static final String CONTRACT_NEGOTIATION_ID = "contractNegotiationId";
    public static final String EDR_ENTRY_CONTRACT_NEGOTIATION_ID = EDC_NAMESPACE + CONTRACT_NEGOTIATION_ID;

    public static final String TRANSFER_PROCESS_ID = "transferProcessId";
    public static final String EDR_ENTRY_TRANSFER_PROCESS_ID = EDC_NAMESPACE + TRANSFER_PROCESS_ID;
    public static final String PROVIDER_ID = "providerId";
    public static final String EDR_ENTRY_PROVIDER_ID = EDC_NAMESPACE + PROVIDER_ID;

    public static final String CREATED_AT = "createdAt";
    public static final String EDR_ENTRY_CREATED_AT = EDC_NAMESPACE + CREATED_AT;
    private String assetId;
    private String agreementId;
    private String transferProcessId;
    private String contractNegotiationId;
    private String providerId;

    private EndpointDataReferenceEntry() {
    }


    public String getAgreementId() {
        return agreementId;
    }

    public String getTransferProcessId() {
        return transferProcessId;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getContractNegotiationId() {
        return contractNegotiationId;
    }

    public String getAssetId() {
        return assetId;
    }

    public static class Builder extends AbstractParticipantResource.Builder<EndpointDataReferenceEntry, EndpointDataReferenceEntry.Builder> {

        private Builder() {
            super(new EndpointDataReferenceEntry());
        }

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetId(String assetId) {
            entity.assetId = assetId;
            return this;
        }

        public Builder agreementId(String agreementId) {
            entity.agreementId = agreementId;
            return this;
        }

        public Builder transferProcessId(String transferProcessId) {
            entity.transferProcessId = transferProcessId;
            return this;
        }

        public Builder providerId(String providerId) {
            entity.providerId = providerId;
            return this;
        }

        public Builder contractNegotiationId(String contractNegotiationId) {
            entity.contractNegotiationId = contractNegotiationId;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public EndpointDataReferenceEntry build() {
            super.build();
            requireNonNull(entity.assetId, ASSET_ID);
            requireNonNull(entity.agreementId, AGREEMENT_ID);
            requireNonNull(entity.transferProcessId, TRANSFER_PROCESS_ID);
            requireNonNull(entity.providerId, PROVIDER_ID);
            // The id is always equals to transfer process id
            entity.id = entity.transferProcessId;
            return entity;
        }
    }
}
