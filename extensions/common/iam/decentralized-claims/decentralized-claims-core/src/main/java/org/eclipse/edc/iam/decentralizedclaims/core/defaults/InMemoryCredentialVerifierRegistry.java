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

package org.eclipse.edc.iam.decentralizedclaims.core.defaults;

import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifier;
import org.eclipse.edc.iam.decentralizedclaims.spi.verification.CredentialVerifierRegistry;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryCredentialVerifierRegistry implements CredentialVerifierRegistry {

    private final List<CredentialVerifier> verifiers = new CopyOnWriteArrayList<>();

    @Override
    public void register(CredentialVerifier verifier) {
        verifiers.add(verifier);
    }

    @Override
    public List<CredentialVerifier> getVerifiers() {
        return List.copyOf(verifiers);
    }
}
