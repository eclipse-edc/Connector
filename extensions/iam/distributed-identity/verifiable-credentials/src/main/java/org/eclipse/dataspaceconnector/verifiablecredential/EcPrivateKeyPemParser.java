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
package org.eclipse.dataspaceconnector.verifiablecredential;


import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import org.eclipse.dataspaceconnector.iam.did.spi.CryptoException;
import org.eclipse.dataspaceconnector.spi.security.KeyParser;

public class EcPrivateKeyPemParser implements KeyParser<ECKey> {

    @Override
    public boolean canParse(Class<?> keyClass) {
        return keyClass.equals(ECKey.class);
    }

    @Override
    public ECKey parse(String encodedContent) {
        try {
            return (ECKey) ECKey.parseFromPEMEncodedObjects(encodedContent);
        } catch (JOSEException e) {
            throw new CryptoException(e);
        }
    }
}
