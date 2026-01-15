/*
 *  Copyright (c) 2023 Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.type;

public interface DspNegotiationPropertyAndTypeNames {

    String DSPACE_TYPE_CONTRACT_NEGOTIATION_TERM = "ContractNegotiation";
    String DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR_TERM = "ContractNegotiationError";

    String DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE_TERM = "ContractRequestMessage";
    String DSPACE_TYPE_CONTRACT_OFFER_MESSAGE_TERM = "ContractOfferMessage";
    String DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE_TERM = "ContractNegotiationEventMessage";
    String DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE_TERM = "ContractAgreementMessage";
    String DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE_TERM = "ContractAgreementVerificationMessage";
    String DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE_TERM = "ContractNegotiationTerminationMessage";

    String DSPACE_PROPERTY_EVENT_TYPE_TERM = "eventType";
    String DSPACE_PROPERTY_AGREEMENT_TERM = "agreement";
    String DSPACE_PROPERTY_OFFER_TERM = "offer";
    String DSPACE_PROPERTY_TIMESTAMP_TERM = "timestamp";
    String DSPACE_PROPERTY_CONSUMER_ID_TERM = "consumerId";
    String DSPACE_PROPERTY_PROVIDER_ID_TERM = "providerId";

    String DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED_TERM = "ACCEPTED";
    String DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_FINALIZED_TERM = "FINALIZED";

    String DSPACE_VALUE_NEGOTIATION_STATE_REQUESTED_TERM = "REQUESTED";
    String DSPACE_VALUE_NEGOTIATION_STATE_OFFERED_TERM = "OFFERED";
    String DSPACE_VALUE_NEGOTIATION_STATE_ACCEPTED_TERM = "ACCEPTED";
    String DSPACE_VALUE_NEGOTIATION_STATE_AGREED_TERM = "AGREED";
    String DSPACE_VALUE_NEGOTIATION_STATE_VERIFIED_TERM = "VERIFIED";
    String DSPACE_VALUE_NEGOTIATION_STATE_FINALIZED_TERM = "FINALIZED";
    String DSPACE_VALUE_NEGOTIATION_STATE_TERMINATED_TERM = "TERMINATED";

}
