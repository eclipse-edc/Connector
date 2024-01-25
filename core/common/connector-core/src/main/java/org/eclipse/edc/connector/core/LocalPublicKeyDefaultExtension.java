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

import org.eclipse.edc.connector.core.security.LocalPublicKeyServiceImpl;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.LocalPublicKeyService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.KeyParserRegistry;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.Map;

import static org.eclipse.edc.connector.core.SecurityDefaultServicesExtension.NAME;

@Extension(value = NAME)
@Provides(LocalPublicKeyService.class)
public class LocalPublicKeyDefaultExtension implements ServiceExtension {

    public static final String NAME = "Local Public Key Default Extension";

    public static final String EDC_PUBLIC_KEYS_PREFIX = "edc.iam.publickeys";

    public static final String CONFIG_ALIAS = EDC_PUBLIC_KEYS_PREFIX + ".<pkAlias>.";

    @Setting(context = CONFIG_ALIAS, value = "ID of the public key.", required = true)
    public static final String ID_SUFFIX = "id";

    @Setting(context = CONFIG_ALIAS, value = "Value of the public key. Multiple formats are supported, depending on the KeyParsers registered in the runtime", required = true)
    public static final String VALUE_SUFFIX = "value";

    @Inject
    public KeyParserRegistry keyParserRegistry;

    private Config keysConfiguration;

    private LocalPublicKeyServiceImpl localPublicKeyService;
    @Inject
    private Vault vault;

    @Provider(isDefault = true)
    public LocalPublicKeyService localPublicKeyService() {
        return localPublicKeyServiceImpl();
    }

    private LocalPublicKeyServiceImpl localPublicKeyServiceImpl() {
        if (localPublicKeyService == null) {
            localPublicKeyService = new LocalPublicKeyServiceImpl(vault, keyParserRegistry);
        }
        return localPublicKeyService;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        keysConfiguration = context.getConfig(EDC_PUBLIC_KEYS_PREFIX);
    }

    @Override
    public void prepare() {
        if (keysConfiguration != null) {
            var result = keysConfiguration.partition().map(this::readPublicKey)
                    .map(entry -> localPublicKeyServiceImpl().addRawKey(entry.getKey(), entry.getValue()))
                    .reduce(Result.success(), Result::merge);

            result.orElseThrow((failure) -> new EdcException(failure.getFailureDetail()));
        }
    }

    private Map.Entry<String, String> readPublicKey(Config config) {
        var id = config.getString(ID_SUFFIX);
        var value = config.getString(VALUE_SUFFIX);
        return Map.entry(id, value);
    }

}
