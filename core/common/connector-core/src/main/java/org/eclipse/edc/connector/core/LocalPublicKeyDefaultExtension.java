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

import org.eclipse.edc.keys.LocalPublicKeyServiceImpl;
import org.eclipse.edc.keys.spi.KeyParserRegistry;
import org.eclipse.edc.keys.spi.LocalPublicKeyService;
import org.eclipse.edc.runtime.metamodel.annotation.Configuration;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.SettingContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static org.eclipse.edc.connector.core.SecurityDefaultServicesExtension.NAME;

@Extension(value = NAME)
@Provides(LocalPublicKeyService.class)
public class LocalPublicKeyDefaultExtension implements ServiceExtension {

    public static final String NAME = "Local Public Key Default Extension";

    public static final String EDC_PUBLIC_KEYS_PREFIX = "edc.iam.publickeys";

    @Inject
    public KeyParserRegistry keyParserRegistry;

    @SettingContext(EDC_PUBLIC_KEYS_PREFIX)
    @Configuration
    private Map<String, PublicKeyConfiguration> configurations;

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
    public void prepare() {
        var result = configurations.values().stream()
                .map(this::readPublicKey)
                .map(entry -> localPublicKeyServiceImpl().addRawKey(entry.getKey(), entry.getValue()))
                .reduce(Result.success(), Result::merge);

        result.orElseThrow((failure) -> new EdcException(failure.getFailureDetail()));
    }

    private Map.Entry<String, String> readPublicKey(PublicKeyConfiguration config) {
        var id = config.id();
        return Optional.ofNullable(config.value())
                .or(() -> Optional.ofNullable(config.path()).map(this::readFromPath))
                .map(key -> Map.entry(id, key))
                .orElseThrow(() -> new EdcException("Either 'value' or 'path' must be configured for public key '%s'".formatted(id)));
    }

    private String readFromPath(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
