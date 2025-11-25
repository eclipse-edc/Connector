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

package org.eclipse.edc.connector.core;

import org.eclipse.edc.keys.KeyParserRegistryImpl;
import org.eclipse.edc.keys.VaultCertificateResolver;
import org.eclipse.edc.keys.VaultPrivateKeyResolver;
import org.eclipse.edc.keys.keyparsers.JwkParser;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.keys.spi.CertificateResolver;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.PrivateKeyResolver;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

import static org.eclipse.edc.connector.core.SecurityDefaultServicesExtension.NAME;

/**
 * This extension provides default/standard implementations for the {@link PrivateKeyResolver} and the {@link CertificateResolver}
 * Those provider methods CANNOT be implemented in {@link CoreDefaultServicesExtension}, because that could potentially cause
 * a conflict with injecting/providing the {@link Vault}
 */
@Extension(value = NAME)
public class SecurityDefaultServicesExtension implements ServiceExtension {
    public static final String NAME = "Security Default Services Extension";

    private KeyParserRegistry keyParserRegistry;

    @Inject
    private Vault vault;

    @Inject
    private TypeManager typeManager;

    private PrivateKeyResolver privateKeyResolver;

    @Provider(isDefault = true)
    public PrivateKeyResolver privateKeyResolver(ServiceExtensionContext context) {
        if (privateKeyResolver == null) {
            privateKeyResolver = new VaultPrivateKeyResolver(keyParserRegistry(context), vault, context.getMonitor().withPrefix("PrivateKeyResolution"), context.getConfig());
        }
        return privateKeyResolver;
    }

    @Provider(isDefault = true)
    public CertificateResolver certificateResolver() {
        return new VaultCertificateResolver(vault);
    }

    @Provider
    public KeyParserRegistry keyParserRegistry(ServiceExtensionContext context) {
        if (keyParserRegistry == null) {
            keyParserRegistry = new KeyParserRegistryImpl();
            //todo: register  Pkcs12Parser
            var monitor = context.getMonitor().withPrefix("PrivateKeyResolution");
            keyParserRegistry.register(new JwkParser(typeManager.getMapper(), monitor));
            keyParserRegistry.register(new PemParser(monitor));
        }
        return keyParserRegistry;
    }

}
