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

import org.eclipse.edc.document.cache.spi.CachedDocumentType;
import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transaction.spi.TransactionContext;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistrationService;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SchemaValidatorRegistrationServiceImpl implements SchemaValidatorRegistrationService {

    private final TransactionContext transactionContext;
    private final SchemaValidatorRegistrationStore store;
    private final CachedDocumentStore cachedDocumentStore;
    private final DynamicSchemaValidatorRegistrar registrar;

    public SchemaValidatorRegistrationServiceImpl(TransactionContext transactionContext, SchemaValidatorRegistrationStore store,
                                                  CachedDocumentStore cachedDocumentStore, DynamicSchemaValidatorRegistrar registrar) {
        this.transactionContext = transactionContext;
        this.store = store;
        this.cachedDocumentStore = cachedDocumentStore;
        this.registrar = registrar;
    }

    @Override
    public SchemaValidatorRegistration findById(String id) {
        return transactionContext.execute(() -> store.findById(id));
    }

    @Override
    public ServiceResult<List<SchemaValidatorRegistration>> search(QuerySpec query) {
        return transactionContext.execute(() -> {
            try (var stream = store.findAll(query)) {
                return ServiceResult.success(stream.toList());
            }
        });
    }

    @Override
    public @NotNull ServiceResult<SchemaValidatorRegistration> create(SchemaValidatorRegistration registration) {
        return transactionContext.execute(() -> schemaAvailable(registration.getSchema())
                .compose(v -> ServiceResult.from(store.create(registration)))
                .onSuccess(this::register));
    }

    @Override
    public @NotNull ServiceResult<SchemaValidatorRegistration> update(SchemaValidatorRegistration registration) {
        return transactionContext.execute(() -> schemaAvailable(registration.getSchema())
                .compose(v -> {
                    var existing = store.findById(registration.getId());
                    return ServiceResult.from(store.update(registration))
                            .onSuccess(updated -> {
                                if (existing != null) {
                                    registrar.evict(existing.getSchema());
                                }
                                register(updated);
                            });
                }));
    }

    @Override
    public @NotNull ServiceResult<SchemaValidatorRegistration> deleteById(String id) {
        return transactionContext.execute(() -> ServiceResult.from(store.delete(id))
                .onSuccess(deleted -> registrar.evict(deleted.getSchema())));
    }

    private ServiceResult<Void> schemaAvailable(String schema) {
        // A schema reference may target a subschema through a fragment (e.g. '.../schema.json#/definitions/Foo').
        // Only the document URL (the part before '#') is cached, so strip the fragment before the lookup.
        var documentUrl = stripFragment(schema);
        var document = cachedDocumentStore.findByUrl(documentUrl);
        if (document == null || document.getType() != CachedDocumentType.JSON_SCHEMA) {
            return ServiceResult.badRequest("The referenced schema '%s' is not cached as a JSON_SCHEMA document. Cache it first via the document cache API.".formatted(documentUrl));
        }
        return ServiceResult.success();
    }

    private String stripFragment(String schema) {
        var hashIndex = schema.indexOf('#');
        return hashIndex < 0 ? schema : schema.substring(0, hashIndex);
    }

    private void register(SchemaValidatorRegistration registration) {
        registrar.ensureRegistered(registration.getVersion(), registration.getValidatedType());
    }
}
