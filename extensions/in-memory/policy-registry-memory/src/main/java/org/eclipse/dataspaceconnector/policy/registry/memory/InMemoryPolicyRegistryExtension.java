/*
 *  Copyright (c) 2020, 2021 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.policy.registry.memory;

import org.eclipse.dataspaceconnector.spi.policy.PolicyRegistry;
import org.eclipse.dataspaceconnector.spi.system.Provides;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;

@Provides(PolicyRegistry.class)
public class InMemoryPolicyRegistryExtension implements ServiceExtension {

    @Override
    public String name() {
        return "In-Memory Policy Registry";
    }


    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(PolicyRegistry.class, new InMemoryPolicyRegistry());
    }

}
