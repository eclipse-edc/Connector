/*
 *  Copyright (c) 2022 Amadeus
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.transfer.dataplane.core.token;

import org.eclipse.dataspaceconnector.common.token.TokenValidationRulesRegistry;
import org.eclipse.dataspaceconnector.common.token.TokenValidationServiceImpl;
import org.eclipse.dataspaceconnector.spi.iam.PublicKeyResolver;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.token.DataPlaneTransferTokenValidator;

/**
 * Token validator used to verify tokens provided in input of Data Plane public API.
 */
public class DataPlaneTransferTokenValidatorImpl extends TokenValidationServiceImpl implements DataPlaneTransferTokenValidator {
    public DataPlaneTransferTokenValidatorImpl(PublicKeyResolver publicKeyResolver, TokenValidationRulesRegistry rulesRegistry) {
        super(publicKeyResolver, rulesRegistry);


    }
}
