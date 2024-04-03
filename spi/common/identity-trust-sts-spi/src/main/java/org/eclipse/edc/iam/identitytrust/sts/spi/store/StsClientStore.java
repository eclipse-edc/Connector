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

package org.eclipse.edc.iam.identitytrust.sts.spi.store;


import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.result.StoreResult;

/**
 * Persists and retrieves {@link StsClient}s.
 */
@ExtensionPoint
public interface StsClientStore {

    /**
     * Stores the {@link  StsClient}
     *
     * @param client The client
     * @return successful when the client is stored, failure otherwise
     */
    StoreResult<StsClient> create(StsClient client);

    /**
     * Returns an {@link StsClient} by its clientId
     *
     * @param clientId clientId of the client
     * @return the client successful if found, failure otherwise
     */
    StoreResult<StsClient> findByClientId(String clientId);

}
