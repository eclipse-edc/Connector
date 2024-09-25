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

package org.eclipse.edc.iam.identitytrust.sts.spi.service;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsAccount;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Mediates access to, modification and authentication of {@link StsAccount}s.
 */
@ExtensionPoint
public interface StsAccountService {

    /**
     * Create the {@link  StsAccount}
     *
     * @param client The client
     * @return successful when the client is created, failure otherwise
     * @deprecated Use {@link StsAccountService#create(StsAccount, String)} instead
     */

    @Deprecated(since = "0.9.0")
    ServiceResult<StsAccount> create(StsAccount client);

    /**
     * Create the {@link  StsAccount}
     *
     * @param client The client
     * @return successful when the client is created, failure otherwise
     */

    ServiceResult<String> create(StsAccount client, @Nullable String clientSecret);

    /**
     * Returns an {@link StsAccount} by its id
     *
     * @param id id of the client
     * @return the client successful if found, failure otherwise
     */
    ServiceResult<StsAccount> findByClientId(String id);

    /**
     * Updates an existing {@link StsAccount}, overwriting all values with the given new object.
     *
     * @param client the new account
     * @return A successful result, or a failure indicating what went wrong.
     */
    ServiceResult<Void> update(StsAccount client);

    /**
     * Updates the client secret associated with this {@link StsAccount}. The old secret is removed from the {@link org.eclipse.edc.spi.security.Vault},
     * and the new secret is stored using the given alias. If the new secret is {@code null}, one is generated at random.
     *
     * @param id          the ID of the {@link StsAccount} to update
     * @param secretAlias The alias under which the new secret is stored in the {@link org.eclipse.edc.spi.security.Vault}
     * @param newSecret   The new client secret. If null, a new one is generated.
     * @return A successful result, or a failure indicating what went wrong.
     */
    ServiceResult<String> updateSecret(String id, String secretAlias, @Nullable String newSecret);

    /**
     * Updates the client secret associated with this {@link StsAccount}. The old secret is removed from the {@link org.eclipse.edc.spi.security.Vault},
     * and the new secret is stored using the given alias. A new secret is generated at random.
     *
     * @param id       the ID of the {@link StsAccount} to update
     * @param newAlias The alias under which the new secret is stored in the {@link org.eclipse.edc.spi.security.Vault}
     * @return A successful result, or a failure indicating what went wrong.
     */
    default ServiceResult<String> updateSecret(String id, String newAlias) {
        return updateSecret(id, newAlias, null);
    }

    /**
     * Deletes an {@link StsAccount} by its ID.
     *
     * @param id The (database) ID
     * @return A successful result, or a failure indicating what went wrong.
     */
    ServiceResult<Void> deleteById(String id);

    /**
     * Queries the storage for a collection of {@link StsAccount} objects that conform to the given {@link QuerySpec}
     *
     * @param querySpec the query
     * @return A collection of accounts, potentially empty. Never null.
     */
    Collection<StsAccount> queryAccounts(QuerySpec querySpec);

    /**
     * Authenticate an {@link StsAccount} given the input secret
     *
     * @param client The client to authenticate
     * @param secret The client secret in input to check
     * @return the successful if authenticated, failure otherwise
     */
    ServiceResult<StsAccount> authenticate(StsAccount client, String secret);

    ServiceResult<StsAccount> findById(String accountId);

}
