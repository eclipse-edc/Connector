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

package org.eclipse.edc.connector.controlplane.services.partner;

import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PartnerServiceImplTest {

    private final PartnerStore partnerStore = mock();
    private final PartnerGroupStore partnerGroupStore = mock();
    private final QueryValidator queryValidator = mock();
    private final PartnerServiceImpl service = new PartnerServiceImpl(partnerStore, partnerGroupStore, new NoopTransactionContext(), queryValidator);

    @Test
    void findById_shouldReturnPartner() {
        var partner = partner("p1");
        when(partnerStore.findById("pc", "p1")).thenReturn(partner);

        assertThat(service.findById("pc", "p1")).isSucceeded().isEqualTo(partner);
    }

    @Test
    void findById_shouldFail_whenNotFound() {
        when(partnerStore.findById("pc", "p1")).thenReturn(null);

        assertThat(service.findById("pc", "p1")).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void findByIdentity_shouldReturnPartner() {
        var partner = partner("p1");
        when(partnerStore.findByIdentity("pc", "did:web:p1")).thenReturn(partner);

        assertThat(service.findByIdentity("pc", "did:web:p1")).isSucceeded().isEqualTo(partner);
    }

    @Test
    void findByIdentity_shouldFail_whenNotFound() {
        when(partnerStore.findByIdentity("pc", "did:web:p1")).thenReturn(null);

        assertThat(service.findByIdentity("pc", "did:web:p1")).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void search_shouldQueryStore() {
        var partner = partner("p1");
        when(queryValidator.validate(any())).thenReturn(Result.success());
        when(partnerStore.query(any())).thenReturn(List.of(partner));

        assertThat(service.search(QuerySpec.none())).isSucceeded().satisfies(l -> assertThat(l).containsExactly(partner));
    }

    @Test
    void search_shouldFail_whenQueryInvalid() {
        when(queryValidator.validate(any())).thenReturn(Result.failure("invalid"));

        assertThat(service.search(QuerySpec.none())).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verifyNoInteractions(partnerStore);
    }

    @Test
    void create_shouldStore() {
        var partner = partner("p1");
        when(partnerGroupStore.findById("pc", "gold")).thenReturn(group("gold"));
        when(partnerStore.create(partner)).thenReturn(StoreResult.success());

        assertThat(service.create(partner)).isSucceeded().isEqualTo(partner);
        verify(partnerStore).create(partner);
    }

    @Test
    void create_shouldFail_whenGroupUnknown() {
        var partner = partner("p1");
        when(partnerGroupStore.findById("pc", "gold")).thenReturn(null);

        assertThat(service.create(partner)).isFailed()
                .satisfies(f -> {
                    assertThat(f.getReason()).isEqualTo(BAD_REQUEST);
                    assertThat(f.getFailureDetail()).contains("gold");
                });
        verify(partnerStore, never()).create(any());
    }

    @Test
    void create_shouldFail_whenIdentityBlank() {
        var partner = Partner.Builder.newInstance().id("p1").participantContextId("pc").identity(" ").build();

        assertThat(service.create(partner)).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verifyNoInteractions(partnerStore);
    }

    @Test
    void create_shouldFail_whenAlreadyExists() {
        var partner = partner("p1");
        when(partnerGroupStore.findById("pc", "gold")).thenReturn(group("gold"));
        when(partnerStore.create(partner)).thenReturn(StoreResult.alreadyExists("exists"));

        assertThat(service.create(partner)).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
    }

    @Test
    void update_shouldStore() {
        var partner = partner("p1");
        when(partnerGroupStore.findById("pc", "gold")).thenReturn(group("gold"));
        when(partnerStore.update(partner)).thenReturn(StoreResult.success());

        assertThat(service.update(partner)).isSucceeded().isEqualTo(partner);
    }

    @Test
    void update_shouldFail_whenNotFound() {
        var partner = partner("p1");
        when(partnerGroupStore.findById("pc", "gold")).thenReturn(group("gold"));
        when(partnerStore.update(partner)).thenReturn(StoreResult.notFound("missing"));

        assertThat(service.update(partner)).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_shouldRemove() {
        var partner = partner("p1");
        when(partnerStore.findById("pc", "p1")).thenReturn(partner);
        when(partnerStore.deleteById("pc", "p1")).thenReturn(StoreResult.success());

        assertThat(service.delete("pc", "p1")).isSucceeded().isEqualTo(partner);
    }

    @Test
    void delete_shouldFail_whenNotFound() {
        when(partnerStore.findById("pc", "p1")).thenReturn(null);

        assertThat(service.delete("pc", "p1")).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        verify(partnerStore, never()).deleteById(any(), any());
    }

    private Partner partner(String id) {
        return Partner.Builder.newInstance().id(id).participantContextId("pc").identity("did:web:" + id).groupIds(Set.of("gold")).build();
    }

    private PartnerGroup group(String id) {
        return PartnerGroup.Builder.newInstance().id(id).participantContextId("pc").name(id).build();
    }
}
