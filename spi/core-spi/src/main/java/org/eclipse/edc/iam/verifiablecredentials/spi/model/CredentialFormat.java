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
 *       Cofinity-X - updates for VCDM 2.0
 *
 */

package org.eclipse.edc.iam.verifiablecredentials.spi.model;

public enum CredentialFormat {

    /**
     * VerifiableCredentials DataModel 1.1, embedded proof (= LD proofs)
     */
    VC1_0_LD,
    /**
     * VerifiableCredentials DataModel 1.1, external proofs via JWS (= JWT representation)
     */
    VC1_0_JWT,
    /**
     * VerifiableCredentials DataModel 2.0, enveloping proof using JOSE (= JWT representation)
     */
    VC2_0_JOSE,
    /**
     * VerifiableCredentials DataModel 2.0, enveloping proof using SD-JWT
     * <p>
     * Currently not implemented in EDC.
     */
    VC2_0_SD_JWT,
    /**
     * VerifiableCredentials DataModel 2.0, enveloping proof using COSE (= CBOR representation)
     * <p>
     * Currently not implemented in EDC
     */
    VC2_0_COSE,
}
