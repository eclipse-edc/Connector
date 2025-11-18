/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.boot;

import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.DefaultParticipantVaultImpl;
import org.eclipse.edc.spi.security.ParticipantVault;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;


@Extension(value = VaultCompatibilityExtension.NAME)
public class VaultCompatibilityExtension implements ServiceExtension {

    public static final String NAME = "Vault Compatibility Services";

    @Inject
    private Vault vault;

    @Override
    public String name() {
        return NAME;
    }

    @Provider(isDefault = true)
    public ParticipantVault createDefaultParticipantVault(ServiceExtensionContext context) {
        return new DefaultParticipantVaultImpl(vault, context.getMonitor().withPrefix("VaultCompatibility"));
    }
}
