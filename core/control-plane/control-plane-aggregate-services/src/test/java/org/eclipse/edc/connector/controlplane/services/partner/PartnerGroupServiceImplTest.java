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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.BAD_REQUEST;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PartnerGroupServiceImplTest {

    private final PartnerGroupStore partnerGroupStore = mock();
    private final PartnerStore partnerStore = mock();
    private final QueryValidator queryValidator = mock();
    private final PartnerGroupServiceImpl service = new PartnerGroupServiceImpl(partnerGroupStore, partnerStore, new NoopTransactionContext(), queryValidator);

    @Test
    void findById_shouldReturnGroup() {
        var group = group("g1");
        when(partnerGroupStore.findById("pc", "g1")).thenReturn(group);

        assertThat(service.findById("pc", "g1")).isSucceeded().isEqualTo(group);
    }

    @Test
    void findById_shouldFail_whenNotFound() {
        when(partnerGroupStore.findById("pc", "g1")).thenReturn(null);

        assertThat(service.findById("pc", "g1")).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void search_shouldQueryStore() {
        var group = group("g1");
        when(queryValidator.validate(any())).thenReturn(Result.success());
        when(partnerGroupStore.query(any())).thenReturn(List.of(group));

        assertThat(service.search(QuerySpec.none())).isSucceeded().satisfies(l -> assertThat(l).containsExactly(group));
    }

    @Test
    void search_shouldFail_whenQueryInvalid() {
        when(queryValidator.validate(any())).thenReturn(Result.failure("invalid"));

        assertThat(service.search(QuerySpec.none())).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verifyNoInteractions(partnerGroupStore);
    }

    @Test
    void create_shouldStore() {
        var group = group("g1");
        when(partnerGroupStore.create(group)).thenReturn(StoreResult.success());

        assertThat(service.create(group)).isSucceeded().isEqualTo(group);
    }

    @Test
    void create_shouldFail_whenAlreadyExists() {
        var group = group("g1");
        when(partnerGroupStore.create(group)).thenReturn(StoreResult.alreadyExists("exists"));

        assertThat(service.create(group)).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
    }

    @Test
    void create_shouldFail_whenNameBlank() {
        var group = PartnerGroup.Builder.newInstance().id("g1").participantContextId("pc").name(" ").build();

        assertThat(service.create(group)).isFailed().extracting(ServiceFailure::getReason).isEqualTo(BAD_REQUEST);
        verifyNoInteractions(partnerGroupStore);
    }

    @Test
    void update_shouldFail_whenNotFound() {
        var group = group("g1");
        when(partnerGroupStore.update(group)).thenReturn(StoreResult.notFound("missing"));

        assertThat(service.update(group)).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
    }

    @Test
    void delete_shouldRemove_whenNotReferenced() {
        var group = group("g1");
        when(partnerGroupStore.findById("pc", "g1")).thenReturn(group);
        when(partnerStore.query(any())).thenReturn(List.of());
        when(partnerGroupStore.deleteById("pc", "g1")).thenReturn(StoreResult.success());

        assertThat(service.delete("pc", "g1")).isSucceeded().isEqualTo(group);
        verify(partnerStore).query(argThat(q -> q.getFilterExpression().contains(criterion("groupIds", "contains", "g1"))));
    }

    @Test
    void delete_shouldFail_whenReferencedByPartner() {
        when(partnerGroupStore.findById("pc", "g1")).thenReturn(group("g1"));
        when(partnerStore.query(any())).thenReturn(List.of(Partner.Builder.newInstance().participantContextId("pc").identity("x").groupId("g1").build()));

        assertThat(service.delete("pc", "g1")).isFailed().extracting(ServiceFailure::getReason).isEqualTo(CONFLICT);
        verify(partnerGroupStore, never()).deleteById(any(), any());
    }

    @Test
    void delete_shouldFail_whenNotFound() {
        when(partnerGroupStore.findById("pc", "g1")).thenReturn(null);

        assertThat(service.delete("pc", "g1")).isFailed().extracting(ServiceFailure::getReason).isEqualTo(NOT_FOUND);
        verifyNoInteractions(partnerStore);
    }

    private PartnerGroup group(String id) {
        return PartnerGroup.Builder.newInstance().id(id).participantContextId("pc").name("Group " + id).build();
    }
}
