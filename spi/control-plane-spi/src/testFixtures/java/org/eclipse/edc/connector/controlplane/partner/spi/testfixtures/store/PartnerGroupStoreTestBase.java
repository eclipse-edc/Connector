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

import org.eclipse.edc.connector.controlplane.partner.spi.PartnerGroup;
import org.eclipse.edc.connector.controlplane.partner.spi.store.PartnerGroupStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreFailure;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

/**
 * Contract tests every {@link PartnerGroupStore} implementation must pass.
 */
public abstract class PartnerGroupStoreTestBase {

    protected static final String PARTICIPANT_CONTEXT_ID = "participant-context";
    protected static final String OTHER_PARTICIPANT_CONTEXT_ID = "other-participant-context";

    protected abstract PartnerGroupStore getStore();

    protected PartnerGroup.Builder groupBuilder(String id) {
        return PartnerGroup.Builder.newInstance()
                .id(id)
                .participantContextId(PARTICIPANT_CONTEXT_ID)
                .name("Group " + id);
    }

    protected PartnerGroup group(String id) {
        return groupBuilder(id).build();
    }

    @Nested
    class Create {

        @Test
        void shouldStoreGroup() {
            var group = groupBuilder("g1").description("desc").property("region", "eu").build();

            var result = getStore().create(group);

            assertThat(result).isSucceeded();
            var stored = getStore().findById(PARTICIPANT_CONTEXT_ID, "g1");
            assertThat(stored).usingRecursiveComparison().ignoringFields("clock").isEqualTo(group);
        }

        @Test
        void shouldFail_whenSameIdInSameParticipantContext() {
            getStore().create(group("g1"));

            var result = getStore().create(group("g1"));

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(ALREADY_EXISTS);
        }

        @Test
        void shouldSucceed_whenSameIdInDifferentParticipantContext() {
            getStore().create(group("g1"));

            var result = getStore().create(groupBuilder("g1").participantContextId(OTHER_PARTICIPANT_CONTEXT_ID).build());

            assertThat(result).isSucceeded();
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
            getStore().create(group("g1"));

            assertThat(getStore().findById(OTHER_PARTICIPANT_CONTEXT_ID, "g1")).isNull();
        }
    }

    @Nested
    class Query {

        @Test
        void shouldFilterByParticipantContext() {
            getStore().create(group("g1"));
            getStore().create(group("g2"));
            getStore().create(groupBuilder("g3").participantContextId(OTHER_PARTICIPANT_CONTEXT_ID).build());

            var result = getStore().query(queryByParticipantContextId(PARTICIPANT_CONTEXT_ID).build());

            assertThat(result).extracting(PartnerGroup::getId).containsExactlyInAnyOrder("g1", "g2");
        }

        @Test
        void shouldFilterByName() {
            getStore().create(group("g1"));
            getStore().create(group("g2"));

            var query = queryByParticipantContextId(PARTICIPANT_CONTEXT_ID).filter(criterion("name", "=", "Group g2")).build();

            assertThat(getStore().query(query)).extracting(PartnerGroup::getId).containsExactly("g2");
        }

        @Test
        void shouldFilterByNestedProperty() {
            getStore().create(groupBuilder("g1").property("region", "eu").build());
            getStore().create(groupBuilder("g2").property("region", "us").build());

            var query = queryByParticipantContextId(PARTICIPANT_CONTEXT_ID).filter(criterion("properties.region", "=", "us")).build();

            assertThat(getStore().query(query)).extracting(PartnerGroup::getId).containsExactly("g2");
        }

        @Test
        void shouldReturnEmpty_whenPropertyDoesNotExist() {
            getStore().create(group("g1"));

            var query = QuerySpec.Builder.newInstance().filter(criterion("notexist", "=", "x")).build();

            assertThat(getStore().query(query)).isEmpty();
        }

        @Test
        void shouldSortAndPage() {
            IntStream.range(0, 10).mapToObj(i -> group("g" + i)).forEach(getStore()::create);

            var query = queryByParticipantContextId(PARTICIPANT_CONTEXT_ID)
                    .sortField("id").sortOrder(SortOrder.ASC)
                    .offset(1).limit(2)
                    .build();

            assertThat(getStore().query(query)).extracting(PartnerGroup::getId).containsExactly("g1", "g2");
        }

        @Test
        void shouldThrow_whenSortingByNonExistentField() {
            getStore().create(group("g1"));

            var query = QuerySpec.Builder.newInstance().sortField("notexist").sortOrder(SortOrder.DESC).build();

            assertThatThrownBy(() -> getStore().query(query)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class Update {

        @Test
        void shouldUpdateFields() {
            getStore().create(groupBuilder("g1").property("region", "eu").build());

            var result = getStore().update(groupBuilder("g1").name("renamed").description("d").properties(Map.of("region", "us")).build());

            assertThat(result).isSucceeded();
            var stored = getStore().findById(PARTICIPANT_CONTEXT_ID, "g1");
            assertThat(stored.getName()).isEqualTo("renamed");
            assertThat(stored.getDescription()).isEqualTo("d");
            assertThat(stored.getProperties()).containsEntry("region", "us");
        }

        @Test
        void shouldFail_whenNotFound() {
            var result = getStore().update(group("missing"));

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotAffectOtherParticipantContext() {
            getStore().create(group("g1"));
            getStore().create(groupBuilder("g1").participantContextId(OTHER_PARTICIPANT_CONTEXT_ID).build());

            getStore().update(groupBuilder("g1").name("changed").build());

            assertThat(getStore().findById(OTHER_PARTICIPANT_CONTEXT_ID, "g1").getName()).isEqualTo("Group g1");
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDelete() {
            getStore().create(group("g1"));

            var result = getStore().deleteById(PARTICIPANT_CONTEXT_ID, "g1");

            assertThat(result).isSucceeded();
            assertThat(getStore().findById(PARTICIPANT_CONTEXT_ID, "g1")).isNull();
        }

        @Test
        void shouldFail_whenNotFound() {
            var result = getStore().deleteById(PARTICIPANT_CONTEXT_ID, "missing");

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }

        @Test
        void shouldNotDeleteInOtherParticipantContext() {
            getStore().create(group("g1"));

            var result = getStore().deleteById(OTHER_PARTICIPANT_CONTEXT_ID, "g1");

            assertThat(result).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
            assertThat(getStore().findById(PARTICIPANT_CONTEXT_ID, "g1")).isNotNull();
        }
    }
}
