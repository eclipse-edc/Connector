/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
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

package org.eclipse.edc.participantcontext.spi.store;

import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.Test;

import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.participantcontext.spi.types.ParticipantResource.queryByParticipantContextId;

public abstract class ParticipantContextStoreTestBase {

    protected abstract ParticipantContextStore getStore();


    @Test
    void create() {
        var participantContext = createParticipantContext();
        var result = getStore().create(participantContext);
        assertThat(result).isSucceeded();
        var query = getStore().query(QuerySpec.max());
        assertThat(query).isSucceeded();
        assertThat(query.getContent()).usingRecursiveFieldByFieldElementComparator().containsExactly(participantContext);
    }

    @Test
    void create_whenExists_shouldReturnFailure() {
        var context = createParticipantContext();
        var result = getStore().create(context);
        assertThat(result).isSucceeded();
        var result2 = getStore().create(context);

        assertThat(result2).isFailed().detail().contains("already exists");
    }

    @Test
    void query_byId() {
        range(0, 5)
                .mapToObj(i -> createParticipantContext("id" + i))
                .forEach(getStore()::create);

        var query = queryByParticipantContextId("id2")
                .build();

        assertThat(getStore().query(query)).isSucceeded()
                .satisfies(str -> assertThat(str).hasSize(1));
    }

    @Test
    void query_noQuerySpec() {
        var resources = range(0, 5)
                .mapToObj(i -> createParticipantContext("id" + i))
                .toList();

        resources.forEach(getStore()::create);

        var res = getStore().query(QuerySpec.none());
        assertThat(res).isSucceeded();
        assertThat(res.getContent())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(resources.toArray(new ParticipantContext[0]));
    }

    @Test
    void query_whenNotFound() {
        var resources = range(0, 5)
                .mapToObj(i -> createParticipantContext("id" + i))
                .toList();

        resources.forEach(getStore()::create);

        var query = queryByParticipantContextId("id7")
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        assertThat(res.getContent()).isEmpty();
    }

    @Test
    void query_byInvalidField_shouldReturnEmptyList() {
        var resources = range(0, 5)
                .mapToObj(i -> createParticipantContext("id" + i))
                .toList();


        resources.forEach(getStore()::create);

        var query = QuerySpec.Builder.newInstance()
                .filter(new Criterion("invalidField", "=", "test-value"))
                .build();
        var res = getStore().query(query);
        assertThat(res).isSucceeded();
        assertThat(res.getContent()).isNotNull().isEmpty();
    }

    @Test
    void update() {
        var participantContext = createParticipantContext();
        var result = getStore().create(participantContext);
        assertThat(result).isSucceeded();

        var updateRes = getStore().update(new ParticipantContext(participantContext.getParticipantContextId()));
        assertThat(updateRes).isSucceeded();
    }

    @Test
    void update_whenNotExists() {
        var context = createParticipantContext("another-id");

        var updateRes = getStore().update(context);
        assertThat(updateRes).isFailed().detail().contains("with ID 'another-id' not found.");
    }

    @Test
    void delete() {
        var context = createParticipantContext();
        getStore().create(context);

        var deleteRes = getStore().deleteById(context.getParticipantContextId());
        assertThat(deleteRes).isSucceeded();
    }

    @Test
    void delete_whenNotExists() {
        assertThat(getStore().deleteById("not-exist")).isFailed()
                .detail().contains("with ID 'not-exist' not found.");
    }
    
    private ParticipantContext createParticipantContext(String id) {
        return new ParticipantContext(id);
    }

    private ParticipantContext createParticipantContext() {
        return createParticipantContext("test-participant");
    }

}
