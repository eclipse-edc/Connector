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

package org.eclipse.edc.iam.oauth2.rule;

import org.eclipse.edc.iam.oauth2.Oauth2ServiceConfiguration;
import org.eclipse.edc.iam.oauth2.spi.Oauth2ValidationRulesRegistry;
import org.eclipse.edc.jwt.TokenValidationRulesRegistryImpl;

import java.time.Clock;

/**
 * Registry for Oauth2 validation rules.
 */
public class Oauth2ValidationRulesRegistryImpl extends TokenValidationRulesRegistryImpl implements Oauth2ValidationRulesRegistry {

    public Oauth2ValidationRulesRegistryImpl(Oauth2ServiceConfiguration configuration, Clock clock) {
        this.addRule(new Oauth2AudienceValidationRule(configuration.getEndpointAudience()));
        this.addRule(new Oauth2NotBeforeValidationRule(clock, configuration.getNotBeforeValidationLeeway()));
        this.addRule(new Oauth2ExpirationIssuedAtValidationRule(clock, configuration.getIssuedAtLeeway()));
    }
}
