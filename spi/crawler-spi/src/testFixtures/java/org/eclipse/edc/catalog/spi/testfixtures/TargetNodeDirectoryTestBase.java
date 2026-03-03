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

    protected abstract TargetNodeDirectory getStore();

    private TargetNode createTargetNode(String id) {
        return new TargetNode(UUID.randomUUID().toString(), id, "http://example.com", List.of());
    }

    @Nested
    class Insert {

        @Test
        void insert_notExisting_shouldInsert() {
            var node = createTargetNode(UUID.randomUUID().toString());

            getStore().insert(node);

            var result = getStore().getAll();

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

            var result = getStore().getAll();

            assertThat(result)
                    .hasSize(1)
                    .allSatisfy(n -> assertThat(n).usingRecursiveComparison().isEqualTo(node2));
        }
    }

    @Nested
    class GetAll {

        @Test
        void getAll() {
            var nodes = List.of(createTargetNode(UUID.randomUUID().toString()), createTargetNode(UUID.randomUUID().toString()));

            nodes.forEach(getStore()::insert);

            var result = getStore().getAll();

            assertThat(result)
                    .hasSize(2)
                    .anySatisfy(n -> assertThat(n.id()).isEqualTo(nodes.get(0).id()))
                    .anySatisfy(n -> assertThat(n.id()).isEqualTo(nodes.get(1).id()));
        }
    }

    @Nested
    class Remove {

        @Test
        void remove_shouldRemoveAndReturnNode() {
            var node = createTargetNode(UUID.randomUUID().toString());
            getStore().insert(node);

            var removed = getStore().remove(node.id());

            assertThat(removed).usingRecursiveComparison().isEqualTo(node);
            assertThat(getStore().getAll()).isEmpty();
        }

        @Test
        void remove_notFound_shouldReturnNull() {
            var result = getStore().remove("non-existent-id");

            assertThat(result).isNull();
        }
    }
}
