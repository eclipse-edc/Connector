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

package org.eclipse.edc.iam.verifiablecredentials.spi;

public interface VcConstants {

    String VC_PREFIX = "https://www.w3.org/2018/credentials#";
    String SCHEMA_ORG_NAMESPACE = "https://schema.org/";

    String PRESENTATION_EXCHANGE_URL = "https://identity.foundation/presentation-exchange/submission/v1";
    String W3C_CREDENTIALS_URL = "https://www.w3.org/2018/credentials/v1";
    String VERIFIABLE_PRESENTATION_TYPE = "VerifiablePresentation";
    String JWS_2020_URL = "https://w3id.org/security/suites/jws-2020/v1";
    String DID_CONTEXT_URL = "https://www.w3.org/ns/did/v1";
    String PRESENTATION_SUBMISSION_URL = "https://identity.foundation/presentation-exchange/submission/v1/";
    String JWS_2020_SIGNATURE_SUITE = "JsonWebSignature2020";
    String ED25519_SIGNATURE_SUITE = "Ed25519Signature2020"; // not used right now
    String VC_PREFIX_V2 = "https://www.w3.org/ns/credentials/v2";
    String STATUSLIST_2021_URL = "https://w3id.org/vc/status-list/2021/v1";
    String STATUSLIST_2021_PREFIX = "https://w3id.org/vc/status-list#";

    String BITSTRING_STATUS_LIST_URL = VC_PREFIX_V2;
    String BITSTRING_STATUS_LIST_PREFIX = "https://www.w3.org/ns/credentials/status#";
}
