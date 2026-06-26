/*
 *  Copyright (c) 2026 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.iam.decentralizedclaims.sts.registry;

import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenService;
import org.eclipse.edc.iam.decentralizedclaims.spi.SecureTokenServiceRegistry;
import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;

/**
 * Provides a {@link SecureTokenServiceRegistry} and a dispatching {@link SecureTokenService} that resolves the concrete
 * implementation to use per participant context from the registry, based on the {@code edc.iam.sts} property.
 */
@Extension(SecureTokenServiceRegistryExtension.NAME)
public class SecureTokenServiceRegistryExtension implements ServiceExtension {

    public static final String NAME = "Secure Token Service Registry Extension";

    @Setting(key = "edc.iam.sts.default.type", description = "Default STS type to use when the 'edc.iam.sts' property is not set on the participant context", defaultValue = "oauth")
    private String defaultStsType;

    @Inject
    private ParticipantContextConfig participantContextConfig;

    private SecureTokenServiceRegistry registry;

    @Override
    public String name() {
        return NAME;
    }

    @Provider
    public SecureTokenServiceRegistry secureTokenServiceRegistry() {
        return registry();
    }

    @Provider
    public SecureTokenService secureTokenService() {
        return new DelegatingSecureTokenService(registry(), participantContextConfig, defaultStsType);
    }

    private SecureTokenServiceRegistry registry() {
        if (registry == null) {
            registry = new SecureTokenServiceRegistryImpl();
        }
        return registry;
    }
}
