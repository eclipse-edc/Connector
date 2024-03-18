/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.spi.store;

import org.eclipse.edc.connector.dataplane.spi.AccessTokenData;
import org.eclipse.edc.junit.assertions.AbstractResultAssert;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public abstract class AccessTokenDataTestBase {

    @Test
    void storeAndGetById() {
        var object = accessTokenData("1");
        getStore().store(object);
        assertThat(getStore().getById("1")).usingRecursiveComparison().isEqualTo(object);
    }

    @Test
    void getById_notFound() {
        assertThat(getStore().getById("not-exist")).isNull();
    }

    @Test
    void store_alreadyExists() {
        var object1 = accessTokenData("1");
        var objectDupl = accessTokenData("1");
        getStore().store(object1);

        AbstractResultAssert.assertThat(getStore().store(objectDupl)).isFailed()
                .detail().isEqualTo("AccessTokenData with ID '1' already exists.");
    }

    @Test
    void deleteById() {
        var object = accessTokenData("1");
        getStore().store(object);

        assertThat(getStore().deleteById("1").succeeded()).isTrue();
    }

    @Test
    void deleteById_notFound() {
        AbstractResultAssert.assertThat(getStore().deleteById("not-exist")).isFailed()
                .detail().isEqualTo("AccessTokenData with ID 'not-exist' does not exist.");
    }

    @Test
    void query_byId() {
        var atd = accessTokenData("test-id");
        getStore().store(atd);

        assertThat(getStore().query(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", "test-id")).build()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(atd);
    }

    @Test
    void query_byClaim() {
        var ct = ClaimToken.Builder.newInstance().claim("foo", "bar").build();
        var atd = new AccessTokenData("test-id", ct, dataAddress());
        getStore().store(atd);

        assertThat(getStore().query(QuerySpec.Builder.newInstance().filter(new Criterion("claimToken.claims.foo", "=", "bar")).build()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(atd);
    }

    @Test
    void query_byDataAddressProperty() {
        var ct = ClaimToken.Builder.newInstance().claim("foo", "bar").build();
        var atd = new AccessTokenData("test-id", ct, DataAddress.Builder.newInstance().type("foo-type").property("qux", "quz").build());
        getStore().store(atd);

        assertThat(getStore().query(QuerySpec.Builder.newInstance().filter(new Criterion("dataAddress.properties.qux", "=", "quz")).build()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(atd);
    }

    @Test
    void query_byMultipleCriteria() {
        var ct = ClaimToken.Builder.newInstance().claim("foo", "bar").build();
        var atd = new AccessTokenData("test-id", ct, DataAddress.Builder.newInstance().type("foo-type").property("qux", "quz").build());
        getStore().store(atd);

        assertThat(getStore().query(QuerySpec.Builder.newInstance().filter(List.of(
                new Criterion("dataAddress.properties.qux", "=", "quz"),
                new Criterion("claimToken.claims.foo", "=", "bar"),
                new Criterion("id", "like", "test-%"))).build()))
                .hasSize(1)
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(atd);
    }

    @Test
    void query_verifySorting() {
        IntStream.range(0, 100).forEach(i -> getStore().store(accessTokenData("id" + i)));

        var q = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.DESC).build();
        assertThat(getStore().query(q)).extracting(AccessTokenData::id).isSortedAccordingTo(Comparator.reverseOrder());

        var q2 = QuerySpec.Builder.newInstance().sortField("id").sortOrder(SortOrder.ASC).build();
        assertThat(getStore().query(q2)).extracting(AccessTokenData::id).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    void query_verifySorting_invalidSortField() {
        IntStream.range(0, 100).forEach(i -> getStore().store(accessTokenData("id" + i)));


        var q = QuerySpec.Builder.newInstance().sortField("not-exist").sortOrder(SortOrder.DESC).build();
        assertThatThrownBy(() -> getStore().query(q)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void verifyPaging() {
        IntStream.range(0, 100).forEach(i -> getStore().store(accessTokenData("id" + i)));

        var q = QuerySpec.Builder.newInstance().offset(40).limit(25).build();
        assertThat(getStore().query(q)).hasSize(25);
    }

    @Test
    void query_defaultQuerySpec() {
        IntStream.range(0, 100).forEach(i -> getStore().store(accessTokenData("id" + i)));
        var q = QuerySpec.none();
        assertThat(getStore().query(q)).hasSize(50);
    }

    @Test
    void update() {
        var object = accessTokenData("1");
        getStore().store(object);

        var update = new AccessTokenData("1", object.claimToken(), object.dataAddress(), Map.of("fizz", "buzz"));

        assertThat(getStore().update(update).succeeded()).isTrue();
    }

    @Test
    void update_whenNotExist() {
        var object = accessTokenData("1");
        assertThat(getStore().update(object).failed()).isTrue();
    }

    protected abstract AccessTokenDataStore getStore();

    protected DataAddress dataAddress() {
        return DataAddress.Builder.newInstance().type("foo-type").build();
    }

    protected AccessTokenData accessTokenData(String id) {
        return new AccessTokenData(id, ClaimToken.Builder.newInstance().build(), dataAddress(), Map.of("foo", List.of("bar", "baz")));
    }
}
