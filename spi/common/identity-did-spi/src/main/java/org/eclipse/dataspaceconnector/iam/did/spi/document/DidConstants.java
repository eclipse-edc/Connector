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

package org.eclipse.dataspaceconnector.iam.did.spi.document;

import org.eclipse.dataspaceconnector.spi.EdcSetting;

import java.util.Arrays;
import java.util.List;

public interface DidConstants {
    String ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019 = "EcdsaSecp256k1VerificationKey2019";
    String JSON_WEB_KEY_2020 = "JsonWebKey2020";
    String RSA_VERIFICATION_KEY_2018 = "RsaVerificationKey2018";
    String ED_25519_VERIFICATION_KEY_2018 = "Ed25519VerificationKey2018";
    List<String> ALLOWED_VERIFICATION_TYPES = Arrays.asList(DidConstants.ECDSA_SECP_256_K_1_VERIFICATION_KEY_2019,
            DidConstants.JSON_WEB_KEY_2020,
            DidConstants.RSA_VERIFICATION_KEY_2018,
            DidConstants.ED_25519_VERIFICATION_KEY_2018);
    @EdcSetting
    String DID_URL_SETTING = "edc.identity.did.url";

}
