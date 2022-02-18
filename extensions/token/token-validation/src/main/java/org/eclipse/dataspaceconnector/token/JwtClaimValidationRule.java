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
 *       Amadeus - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.token;

import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.dataspaceconnector.spi.iam.ValidationRule;

public interface JwtClaimValidationRule extends ValidationRule<JWTClaimsSet> {
}
