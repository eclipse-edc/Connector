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
 *       sovity GmbH - added issuedAt leeway
 *
 */

package org.eclipse.edc.iam.oauth2.rule;

import org.eclipse.edc.iam.oauth2.Oauth2ServiceConfiguration;
import org.eclipse.edc.iam.oauth2.spi.Oauth2ValidationRulesRegistry;
import org.eclipse.edc.jwt.TokenValidationRulesRegistryImpl;
import org.eclipse.edc.jwt.rules.AudienceValidationRule;
import org.eclipse.edc.jwt.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.jwt.rules.NotBeforeValidationRule;

import java.time.Clock;

/**
 * Registry for Oauth2 validation rules.
 *
 * @deprecated This specialization will be removed issue https://github.com/eclipse-edc/Connector/issues/3744 and get replaced by a security context
 */
@Deprecated
public class Oauth2ValidationRulesRegistryImpl extends TokenValidationRulesRegistryImpl implements Oauth2ValidationRulesRegistry {

    public Oauth2ValidationRulesRegistryImpl(Oauth2ServiceConfiguration configuration, Clock clock) {
        this.addRule(new AudienceValidationRule(configuration.getEndpointAudience()));
        this.addRule(new NotBeforeValidationRule(clock, configuration.getNotBeforeValidationLeeway()));
        this.addRule(new ExpirationIssuedAtValidationRule(clock, configuration.getIssuedAtLeeway()));
    }
}
