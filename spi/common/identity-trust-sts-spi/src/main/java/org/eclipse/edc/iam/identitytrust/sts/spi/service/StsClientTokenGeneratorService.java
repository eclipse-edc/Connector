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

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClientTokenAdditionalParams;
import org.eclipse.edc.runtime.metamodel.annotation.ExtensionPoint;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.ServiceResult;

/**
 * Self-Issued ID Token minting interface.
 */
@ExtensionPoint
public interface StsClientTokenGeneratorService {

    /**
     * Mint a Self-Issued ID Token for the input {@link StsClient} with additional parameters.
     *
     * @param client           The {@link StsClient}
     * @param additionalParams The {@link StsClientTokenAdditionalParams}
     * @return The issued token if successful, failure otherwise
     */
    ServiceResult<TokenRepresentation> tokenFor(StsClient client, StsClientTokenAdditionalParams additionalParams);

}
