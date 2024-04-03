/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.spi;

public interface VcConstants {

    String VC_PREFIX = "https://www.w3.org/2018/credentials#";
    String SCHEMA_ORG_NAMESPACE = "https://schema.org/";

    String IATP_CONTEXT_URL = "https://w3id.org/tractusx-trust/v0.8";
    String IATP_PREFIX = IATP_CONTEXT_URL + "/";
    String PRESENTATION_EXCHANGE_URL = "https://identity.foundation/presentation-exchange/submission/v1";
    String W3C_CREDENTIALS_URL = "https://www.w3.org/2018/credentials/v1";
    String VERIFIABLE_PRESENTATION_TYPE = "VerifiablePresentation";
    String JWS_2020_URL = "https://w3id.org/security/suites/jws-2020/v1";
    String DID_CONTEXT_URL = "https://www.w3.org/ns/did/v1";
    String PRESENTATION_SUBMISSION_URL = "https://identity.foundation/presentation-exchange/submission/v1/";
    String JWS_2020_SIGNATURE_SUITE = "JsonWebSignature2020";
    String ED25519_SIGNATURE_SUITE = "Ed25519Signature2020"; // not used right now
}
