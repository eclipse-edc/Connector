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
package org.eclipse.dataspaceconnector.iam.did.spi.hub;

import org.eclipse.dataspaceconnector.iam.did.spi.hub.message.Commit;

/**
 * Implements a Distributed Identity Foundation Identity Hub as described by {@code https://identity.foundation/identity-hub/spec/}.
 */
public interface IdentityHub {

    /**
     * Writes a commit JWE to the hub and returns either a success response encoded as a JWE or a plaintext error message.
     */
    String write(String commitJwe);

    /**
     * Workaround for Hackathon .NET client
     */
    @Deprecated
    void write(Commit commit);

    /**
     * Returns the commit history for an object as a JWE.
     */
    String queryCommits(String queryJwe);

    /**
     * Returns details of stored objects of a give type as a JWE.
     */
    String queryObjects(String queryJwe);
}
