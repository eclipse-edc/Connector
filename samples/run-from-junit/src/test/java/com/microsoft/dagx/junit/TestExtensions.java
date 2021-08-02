package com.microsoft.dagx.junit;/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

import com.microsoft.dagx.spi.iam.IdentityService;
import com.microsoft.dagx.spi.system.ServiceExtension;
import com.microsoft.dagx.spi.system.ServiceExtensionContext;

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
