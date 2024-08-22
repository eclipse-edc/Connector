/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.iam.verifiablecredentials;

import org.eclipse.edc.iam.verifiablecredentials.revocation.RevocationServiceRegistryImpl;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.RevocationServiceRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.iam.verifiablecredentials.RevocationServiceRegistryExtension.NAME;

@Extension(value = NAME)
public class RevocationServiceRegistryExtension implements ServiceExtension {
    public static final String NAME = "Revocation Service Extension";

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public RevocationServiceRegistry createRevocationListService(ServiceExtensionContext context) {
        return new RevocationServiceRegistryImpl(context.getMonitor());
    }
}
