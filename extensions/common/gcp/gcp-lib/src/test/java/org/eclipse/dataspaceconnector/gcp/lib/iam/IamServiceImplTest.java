/*
 *  Copyright (c) 2022 Google LLC
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Google LCC - Initial implementation
 *
 */

package org.eclipse.dataspaceconnector.gcp.lib.iam;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.ApiExceptionFactory;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.iam.admin.v1.IAMClient;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenRequest;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenResponse;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.iam.admin.v1.CreateServiceAccountRequest;
import com.google.iam.admin.v1.ServiceAccount;
import org.eclipse.dataspaceconnector.gcp.lib.common.GcpExtensionException;
import org.eclipse.dataspaceconnector.gcp.lib.common.ServiceAccountWrapper;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.Answers.RETURNS_SMART_NULLS;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IamServiceImplTest {

    private final String projectId = "test-project-Id";
    private final String serviceAccountName = "test-service-account";
    private final String serviceAccountEmail = String.format("%s@%s.iam.gserviceaccount.com", serviceAccountName, projectId);
    private Monitor monitor;
    private IAMClient clientMock;
    private IamService iamApi;
    private IamClientFactory clientFactory;
    private ServiceAccountWrapper testServiceAccount;

    private static ApiException apiExceptionWithStatusCode(StatusCode.Code code) {
        return ApiExceptionFactory.createException(
                new Exception(), new StatusCode() {
                    @Override
                    public Code getCode() {
                        return code;
                    }

                    @Override
                    public Object getTransportCode() {
                        return null;
                    }
                }, false);
    }

    @BeforeEach
    void setUp() {
        monitor = Mockito.mock(Monitor.class);
        clientFactory = Mockito.mock(IamClientFactory.class, RETURNS_SMART_NULLS);
        testServiceAccount = new ServiceAccountWrapper(serviceAccountEmail, serviceAccountName);
        iamApi = new IamServiceImpl(clientFactory, monitor, projectId);
    }

    @Test
    void testCreateServiceAccount() throws IOException {
        clientMock = Mockito.mock(IAMClient.class, RETURNS_SMART_NULLS);
        given(clientFactory.createIamClient()).willReturn(clientMock);
        ServiceAccount serviceAccount = ServiceAccount.newBuilder().setEmail(serviceAccountEmail).build();
        given(clientMock.createServiceAccount(any(CreateServiceAccountRequest.class))).willReturn(serviceAccount);

        ServiceAccountWrapper createdServiceAccount = iamApi.getOrCreateServiceAccount(serviceAccountName);
        assertThat(createdServiceAccount.getEmail()).isEqualTo(serviceAccountEmail);
    }

    @Test
    void testCreateServiceAccountClientCreationFails() throws IOException {
        when(clientFactory.createIamClient()).thenThrow(IOException.class);

        assertThatThrownBy(() -> iamApi.getOrCreateServiceAccount(serviceAccountName))
                .isInstanceOf(GcpExtensionException.class);
    }

    @Test
    void testCreateAccessToken() throws IOException {
        var clientMock = Mockito.mock(IamCredentialsClient.class);
        given(clientFactory.createCredentialsClient()).willReturn(clientMock);
        String expectedTokenString = "test-access-token";
        GenerateAccessTokenResponse expectedKey = GenerateAccessTokenResponse.newBuilder().setAccessToken(expectedTokenString).build();
        GenerateAccessTokenRequest expectedRequest = and(
                argThat(x -> x.getName().equals("projects/-/serviceAccounts/" + serviceAccountEmail)),
                argThat(x -> x.getLifetime().getSeconds() == 3600)
        );
        given(clientMock.generateAccessToken(expectedRequest)).willReturn(expectedKey);
        assertThat(iamApi.createAccessToken(testServiceAccount).getToken()).isEqualTo(expectedTokenString);
    }

    @Test
    void testCreateAccessTokenClientCreationFails() throws IOException {
        given(clientFactory.createCredentialsClient()).willThrow(IOException.class);

        assertThatThrownBy(() -> iamApi.createAccessToken(testServiceAccount))
                .isInstanceOf(GcpExtensionException.class);
    }


    @Test
    void testDeleteServiceAccount() throws IOException {
        clientMock = Mockito.mock(IAMClient.class, RETURNS_SMART_NULLS);
        given(clientFactory.createIamClient()).willReturn(clientMock);

        var serviceAccount = new ServiceAccountWrapper(serviceAccountEmail, serviceAccountName);
        doNothing().when(clientMock).deleteServiceAccount(serviceAccountName);

        iamApi.deleteServiceAccountIfExists(serviceAccount);

        verify(clientMock, times(1)).deleteServiceAccount(serviceAccount.getName());
    }

    @Test
    void testDeleteServiceAccountThatAlreadyExistsSucceeds() throws IOException {
        clientMock = Mockito.mock(IAMClient.class, RETURNS_SMART_NULLS);
        given(clientFactory.createIamClient()).willReturn(clientMock);

        ApiException exception = apiExceptionWithStatusCode(StatusCode.Code.NOT_FOUND);
        doThrow(exception)
                .when(clientMock).deleteServiceAccount(serviceAccountName);

        iamApi.deleteServiceAccountIfExists(testServiceAccount);
        verify(clientMock, times(1)).deleteServiceAccount(serviceAccountName);
    }

    @Test
    void testDeleteServiceAccountFails() throws IOException {
        clientMock = Mockito.mock(IAMClient.class, RETURNS_SMART_NULLS);
        given(clientFactory.createIamClient()).willReturn(clientMock);

        ApiException exception = apiExceptionWithStatusCode(StatusCode.Code.INTERNAL);
        doThrow(exception)
                .when(clientMock).deleteServiceAccount(serviceAccountName);

        assertThatThrownBy(() -> iamApi.deleteServiceAccountIfExists(testServiceAccount))
                .isInstanceOf(GcpExtensionException.class);
    }

    @Test
    void testDeleteServiceAccountClientCreationFails() throws IOException {
        given(clientFactory.createIamClient()).willThrow(IOException.class);

        assertThatThrownBy(() -> iamApi.deleteServiceAccountIfExists(testServiceAccount))
                .isInstanceOf(GcpExtensionException.class);
    }
}