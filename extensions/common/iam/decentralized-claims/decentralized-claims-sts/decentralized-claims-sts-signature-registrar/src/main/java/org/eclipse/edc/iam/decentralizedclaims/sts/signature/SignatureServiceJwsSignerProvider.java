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

package org.eclipse.edc.iam.decentralizedclaims.sts.signature;

import com.nimbusds.jose.JWSSigner;
import org.eclipse.edc.jwt.spi.signer.JwsSignerProvider;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.SignatureService;

/**
 * A {@link JwsSignerProvider} that creates {@link SignatureServiceJwsSigner}s. The {@code privateKeyId} passed in is
 * interpreted as the name/identifier of the key the backing {@link SignatureService} should sign with.
 */
public class SignatureServiceJwsSignerProvider implements JwsSignerProvider {

    private final SignatureService signatureService;

    public SignatureServiceJwsSignerProvider(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @Override
    public Result<JWSSigner> createJwsSigner(String participantContextId, String privateKeyId) {
        return Result.success(new SignatureServiceJwsSigner(signatureService, privateKeyId));
    }
}
