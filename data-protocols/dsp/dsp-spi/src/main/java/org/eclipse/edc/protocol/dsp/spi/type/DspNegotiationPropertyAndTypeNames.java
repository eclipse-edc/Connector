/*
 *  Copyright (c) 2023 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.protocol.dsp.spi.type;

import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;

public interface DspNegotiationPropertyAndTypeNames {

    // types

    String DSPACE_TYPE_CONTRACT_NEGOTIATION = DSPACE_SCHEMA + "ContractNegotiation";
    String DSPACE_TYPE_CONTRACT_NEGOTIATION_ERROR = DSPACE_SCHEMA + "ContractNegotiationError";

    // messages

    String DSPACE_TYPE_CONTRACT_REQUEST_MESSAGE = DSPACE_SCHEMA + "ContractRequestMessage";
    String DSPACE_TYPE_CONTRACT_OFFER_MESSAGE = DSPACE_SCHEMA + "ContractOfferMessage";
    String DSPACE_TYPE_CONTRACT_NEGOTIATION_EVENT_MESSAGE = DSPACE_SCHEMA + "ContractNegotiationEventMessage";
    String DSPACE_TYPE_CONTRACT_AGREEMENT_MESSAGE = DSPACE_SCHEMA + "ContractAgreementMessage";
    String DSPACE_TYPE_CONTRACT_AGREEMENT_VERIFICATION_MESSAGE = DSPACE_SCHEMA + "ContractAgreementVerificationMessage";
    String DSPACE_TYPE_CONTRACT_NEGOTIATION_TERMINATION_MESSAGE = DSPACE_SCHEMA + "ContractNegotiationTerminationMessage";

    // properties

    String DSPACE_PROPERTY_EVENT_TYPE = DSPACE_SCHEMA + "eventType";
    String DSPACE_PROPERTY_AGREEMENT = DSPACE_SCHEMA + "agreement";
    String DSPACE_PROPERTY_OFFER = DSPACE_SCHEMA + "offer";
    String DSPACE_PROPERTY_TIMESTAMP = DSPACE_SCHEMA + "timestamp";
    String DSPACE_PROPERTY_CONSUMER_ID = DSPACE_SCHEMA + "consumerId";
    String DSPACE_PROPERTY_PROVIDER_ID = DSPACE_SCHEMA + "providerId";

    // event types

    String DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_ACCEPTED = DSPACE_SCHEMA + "ACCEPTED";
    String DSPACE_VALUE_NEGOTIATION_EVENT_TYPE_FINALIZED = DSPACE_SCHEMA + "FINALIZED";

    // negotiation states

    String DSPACE_VALUE_NEGOTIATION_STATE_REQUESTED = DSPACE_SCHEMA + "REQUESTED";
    String DSPACE_VALUE_NEGOTIATION_STATE_OFFERED = DSPACE_SCHEMA + "OFFERED";
    String DSPACE_VALUE_NEGOTIATION_STATE_ACCEPTED = DSPACE_SCHEMA + "ACCEPTED";
    String DSPACE_VALUE_NEGOTIATION_STATE_AGREED = DSPACE_SCHEMA + "AGREED";
    String DSPACE_VALUE_NEGOTIATION_STATE_VERIFIED = DSPACE_SCHEMA + "VERIFIED";
    String DSPACE_VALUE_NEGOTIATION_STATE_FINALIZED = DSPACE_SCHEMA + "FINALIZED";
    String DSPACE_VALUE_NEGOTIATION_STATE_TERMINATED = DSPACE_SCHEMA + "TERMINATED";

}
