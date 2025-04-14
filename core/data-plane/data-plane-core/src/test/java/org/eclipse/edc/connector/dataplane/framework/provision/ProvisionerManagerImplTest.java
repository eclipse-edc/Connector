/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.connector.dataplane.framework.provision;

import org.eclipse.edc.connector.dataplane.spi.provision.DeprovisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Deprovisioner;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionResourceDefinition;
import org.eclipse.edc.connector.dataplane.spi.provision.ProvisionedResource;
import org.eclipse.edc.connector.dataplane.spi.provision.Provisioner;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ProvisionerManagerImplTest {

    private final Monitor monitor = mock();
    private final ProvisionerManagerImpl provisionerManager = new ProvisionerManagerImpl(monitor);

    @Nested
    class Provision {

        @Test
        void shouldInvokeFirstSupportedProvisioners() {
            var firstResource = new ProvisionedResource();
            var secondResource = new ProvisionedResource();
            provisionerManager.register(new TestProvisioner("test-type", completedFuture(StatusResult.success(firstResource))));
            provisionerManager.register(new TestProvisioner("test-type", completedFuture(StatusResult.success(secondResource))));
            var definition = ProvisionResourceDefinition.Builder.newInstance().type("test-type").build();

            var result = provisionerManager.provision(List.of(definition));

            assertThat(result).succeedsWithin(1, SECONDS).asInstanceOf(list(StatusResult.class))
                    .hasSize(1).first().satisfies(StatusResult::succeeded)
                    .extracting(StatusResult::getContent).isSameAs(firstResource);
        }

        @Test
        void shouldFail_whenNoProvisionerAvailable() {
            var resource = new ProvisionedResource();
            provisionerManager.register(new TestProvisioner("test-type", completedFuture(StatusResult.success(resource))));
            var definition = ProvisionResourceDefinition.Builder.newInstance().type("another-type").build();

            var result = provisionerManager.provision(List.of(definition));

            assertThat(result).failsWithin(1, SECONDS);
            verify(monitor).severe(contains("Error provisioning"));
        }

        @Test
        void shouldFail_whenAtLeastOneProvisionerFails() {
            provisionerManager.register(new TestProvisioner("test-type", failedFuture(new EdcException("error"))));
            provisionerManager.register(new TestProvisioner("test-type", completedFuture(StatusResult.success(new ProvisionedResource()))));
            var definition = ProvisionResourceDefinition.Builder.newInstance().type("test-type").build();

            var result = provisionerManager.provision(List.of(definition));

            assertThat(result).failsWithin(1, SECONDS);
        }

        private record TestProvisioner(String supportedType, CompletableFuture<StatusResult<ProvisionedResource>> result)
                implements Provisioner {

            @Override
            public CompletableFuture<StatusResult<ProvisionedResource>> provision(ProvisionResourceDefinition provisionResourceDefinition) {
                return result;
            }
        }
    }

    @Nested
    class Deprovision {

        @Test
        void shouldInvokeFirstSupportedDeprovisioners() {
            var firstResource = new DeprovisionedResource();
            var secondResource = new DeprovisionedResource();
            provisionerManager.register(new TestDeprovisioner("test-type", completedFuture(StatusResult.success(firstResource))));
            provisionerManager.register(new TestDeprovisioner("test-type", completedFuture(StatusResult.success(secondResource))));
            var definition = ProvisionResourceDefinition.Builder.newInstance().type("test-type").build();

            var result = provisionerManager.deprovision(List.of(definition));

            assertThat(result).succeedsWithin(1, SECONDS).asInstanceOf(list(StatusResult.class))
                    .hasSize(1).first().satisfies(StatusResult::succeeded)
                    .extracting(StatusResult::getContent).isSameAs(firstResource);
        }

        @Test
        void shouldFail_whenNoDeprovisionerAvailable() {
            var resource = new DeprovisionedResource();
            provisionerManager.register(new TestDeprovisioner("test-type", completedFuture(StatusResult.success(resource))));
            var definition = ProvisionResourceDefinition.Builder.newInstance().type("another-type").build();

            var result = provisionerManager.deprovision(List.of(definition));

            assertThat(result).failsWithin(1, SECONDS);
            verify(monitor).severe(contains("Error provisioning"));
        }

        @Test
        void shouldFail_whenAtLeastOneDeprovisionerFails() {
            provisionerManager.register(new TestDeprovisioner("test-type", failedFuture(new EdcException("error"))));
            provisionerManager.register(new TestDeprovisioner("test-type", completedFuture(StatusResult.success(new DeprovisionedResource()))));
            var definition = ProvisionResourceDefinition.Builder.newInstance().type("test-type").build();

            var result = provisionerManager.deprovision(List.of(definition));

            assertThat(result).failsWithin(1, SECONDS);
        }

        private record TestDeprovisioner(String supportedType, CompletableFuture<StatusResult<DeprovisionedResource>> result)
                implements Deprovisioner {

            @Override
            public CompletableFuture<StatusResult<DeprovisionedResource>> deprovision(ProvisionResourceDefinition provisionResourceDefinition) {
                return result;
            }
        }
    }

}
