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

package org.eclipse.edc.connector.controlplane.partner.spi.testfixtures.store;

import org.eclipse.edc.connector.controlplane.partner.spi.Partner;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreFailure;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

/**
 * Contract tests every {@link PartnerStore} implementation must pass.
 */
public abstract class PartnerStoreTestBase {

    protected static final String PARTICIPANT_CONTEXT_ID = "participant-context";
    protected static final String OTHER_PARTICIPANT_CONTEXT_ID = "other-participant-context";

    protected abstract PartnerStore getStore();

    protected Partner.Builder partnerBuilder(String id) {
        return Partner.Builder.newInstance()
                .id(id)
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .identity("did:web:" + id)
                .name("Partner " + id);
    }

    protected Partner partner(String id) {
        return partnerBuilder(id).build();
    }

    @Nested
    class Create {

        @Test
        void shouldStorePartner() {
            var partner = partnerBuilder("p1")
                    .property("businessId", "BID-0001")
                    .property("nested", Map.of("k", "v"))
                    .groupIds(Set.of("gold", "eu"))
                    .build();

            var result = getStore().create(partner);

            assertThat(result).isSucceeded();
            var stored = getStore().findById(PARTICIPANT_CONTEXT_ID, "p1");
            assertThat(stored).usingRecursiveComparison().ignoringFields("clock").isEqualTo(partner);
            assertThat(stored.getProperties()).containsEntry("businessId", "BID-0001");
            assertThat(stored.getGroupIds()).containsExactlyInAnyOrder("gold", "eu");
        }

        @Test
        void shouldFail_whenSameIdInSameParticipantContext() {
            getStore().create(partner("p1"));

            var result = getStore().create(partnerBuilder("p1").identity("did:web:another").build());

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_EXISTS);
        }

        @Test
        void shouldFail_whenSameIdentityInSameParticipantContext() {
            getStore().create(partner("p1"));

            var result = getStore().create(partnerBuilder("p2").identity("did:web:p1").build());

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_EXISTS);
        }

        @Test
        void shouldSucceed_whenSameIdAndIdentityInDifferentParticipantContext() {
            getStore().create(partner("p1"));

            var result = getStore().create(partnerBuilder("p1").participantContextId(OTHER_PARTICIPANT_CONTEXT_ID).build());

            assertThat(result).isSucceeded();
            assertThat(getStore().findById(PARTICIPANT_CONTEXT_ID, "p1")).isNotNull();
            assertThat(getStore().findById(OTHER_PARTICIPANT_CONTEXT_ID, "p1")).isNotNull();
        }
    }

    @Nested
    class FindById {

        @Test
        void shouldReturnNull_whenNotFound() {
            assertThat(getStore().findById(PARTICIPANT_CONTEXT_ID, "missing")).isNull();
        }

        @Test
        void shouldNotLeakAcrossParticipantContexts() {
            getStore().create(partner("p1"));

            assertThat(getStore().findById(OTHER_PARTICIPANT_CONTEXT_ID, "p1")).isNull();
        }
    }

    @Nested
    class FindByIdentity {

        @Test
        void shouldReturnPartner() {
            getStore().create(partner("p1"));
            getStore().create(partner("p2"));

            var found = getStore().findByIdentity(PARTICIPANT_CONTEXT_ID, "did:web:p2");

            assertThat(found).isNotNull().extracting(Partner::getId).isEqualTo("p2");
        }

        @Test
        void shouldReturnNull_whenNotFound() {
            getStore().create(partner("p1"));

            assertThat(getStore().findByIdentity(PARTICIPANT_CONTEXT_ID, "did:web:missing")).isNull();
        }

        @Test
        void shouldNotLeakAcrossParticipantContexts() {
            getStore().create(partner("p1"));

            assertThat(getStore().findByIdentity(OTHER_PARTICIPANT_CONTEXT_ID, "did:web:p1")).isNull();
        }
    }

    @Nested
    class Query {

        @Test
        void shouldFilterByParticipantContext() {
            getStore().create(partner("p1"));
            getStore().create(partner("p2"));
            getStore().create(partnerBuilder("p3").participantContextId(OTHER_PARTICIPANT_CONTEXT_ID).build());

            var result = getStore().query(queryByParticipantContextId(PARTICIPANT_CONTEXT_ID).build());

            assertThat(result).extracting(Partner::getId).containsExactlyInAnyOrder("p1", "p2");
        }

        @Test
        void shouldFilterByNestedProperty() {
            getStore().create(partnerBuilder("p1").property("businessId", "BID-0001").build());
            getStore().create(partnerBuilder("p2").property("businessId", "BID-0002").build());
            getStore().create(partnerBuilder("p3").build());

            var query = queryByParticipantContextId(PARTICIPANT_CONTEXT_ID)
                    .filter(criterion("properties.businessId", "=", "BID-0002"))
                    .build();

            assertThat(getStore().query(query)).extracting(Partner::getId).containsExactly("p2");
        }

        @Test
        void shouldFilterByGroupIdContains() {
            getStore().create(partnerBuilder("p1").groupIds(Set.of("gold", "eu")).build());
            getStore().create(partnerBuilder("p2").groupIds(Set.of("silver")).build());
            getStore().create(partnerBuilder("p3").groupIds(Set.of("gold")).build());

            var query = queryByParticipantContextId(PARTICIPANT_CONTEXT_ID)
                    .filter(criterion("groupIds", "contains", "gold"))
                    .build();

            assertThat(getStore().query(query)).extracting(Partner::getId).containsExactlyInAnyOrder("p1", "p3");
        }

        @Test
        void shouldFilterByIdentity() {
            getStore().create(partner("p1"));
            getStore().create(partner("p2"));

            var query = queryByParticipantContextId(PARTICIPANT_CONTEXT_ID)
                    .filter(criterion("identity", "=", "did:web:p1"))
                    .build();

            assertThat(getStore().query(query)).extracting(Partner::getId).containsExactly("p1");
        }

        @Test
        void shouldReturnEmpty_whenPropertyDoesNotExist() {
            getStore().create(partner("p1"));

            var query = QuerySpec.Builder.newInstance().filter(criterion("notexist", "=", "x")).build();

            assertThat(getStore().query(query)).isEmpty();
        }

        @Test
        void shouldSortAndPage() {
            IntStream.range(0, 10).mapToObj(i -> partner("p" + i)).forEach(getStore()::create);

            var query = queryByParticipantContextId(PARTICIPANT_CONTEXT_ID)
                    .sortField("id").sortOrder(SortOrder.DESC)
                    .offset(2).limit(3)
                    .build();

            assertThat(getStore().query(query)).extracting(Partner::getId).containsExactly("p7", "p6", "p5");
        }

        @Test
        void shouldThrow_whenSortingByNonExistentField() {
            getStore().create(partner("p1"));

            var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

            assertThatThrownBy(() -> getStore().query(query)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdateFields() {
            getStore().create(partnerBuilder("p1").property("businessId", "old").groupIds(Set.of("gold")).build());

            var updated = partnerBuilder("p1")
                    .identity("did:web:renamed")
                    .name("renamed")
                    .properties(Map.of("businessId", "new", "tier", 1))
                    .groupIds(Set.of("silver"))
                    .build();
            var result = getStore().update(updated);

            assertThat(result).isSucceeded();
            var stored = getStore().findById(PARTICIPANT_CONTEXT_ID, "p1");
            assertThat(stored.getIdentity()).isEqualTo("did:web:renamed");
            assertThat(stored.getName()).isEqualTo("renamed");
            assertThat(stored.getProperties()).containsEntry("businessId", "new").containsKey("tier");
            assertThat(stored.getGroupIds()).containsExactly("silver");
        }

        @Test
        void shouldFail_whenNotFound() {
            var result = getStore().update(partner("missing"));

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldFail_whenIdentityTakenByAnotherPartner() {
            getStore().create(partner("p1"));
            getStore().create(partner("p2"));

            var result = getStore().update(partnerBuilder("p2").identity("did:web:p1").build());

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_EXISTS);
        }

        @Test
        void shouldNotAffectOtherParticipantContext() {
            getStore().create(partner("p1"));
            getStore().create(partnerBuilder("p1").participantContextId(OTHER_PARTICIPANT_CONTEXT_ID).build());

            var result = getStore().update(partnerBuilder("p1").name("changed").build());

            assertThat(result).isSucceeded();
            assertThat(getStore().findById(OTHER_PARTICIPANT_CONTEXT_ID, "p1").getName()).isEqualTo("Partner p1");
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDelete() {
            getStore().create(partner("p1"));

            var result = getStore().deleteById(PARTICIPANT_CONTEXT_ID, "p1");

            assertThat(result).isSucceeded();
            assertThat(getStore().findById(PARTICIPANT_CONTEXT_ID, "p1")).isNull();
        }

        @Test
        void shouldFail_whenNotFound() {
            var result = getStore().deleteById(PARTICIPANT_CONTEXT_ID, "missing");

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotDeleteInOtherParticipantContext() {
            getStore().create(partner("p1"));

            var result = getStore().deleteById(OTHER_PARTICIPANT_CONTEXT_ID, "p1");

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
            assertThat(getStore().findById(PARTICIPANT_CONTEXT_ID, "p1")).isNotNull();
        }
    }
}
