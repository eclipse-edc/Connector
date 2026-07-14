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

package org.eclipse.edc.validator.registration;

import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorFactory;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistrationService;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.eclipse.edc.validator.spi.JsonObjectValidatorRegistry;

import static org.eclipse.edc.validator.registration.SchemaValidationCoreExtension.NAME;

/**
 * Provides the {@link SchemaValidatorRegistrationService} and re-activates persisted registrations at boot so the
 * corresponding validators are wired into the {@link JsonObjectValidatorRegistry} across restarts.
 */
@Extension(NAME)
public class SchemaValidationCoreExtension implements ServiceExtension {

    public static final String NAME = "Schema Validator Registration";

    @Inject
    private SchemaValidatorRegistrationStore store;
    @Inject
    private SchemaValidatorFactory schemaValidatorFactory;
    @Inject
    private JsonObjectValidatorRegistry validatorRegistry;
    @Inject
    private CachedDocumentStore cachedDocumentStore;
    @Inject
    private TransactionContext transactionContext;

    private DynamicSchemaValidatorRegistrar registrar;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        registrar = new DynamicSchemaValidatorRegistrar(validatorRegistry, schemaValidatorFactory, store);
    }

    @Provider
    public SchemaValidatorRegistrationService schemaValidatorRegistrationService() {
        return new SchemaValidatorRegistrationServiceImpl(transactionContext, store, cachedDocumentStore, registrar);
    }

    @Override
    public void start() {
        transactionContext.execute(() -> {
            try (var stream = store.findAll(QuerySpec.max())) {
                stream.forEach(registration -> registrar.ensureRegistered(registration.getVersion(), registration.getValidatedType()));
            }
        });
    }
}
