/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.token.validation;

import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2ValidationRulesRegistry;
import org.eclipse.dataspaceconnector.ids.token.validation.rule.IdsValidationRule;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.EdcSetting;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Extension;
import org.eclipse.dataspaceconnector.runtime.metamodel.annotation.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * ServiceExtension providing extended IDS token validation
 */
@Extension(value = IdsTokenValidationServiceExtension.NAME)
public final class IdsTokenValidationServiceExtension implements ServiceExtension {

    @EdcSetting
    public static final String EDC_IDS_VALIDATION_REFERRINGCONNECTOR = "edc.ids.validation.referringconnector";
    public static final String NAME = "IDS Token Validation";

    @Inject
    private Oauth2ValidationRulesRegistry oauth2ValidationRulesRegistry;

    @Override
    public String name() {
        return NAME;
    }


    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        var validateReferring = serviceExtensionContext.getSetting(EDC_IDS_VALIDATION_REFERRINGCONNECTOR, false);
        oauth2ValidationRulesRegistry.addRule(new IdsValidationRule(validateReferring));
    }
}
