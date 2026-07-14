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

import org.eclipse.edc.document.cache.spi.CachedDocument;
import org.eclipse.edc.document.cache.spi.CachedDocumentType;
import org.eclipse.edc.document.cache.spi.store.CachedDocumentStore;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.validator.registration.spi.SchemaValidatorRegistration;
import org.eclipse.edc.validator.registration.spi.store.SchemaValidatorRegistrationStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchemaValidatorRegistrationServiceImplTest {

    private final SchemaValidatorRegistrationStore store = mock();
    private final CachedDocumentStore cachedDocumentStore = mock();
    private final DynamicSchemaValidatorRegistrar registrar = mock();
    private final SchemaValidatorRegistrationServiceImpl service = new SchemaValidatorRegistrationServiceImpl(
            new NoopTransactionContext(), store, cachedDocumentStore, registrar);

    private static final String SCHEMA = "https://example.com/schema/asset.json";

    private SchemaValidatorRegistration registration() {
        return SchemaValidatorRegistration.Builder.newInstance()
                .version("v5").validatedType("Asset").schema(SCHEMA).profiles(List.of()).build();
    }

    private CachedDocument schemaDocument(CachedDocumentType type) {
        return CachedDocument.Builder.newInstance().url(SCHEMA).content("{}").type(type).build();
    }

    @Test
    void create_whenSchemaNotCached_returnsBadRequest() {
        when(cachedDocumentStore.findByUrl(SCHEMA)).thenReturn(null);

        var result = service.create(registration());

        assertThat(result.failed()).isTrue();
        verify(store, never()).create(any());
        verify(registrar, never()).ensureRegistered(any(), any());
    }

    @Test
    void create_whenCachedButNotSchemaType_returnsBadRequest() {
        when(cachedDocumentStore.findByUrl(SCHEMA)).thenReturn(schemaDocument(CachedDocumentType.JSON_LD));

        var result = service.create(registration());

        assertThat(result.failed()).isTrue();
        verify(store, never()).create(any());
    }

    @Test
    void create_whenSchemaCached_succeedsAndRegisters() {
        when(cachedDocumentStore.findByUrl(SCHEMA)).thenReturn(schemaDocument(CachedDocumentType.JSON_SCHEMA));
        when(store.create(any())).thenAnswer(i -> StoreResult.success(i.getArgument(0)));

        var result = service.create(registration());

        assertThat(result.succeeded()).isTrue();
        verify(registrar).ensureRegistered("v5", "Asset");
    }

    @Test
    void create_stripsSchemaFragment_whenCheckingCache() {
        var withFragment = SchemaValidatorRegistration.Builder.newInstance()
                .version("v5").validatedType("PolicyDefinition")
                .schema(SCHEMA + "#/definitions/PolicyDefinition").profiles(List.of()).build();
        when(cachedDocumentStore.findByUrl(SCHEMA)).thenReturn(schemaDocument(CachedDocumentType.JSON_SCHEMA));
        when(store.create(any())).thenAnswer(i -> StoreResult.success(i.getArgument(0)));

        var result = service.create(withFragment);

        assertThat(result.succeeded()).isTrue();
        verify(cachedDocumentStore).findByUrl(SCHEMA); // looked up without the fragment
        verify(registrar).ensureRegistered("v5", "PolicyDefinition");
    }

    @Test
    void update_evictsPreviousSchemaAndReRegisters() {
        var existing = SchemaValidatorRegistration.Builder.newInstance()
                .id("id1").version("v5").validatedType("Asset").schema("https://example.com/schema/old.json").build();
        var updated = registration().toBuilder().id("id1").build();
        when(cachedDocumentStore.findByUrl(SCHEMA)).thenReturn(schemaDocument(CachedDocumentType.JSON_SCHEMA));
        when(store.findById("id1")).thenReturn(existing);
        when(store.update(any())).thenAnswer(i -> StoreResult.success(i.getArgument(0)));

        var result = service.update(updated);

        assertThat(result.succeeded()).isTrue();
        verify(registrar).evict("https://example.com/schema/old.json");
        verify(registrar).ensureRegistered("v5", "Asset");
    }

    @Test
    void delete_evictsSchema() {
        var existing = registration().toBuilder().id("id1").build();
        when(store.delete("id1")).thenReturn(StoreResult.success(existing));

        var result = service.deleteById("id1");

        assertThat(result.succeeded()).isTrue();
        verify(registrar).evict(SCHEMA);
    }
}
