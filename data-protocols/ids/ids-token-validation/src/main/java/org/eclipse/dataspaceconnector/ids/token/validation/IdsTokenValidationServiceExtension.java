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
 *       Fraunhofer Institute for Software and Systems Engineering
 *
 */

package org.eclipse.dataspaceconnector.ids.token.validation;
import org.eclipse.dataspaceconnector.iam.oauth2.spi.Oauth2Service;
import org.eclipse.dataspaceconnector.ids.token.validation.rule.IdsValidationRule;
import org.eclipse.dataspaceconnector.spi.EdcSetting;
import org.eclipse.dataspaceconnector.spi.system.Inject;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

/**
 * ServiceExtension providing extended IDS token validation
 */
public final class IdsTokenValidationServiceExtension implements ServiceExtension {

    @EdcSetting
    public static final String EDC_IDS_VALIDATION_REFERRINGCONNECTOR = "edc.ids.validation.referringconnector";

    @Inject
    private Oauth2Service oauth2Service;

    @Override
    public String name() {
        return "IDS Token Validation";
    }


    @Override
    public void initialize(ServiceExtensionContext serviceExtensionContext) {
        var validateReferring = serviceExtensionContext.getSetting(EDC_IDS_VALIDATION_REFERRINGCONNECTOR, false);
        oauth2Service.addAdditionalValidationRule(new IdsValidationRule(validateReferring));
    }
}
