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

package org.eclipse.edc.signaling.oauth2.tokenexchange.logic;

import org.eclipse.edc.spi.result.Result;

/**
 * Supplies the workload credential (a Kubernetes projected ServiceAccount token, a SPIFFE JWT-SVID, ...) that is
 * presented to the token exchange broker as the {@code subject_token}.
 * <p>
 * Workload credentials are short-lived and rotate independently of the exchanged tokens, therefore implementations
 * must re-read the credential from its source on every invocation.
 */
@FunctionalInterface
public interface WorkloadCredentialSource {

    /**
     * Returns the current workload credential, or a failure if it cannot be obtained.
     */
    Result<String> get();
}
