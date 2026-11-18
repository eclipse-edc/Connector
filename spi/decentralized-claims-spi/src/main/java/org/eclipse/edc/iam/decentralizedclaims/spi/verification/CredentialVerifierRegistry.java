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

package org.eclipse.edc.iam.decentralizedclaims.spi.verification;

import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;

import java.util.List;

/**
 * Holds the {@link CredentialVerifier}s that verify the presentations and credentials of a counter-party, one for each
 * supported format. A presentation in a format no registered verifier can handle is rejected.
 * <p>
 * The core registers the verifiers of the supported formats (JWT VCDM 1.1 and VCDM 2.0 JOSE); extensions can register
 * the verifiers of additional formats.
 */
@ExtensionPoint
public interface CredentialVerifierRegistry {

    /**
     * Registers a verifier. Verifiers are tried in registration order, the first one that can handle the input verifies it.
     *
     * @param verifier the verifier
     */
    void register(CredentialVerifier verifier);

    /**
     * Returns the registered verifiers, in registration order.
     *
     * @return the verifiers, never null
     */
    List<CredentialVerifier> getVerifiers();
}
