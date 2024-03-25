/*
 *  Copyright (c) 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.provision.http.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Interceptor;
import org.eclipse.edc.connector.provision.http.config.ProvisionerConfiguration;
import org.eclipse.edc.connector.transfer.spi.types.ProvisionedResource;
import org.eclipse.edc.connector.transfer.spi.types.ResourceDefinition;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.connector.provision.http.HttpProvisionerFixtures.createResponse;
import static org.eclipse.edc.connector.provision.http.config.ProvisionerConfiguration.ProvisionerType.PROVIDER;
import static org.eclipse.http.client.testfixtures.HttpTestUtils.testHttpClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpProviderProvisionerTest {
    private HttpProviderProvisioner provisioner;
    private Interceptor delegate;

    @BeforeEach
    void setUp() throws MalformedURLException {
        var configuration = ProvisionerConfiguration.Builder.newInstance()
                .name("test")
                .provisionerType(PROVIDER)
                .dataAddressType("test")
                .policyScope("test")
                .endpoint(new URL("http://bar.com"))
                .build();

        delegate = mock(Interceptor.class);
        provisioner = new HttpProviderProvisioner(configuration, new URL("http://foo.com"), mock(PolicyEngine.class),
                testHttpClient(delegate), new ObjectMapper(), mock(Monitor.class));
    }

    @Test
    void verifyCanProvision() {
        assertThat(provisioner.canProvision(createResourceDefinition())).isTrue();
        assertThat(provisioner.canProvision(new TestResourceDefinition())).isFalse();

        var differentType = HttpProviderResourceDefinition.Builder.newInstance()
                .assetId("1")
                .transferProcessId("2")
                .dataAddressType("another-type")
                .id("3")
                .build();
        assertThat(provisioner.canProvision(differentType)).isFalse();
    }

    @Test
    void verifyCanDeprovision() {
        assertThat(provisioner.canDeprovision(createProvisionedResource())).isTrue();
        assertThat(provisioner.canDeprovision(new TestProvisionedResource())).isFalse();

        var dataAddress = DataAddress.Builder.newInstance().type("another-type").build();
        var differentType = HttpProvisionedContentResource.Builder.newInstance()
                .assetId("1")
                .transferProcessId("2")
                .resourceName("test")
                .dataAddress(dataAddress)
                .resourceDefinitionId("3")
                .id("3")
                .build();

        assertThat(provisioner.canDeprovision(differentType)).isFalse();
    }

    @Test
    void verifyProvisionOkAndInProcess() throws Exception {
        when(delegate.intercept(any())).thenAnswer((invocation -> createResponse(200, invocation)));

        var policy = Policy.Builder.newInstance().build();

        var definition = createResourceDefinition();

        var result = provisioner.provision(definition, policy).get();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().isInProcess()).isTrue();
    }

    @Test
    void verifyProvision404Response() throws Exception {
        when(delegate.intercept(any())).thenAnswer((invocation -> createResponse(404, invocation)));

        var policy = Policy.Builder.newInstance().build();

        var definition = createResourceDefinition();

        var result = provisioner.provision(definition, policy).get();

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
    }

    @Test
    void verifyProvisionRetryResponse() throws Exception {
        when(delegate.intercept(any())).thenAnswer((invocation -> createResponse(503, invocation)));

        var policy = Policy.Builder.newInstance().build();

        var definition = createResourceDefinition();

        var result = provisioner.provision(definition, policy).get();

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.ERROR_RETRY);
    }

    @Test
    void verifyDeprovisionOkAndInProcess() throws Exception {
        when(delegate.intercept(any())).thenAnswer((invocation -> createResponse(200, invocation)));

        var policy = Policy.Builder.newInstance().build();

        var definition = createProvisionedResource();

        var result = provisioner.deprovision(definition, policy).get();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent().isInProcess()).isTrue();
    }


    @Test
    void verifyDeprovision404Response() throws Exception {
        when(delegate.intercept(any())).thenAnswer((invocation -> createResponse(404, invocation)));

        var policy = Policy.Builder.newInstance().build();

        var definition = createProvisionedResource();

        var result = provisioner.deprovision(definition, policy).get();

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.FATAL_ERROR);
    }


    @Test
    void verifyDeprovisionRetryResponse() throws Exception {
        when(delegate.intercept(any())).thenAnswer((invocation -> createResponse(503, invocation)));

        var policy = Policy.Builder.newInstance().build();

        var definition = createProvisionedResource();

        var result = provisioner.deprovision(definition, policy).get();

        assertThat(result.failed()).isTrue();
        assertThat(result.getFailure().status()).isEqualTo(ResponseStatus.ERROR_RETRY);
    }


    private HttpProviderResourceDefinition createResourceDefinition() {
        return HttpProviderResourceDefinition.Builder.newInstance()
                .assetId("1")
                .transferProcessId("2")
                .dataAddressType("test")
                .id("3")
                .build();
    }

    private HttpProvisionedContentResource createProvisionedResource() {
        var dataAddress = DataAddress.Builder.newInstance().type("test").build();

        return HttpProvisionedContentResource.Builder.newInstance()
                .assetId("1")
                .transferProcessId("2")
                .resourceName("test")
                .dataAddress(dataAddress)
                .resourceDefinitionId("3")
                .id("3")
                .build();
    }


    private static class TestResourceDefinition extends ResourceDefinition {
        @Override
        public <RD extends ResourceDefinition, B extends Builder<RD, B>> B toBuilder() {
            return null;
        }
    }

    private static class TestProvisionedResource extends ProvisionedResource {

    }

}
