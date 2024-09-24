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
import org.eclipse.edc.spi.result.ServiceResult;

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
     */

    ServiceResult<StsAccount> create(StsAccount client);

    /**
     * Returns an {@link StsAccount} by its id
     *
     * @param id id of the client
     * @return the client successful if found, failure otherwise
     */
    ServiceResult<StsAccount> findByClientId(String id);

    /**
     * Authenticate an {@link StsAccount} given the input secret
     *
     * @param client The client to authenticate
     * @param secret The client secret in input to check
     * @return the successful if authenticated, failure otherwise
     */
    ServiceResult<StsAccount> authenticate(StsAccount client, String secret);

}
