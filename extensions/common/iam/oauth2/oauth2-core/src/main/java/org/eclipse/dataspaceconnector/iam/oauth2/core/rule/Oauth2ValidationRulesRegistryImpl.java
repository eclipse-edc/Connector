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

package org.eclipse.dataspaceconnector.iam.oauth2.core.rule;

import org.eclipse.dataspaceconnector.common.token.TokenValidationRulesRegistryImpl;
import org.eclipse.dataspaceconnector.iam.oauth2.core.Oauth2Configuration;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2ValidationRulesRegistry;

import java.time.Clock;

/**
 * Registry for Oauth2 validation rules.
 */
public class Oauth2ValidationRulesRegistryImpl extends TokenValidationRulesRegistryImpl implements Oauth2ValidationRulesRegistry {

    public Oauth2ValidationRulesRegistryImpl(Oauth2Configuration configuration, Clock clock) {
        this.addRule(new Oauth2ValidationRule(configuration, clock));
    }
}
