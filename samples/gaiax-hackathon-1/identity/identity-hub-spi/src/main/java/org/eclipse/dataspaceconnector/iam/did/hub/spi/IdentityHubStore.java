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
package org.eclipse.dataspaceconnector.iam.did.hub.spi;

import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.Commit;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.CommitQuery;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.HubObject;
import org.eclipse.dataspaceconnector.iam.did.hub.spi.message.ObjectQuery;

import java.util.Collection;

/**
 * Provides persistence and query capabilities for hub data.
 */
public interface IdentityHubStore {

    /**
     * Persists a commit.
     */
    void write(Commit commit);

    /**
     * Executes a commit query.
     */
    Collection<Commit> query(CommitQuery query);

    /**
     * Executes an object query.
     */
    Collection<HubObject> query(ObjectQuery query);


}
