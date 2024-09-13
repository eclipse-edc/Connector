/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures;

import org.eclipse.edc.iam.identitytrust.sts.spi.model.StsClient;
import org.eclipse.edc.iam.identitytrust.sts.spi.store.StsClientStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.spi.result.StoreFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClient;
import static org.eclipse.edc.iam.identitytrust.sts.spi.store.fixtures.TestFunctions.createClientBuilder;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

/**
 * Base compliance tests for implementors of {@link StsClientStore}.
 */
public abstract class StsClientStoreTestBase {

    protected abstract StsClientStore getStsClientStore();

    protected String getRandomId() {
        return UUID.randomUUID().toString();
    }

    private List<StsClient> createClients(int size) {
        return IntStream.range(0, size).mapToObj(i -> createClient("id" + i))
                .toList();
    }

    private void delay(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveClients(List<StsClient> clients) {
        clients.forEach(getStsClientStore()::create);
    }

    @Nested
    class Create {

        @Test
        @DisplayName("Save a single client that not exists")
        void create() {
            var client = createClient(getRandomId());
            assertThat(getStsClientStore().create(client)).isSucceeded();

            var clientFromDb = getStsClientStore().findByClientId(client.getId()).getContent();
            assertThat(client).usingRecursiveComparison().isEqualTo(clientFromDb);
        }


        @Test
        @DisplayName("Saves multiple client that not exists")
        void create_MultipleClients() {

            var clients = IntStream.range(0, 10)
                    .mapToObj(i -> createClient("id" + i))
                    .peek(getStsClientStore()::create)
                    .toList();

            var result = getStsClientStore().findAll(QuerySpec.max());

            assertThat(result).hasSize(clients.size())
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsAll(clients);
        }

        @Test
        @DisplayName("Shouldn't save a single client that already exists")
        void alreadyExist_shouldNotUpdate() {
            var client = createClient("id");
            getStsClientStore().create(client);
            var saveResult = getStsClientStore().create(createClient("id"));

            assertThat(saveResult.failed()).isTrue();
            assertThat(saveResult.reason()).isEqualTo(ALREADY_EXISTS);

            var result = getStsClientStore().findAll(QuerySpec.max());

            assertThat(result).hasSize(1)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(client);
        }
    }

    @Nested
    class FindAll {

        @ParameterizedTest
        @ValueSource(ints = { 49, 50, 51, 100 })
        void verifyQueryDefaults(int size) {
            var all = IntStream.range(0, size).mapToObj(i -> createClient("id" + i))
                    .peek(getStsClientStore()::create)
                    .collect(Collectors.toList());

            assertThat(getStsClientStore().findAll(QuerySpec.max())).hasSize(size)
                    .usingRecursiveFieldByFieldElementComparator()
                    .isSubsetOf(all);
        }

        @Test
        @DisplayName("Find all clients with limit and offset")
        void withSpec() {
            var limit = 20;

            IntStream.range(0, 50).mapToObj(i -> createClient("id" + i))
                    .forEach(getStsClientStore()::create);

            var spec = QuerySpec.Builder.newInstance()
                    .limit(limit)
                    .offset(20)
                    .build();

            var resultClients = getStsClientStore().findAll(spec);

            assertThat(resultClients).isNotNull().hasSize(limit);
        }

        @ParameterizedTest
        @ArgumentsSource(FilterArgumentProvider.class)
        void query_withQuerySpec(String field, Function<StsClient, String> mapping) {
            var clients = createClients(10);
            saveClients(clients);


            var client = createClientBuilder("id")
                    .name("client_name")
                    .clientId("client_id")
                    .did("did:web:client")
                    .secretAlias("secret_alias")
                    .privateKeyAlias("private_key_alias")
                    .publicKeyReference("public_key_reference")
                    .build();

            getStsClientStore().create(client);

            var filter = Criterion.Builder.newInstance()
                    .operandLeft(field)
                    .operator("=")
                    .operandRight(mapping.apply(client))
                    .build();

            var results = getStsClientStore().findAll(QuerySpec.Builder.newInstance().filter(filter).build());

            assertThat(results).usingRecursiveFieldByFieldElementComparator()
                    .containsOnly(client);

        }

        @Test
        @DisplayName("Verify empty result when query contains a nonexistent value")
        void queryByNonexistentValue() {

            var clients = createClients(20);
            saveClients(clients);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("client_id", "=", "somevalue")))
                    .build();

            assertThat(getStsClientStore().findAll(spec)).isEmpty();
        }

        @Test
        void invalidOperator() {

            var stsClients = createClients(20);
            saveClients(stsClients);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(List.of(new Criterion("did", "sqrt", "foobar"))) //sqrt is invalid
                    .build();

            assertThatThrownBy(() -> getStsClientStore().findAll(spec)).isInstanceOf(IllegalArgumentException.class);
        }


        @Test
        void verifyPaging() {
            var stsClients = createClients(10);
            saveClients(stsClients);

            // page size fits
            assertThat(getStsClientStore().findAll(QuerySpec.Builder.newInstance().offset(4).limit(2).build())).hasSize(2);

            // page size larger than collection
            assertThat(getStsClientStore().findAll(QuerySpec.Builder.newInstance().offset(5).limit(100).build())).hasSize(5);
        }

        @Test
        void shouldReturnEmpty_whenQueryByInvalidKey() {
            var stsClients = createClients(5);
            saveClients(stsClients);

            var spec = QuerySpec.Builder.newInstance()
                    .filter(criterion("not-exist", "=", "some-value"))
                    .build();

            assertThat(getStsClientStore().findAll(spec)).isEmpty();
        }

        @Test
        void verifySorting() {

            var stsClients = IntStream.range(0, 10).mapToObj(idx -> {
                delay(10);
                return createClient("id" + idx);
            }).toList();

            saveClients(stsClients);


            assertThat(getStsClientStore().findAll(QuerySpec.Builder.newInstance().sortField("createdAt").sortOrder(SortOrder.ASC).build()))
                    .hasSize(10)
                    .isSortedAccordingTo(Comparator.comparing(StsClient::getCreatedAt));

            assertThat(getStsClientStore().findAll(QuerySpec.Builder.newInstance().sortField("createdAt").sortOrder(SortOrder.DESC).build()))
                    .hasSize(10)
                    .isSortedAccordingTo((c1, c2) -> Long.compare(c2.getCreatedAt(), c1.getCreatedAt()));
        }

        @Test
        void verifySorting_invalidProperty() {
            var stsClients = createClients(10);
            saveClients(stsClients);

            var query = QuerySpec.Builder.newInstance().sortField("not-exist").sortOrder(SortOrder.DESC).build();

            // must actually collect, otherwise the stream is not materialized
            assertThatThrownBy(() -> getStsClientStore().findAll(query).toList()).isInstanceOf(IllegalArgumentException.class);
        }

        static class FilterArgumentProvider implements ArgumentsProvider {
            @Override
            public Stream<? extends Arguments> provideArguments(ExtensionContext context) {

                return Stream.of(
                        Arguments.of("id", (Function<StsClient, String>) StsClient::getId),
                        Arguments.of("clientId", (Function<StsClient, String>) StsClient::getClientId),
                        Arguments.of("name", (Function<StsClient, String>) StsClient::getName),
                        Arguments.of("did", (Function<StsClient, String>) StsClient::getDid),
                        Arguments.of("secretAlias", (Function<StsClient, String>) StsClient::getSecretAlias),
                        Arguments.of("privateKeyAlias", (Function<StsClient, String>) StsClient::getPrivateKeyAlias),
                        Arguments.of("publicKeyReference", (Function<StsClient, String>) StsClient::getPublicKeyReference)
                );
            }
        }
    }

    @Nested
    class FindById {

        @Test
        @DisplayName("Find client by ID that exists")
        void whenPresent() {
            var client = createClient(getRandomId(), "alias", getRandomId());
            getStsClientStore().create(client);

            var clientFromDb = getStsClientStore().findById(client.getId()).getContent();

            assertThat(client).usingRecursiveComparison().isEqualTo(clientFromDb);
        }

        @Test
        @DisplayName("Find client by ID when not exists")
        void whenNonexistent() {
            assertThat(getStsClientStore().findById("nonexistent"))
                    .isFailed()
                    .extracting(StoreFailure::getReason)
                    .isEqualTo(StoreFailure.Reason.NOT_FOUND);
        }
    }

    @Nested
    class FindByClientId {

        @Test
        @DisplayName("Find client by Client ID that exists")
        void whenPresent() {
            var client = createClient(getRandomId(), "alias", getRandomId());
            getStsClientStore().create(client);

            var clientFromDb = getStsClientStore().findByClientId(client.getClientId()).getContent();

            assertThat(client).usingRecursiveComparison().isEqualTo(clientFromDb);
        }

        @Test
        @DisplayName("Find client by Client ID when not exists")
        void whenNonexistent() {
            assertThat(getStsClientStore().findByClientId("nonexistent"))
                    .isFailed()
                    .extracting(StoreFailure::getReason)
                    .isEqualTo(StoreFailure.Reason.NOT_FOUND);
        }
    }

    @Nested
    class Update {
        @Test
        @DisplayName("Update a non-existing Client")
        void doesNotExist_shouldNotCreate() {
            var client = createClient(getRandomId(), "alias", getRandomId());

            var result = getStsClientStore().update(client);

            assertThat(result.failed()).isTrue();
            assertThat(result.reason()).isEqualTo(NOT_FOUND);

            var existing = getStsClientStore().findAll(QuerySpec.max());

            assertThat(existing).hasSize(0);
        }

        @Test
        @DisplayName("Update an existing client")
        void exists() {
            var client1 = createClient("id", "alias", getRandomId());
            var client2 = createClientBuilder("id")
                    .clientId(client1.getClientId())
                    .name("nameChanged")
                    .privateKeyAlias("privateAliasChanged")
                    .publicKeyReference("publicRefChanged")
                    .did("didChanged")
                    .secretAlias("aliasChanged")
                    .build();


            getStsClientStore().create(client1);
            getStsClientStore().update(client2);

            var clients = getStsClientStore().findAll(QuerySpec.none()).collect(Collectors.toList());

            assertThat(clients).isNotNull().hasSize(1).first().satisfies(client -> {
                assertThat(client.getId()).isEqualTo(client2.getId());
                assertThat(client.getName()).isEqualTo(client2.getName());
                assertThat(client.getDid()).isEqualTo(client2.getDid());
                assertThat(client.getSecretAlias()).isEqualTo(client2.getSecretAlias());
                assertThat(client.getPrivateKeyAlias()).isEqualTo(client2.getPrivateKeyAlias());
                assertThat(client.getPublicKeyReference()).isEqualTo(client2.getPublicKeyReference());
                assertThat(client.getClientId()).isEqualTo(client2.getClientId());
            });
        }
    }

    @Nested
    class Delete {
        @Test
        void shouldDelete() {
            var client = createClient(getRandomId(), "alias", getRandomId());
            getStsClientStore().create(client);
            assertThat(getStsClientStore().findAll(QuerySpec.max())).hasSize(1);

            var deleted = getStsClientStore().deleteById(client.getId());

            assertThat(deleted.succeeded()).isTrue();
            assertThat(deleted.getContent()).isNotNull().usingRecursiveComparison().isEqualTo(client);
            assertThat(getStsClientStore().findAll(QuerySpec.max())).isEmpty();
        }

        @Test
        void shouldNotDelete_whenEntityDoesNotExist() {
            var deleted = getStsClientStore().deleteById("test-id1");

            assertThat(deleted).isFailed().extracting(StoreFailure::getReason).isEqualTo(NOT_FOUND);
        }
    }
}
