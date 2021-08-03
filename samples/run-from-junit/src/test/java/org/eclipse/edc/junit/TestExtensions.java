package org.eclipse.edc.junit;/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.util.Set;

/**
 *
 */
public class TestExtensions {

    public static ServiceExtension mockIamExtension(IdentityService identityService) {
        return new ServiceExtension() {
            @Override
            public Set<String> provides() {
                return Set.of("iam");
            }

            @Override
            public void initialize(ServiceExtensionContext context) {
                context.registerService(IdentityService.class, identityService);
            }
        };
    }

}
