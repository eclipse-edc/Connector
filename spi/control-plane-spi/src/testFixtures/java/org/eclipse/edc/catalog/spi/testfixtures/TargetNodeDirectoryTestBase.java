/*
 *  Copyright (c) 2024 Amadeus IT Group
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Amadeus IT Group - initial API and implementation
 *       Think-it GmbH - multi-participant support
 *
 */

package org.eclipse.edc.catalog.spi.testfixtures;

import org.eclipse.edc.crawler.spi.TargetNode;
import org.eclipse.edc.crawler.spi.TargetNodeDirectory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class TargetNodeDirectoryTestBase {

    protected static final String PARTICIPANT_CONTEXT_ID = "participant-context-id";
    protected static final String OTHER_PARTICIPANT_CONTEXT_ID = "other-participant-context-id";

    protected abstract TargetNodeDirectory getStore();

    private TargetNode createTargetNode(String id) {
        return createTargetNode(id, PARTICIPANT_CONTEXT_ID);
    }

    private TargetNode createTargetNode(String id, String participantContextId) {
        return new TargetNode(UUID.randomUUID().toString(), id, "http://example.com", participantContextId, List.of());
    }

    @Nested
    class Insert {

        @Test
        void insert_notExisting_shouldInsert() {
            var node = createTargetNode(UUID.randomUUID().toString());

            getStore().insert(node);

            var result = getStore().getAll(PARTICIPANT_CONTEXT_ID);

            assertThat(result)
                    .hasSize(1)
                    .allSatisfy(n -> assertThat(n).usingRecursiveComparison().isEqualTo(node));
        }

        @Test
        void insert_existing_shouldUpdate() {
            var id = UUID.randomUUID().toString();
            var node1 = createTargetNode(id);
            var node2 = createTargetNode(id);

            getStore().insert(node1);
            getStore().insert(node2);

            var result = getStore().getAll(PARTICIPANT_CONTEXT_ID);

            assertThat(result)
                    .hasSize(1)
                    .allSatisfy(n -> assertThat(n).usingRecursiveComparison().isEqualTo(node2));
        }

        @Test
        void insert_sameIdDifferentParticipant_shouldBeIsolated() {
            var id = UUID.randomUUID().toString();
            var node = createTargetNode(id, PARTICIPANT_CONTEXT_ID);
            var otherNode = createTargetNode(id, OTHER_PARTICIPANT_CONTEXT_ID);

            getStore().insert(node);
            getStore().insert(otherNode);

            assertThat(getStore().getAll(PARTICIPANT_CONTEXT_ID))
                    .hasSize(1)
                    .allSatisfy(n -> assertThat(n).usingRecursiveComparison().isEqualTo(node));
            assertThat(getStore().getAll(OTHER_PARTICIPANT_CONTEXT_ID))
                    .hasSize(1)
                    .allSatisfy(n -> assertThat(n).usingRecursiveComparison().isEqualTo(otherNode));
        }
    }

    @Nested
    class GetAll {

        @Test
        void getAll() {
            var nodes = List.of(createTargetNode(UUID.randomUUID().toString()), createTargetNode(UUID.randomUUID().toString()));

            nodes.forEach(getStore()::insert);

            var result = getStore().getAll(PARTICIPANT_CONTEXT_ID);

            assertThat(result)
                    .hasSize(2)
                    .anySatisfy(n -> assertThat(n.id()).isEqualTo(nodes.get(0).id()))
                    .anySatisfy(n -> assertThat(n.id()).isEqualTo(nodes.get(1).id()));
        }

        @Test
        void getAll_shouldOnlyReturnNodesOfParticipant() {
            getStore().insert(createTargetNode(UUID.randomUUID().toString(), PARTICIPANT_CONTEXT_ID));
            getStore().insert(createTargetNode(UUID.randomUUID().toString(), OTHER_PARTICIPANT_CONTEXT_ID));

            assertThat(getStore().getAll(PARTICIPANT_CONTEXT_ID)).hasSize(1)
                    .allSatisfy(n -> assertThat(n.participantContextId()).isEqualTo(PARTICIPANT_CONTEXT_ID));
        }
    }

    @Nested
    class Remove {

        @Test
        void remove_shouldRemoveAndReturnNode() {
            var node = createTargetNode(UUID.randomUUID().toString());
            getStore().insert(node);

            var removed = getStore().remove(PARTICIPANT_CONTEXT_ID, node.id());

            assertThat(removed).usingRecursiveComparison().isEqualTo(node);
            assertThat(getStore().getAll(PARTICIPANT_CONTEXT_ID)).isEmpty();
        }

        @Test
        void remove_notFound_shouldReturnNull() {
            var result = getStore().remove(PARTICIPANT_CONTEXT_ID, "non-existent-id");

            assertThat(result).isNull();
        }

        @Test
        void remove_differentParticipant_shouldNotRemove() {
            var node = createTargetNode(UUID.randomUUID().toString(), PARTICIPANT_CONTEXT_ID);
            getStore().insert(node);

            var result = getStore().remove(OTHER_PARTICIPANT_CONTEXT_ID, node.id());

            assertThat(result).isNull();
            assertThat(getStore().getAll(PARTICIPANT_CONTEXT_ID)).hasSize(1);
        }
    }
}
