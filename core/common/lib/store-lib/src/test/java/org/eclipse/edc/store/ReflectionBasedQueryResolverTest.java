/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.edc.store;

import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QueryResolver;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.spi.query.Criterion.criterion;

class ReflectionBasedQueryResolverTest {

    private final QueryResolver<FakeItem> queryResolver = new ReflectionBasedQueryResolver<>(FakeItem.class, CriterionOperatorRegistryImpl.ofDefaults());

    @Test
    void verifyQuery_noFilters() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        var spec = QuerySpec.Builder.newInstance().build();
        assertThat(queryResolver.query(stream, spec)).hasSize(10);
    }

    @Test
    void verifyQuery_equalStringProperty() {
        var stream = Stream.concat(
                IntStream.range(0, 5).mapToObj(i -> new FakeItem(i, "Alice")),
                IntStream.range(5, 10).mapToObj(i -> new FakeItem(i, "Bob")));

        var spec = QuerySpec.Builder.newInstance().filter(criterion("name", "=", "Alice")).build();
        assertThat(queryResolver.query(stream, spec)).hasSize(5).extracting(FakeItem::getName).containsOnly("Alice");

    }

    @Test
    void verifyQuery_criterionFilterIntProperty() {
        var stream = Stream.concat(
                IntStream.range(0, 5).mapToObj(i -> new FakeItem(i, "Alice")),
                IntStream.range(5, 10).mapToObj(i -> new FakeItem(i, "Bob")));

        var spec = QuerySpec.Builder.newInstance().filter(List.of(new Criterion("name", "=", "Bob"))).build();
        Collection<FakeItem> actual = queryResolver.query(stream, spec).collect(Collectors.toList());
        assertThat(actual).hasSize(5).extracting(FakeItem::getName).containsOnly("Bob");
    }

    @Test
    void verifyQuery_sortDesc() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        var spec = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(queryResolver.query(stream, spec)).hasSize(10).isSortedAccordingTo(Comparator.comparing(FakeItem::getId).reversed());
    }

    @Test
    void verifyQuery_sortAsc() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        var spec = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(queryResolver.query(stream, spec)).hasSize(10).isSortedAccordingTo(Comparator.comparing(FakeItem::getId));
    }

    @Test
    void verifyQuery_invalidSortField() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        var spec = QuerySpec.Builder.newInstance().sortField("xyz").sortOrder(SortOrder.ASC).build();

        assertThatThrownBy(() -> queryResolver.query(stream, spec)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyQuery_offsetAndLimit() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        var spec = QuerySpec.Builder.newInstance().offset(1).limit(2).build();
        assertThat(queryResolver.query(stream, spec)).extracting(FakeItem::getId).containsExactly(1, 2);
    }

    @Test
    void verifyQuery_allFilters() {
        var stream = IntStream.range(0, 10).mapToObj(FakeItem::new);

        var spec = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).offset(1).limit(2).build();
        assertThat(queryResolver.query(stream, spec)).extracting(FakeItem::getId).containsExactly(8, 7);
    }

    @Test
    void verifyExceptionThrown_invalidCriterion() {
        var stream = Stream.concat(
                IntStream.range(0, 5).mapToObj(i -> new FakeItem(i, "Alice")),
                IntStream.range(5, 10).mapToObj(i -> new FakeItem(i, "Bob")));
        var criterion = new Criterion("name", "GREATER_THAN", "(Bob, Alice)");

        assertThatThrownBy(() -> queryResolver.query(stream, QuerySpec.Builder.newInstance().filter(List.of(criterion)).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Operator [GREATER_THAN] is not supported.");
    }

    private static class FakeItem {
        private final int id;
        private String name;

        private FakeItem(int id) {
            this.id = id;
        }

        private FakeItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FakeItem fakeItem = (FakeItem) o;
            return id == fakeItem.id && Objects.equals(name, fakeItem.name);
        }
    }

}
