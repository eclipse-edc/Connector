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
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenRequest;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenResponse;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.ServiceAccountName;
import com.google.common.collect.ImmutableList;
import com.google.iam.admin.v1.CreateServiceAccountRequest;
import com.google.iam.admin.v1.ProjectName;
import com.google.iam.admin.v1.ServiceAccount;
import com.google.protobuf.Duration;
import org.eclipse.dataspaceconnector.gcp.lib.common.GcpExtensionException;
import org.eclipse.dataspaceconnector.gcp.lib.common.ServiceAccountWrapper;
import org.eclipse.dataspaceconnector.gcp.lib.storage.GcsAccessToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class IamServiceImpl implements IamService {

    public static final ImmutableList<String> OAUTH_SCOPE = ImmutableList.of("https://www.googleapis.com/auth/cloud-platform");
    private static final long ONE_HOUR_IN_S = TimeUnit.HOURS.toSeconds(1);
    private final IamClientFactory clientFactory;
    private final String gcpProjectId;
    private final Monitor monitor;

    public IamServiceImpl(IamClientFactory clientFactory, Monitor monitor, String gcpProjectId) {
        this.monitor = monitor;
        this.gcpProjectId = gcpProjectId;
        this.clientFactory = clientFactory;
    }

    @Override
    public ServiceAccountWrapper getOrCreateServiceAccount(String serviceAccountName) {
        ServiceAccount requestedServiceAccount = ServiceAccount.newBuilder().setDisplayName(serviceAccountName).build();
        CreateServiceAccountRequest request = CreateServiceAccountRequest.newBuilder()
                .setName(ProjectName.of(gcpProjectId).toString())
                .setAccountId(serviceAccountName)
                .setServiceAccount(requestedServiceAccount)
                .build();

        try (var client = clientFactory.createIamClient()) {
            ServiceAccount serviceAccount = client.createServiceAccount(request);
            monitor.info("Created service account: " + serviceAccount.getEmail());
            return new ServiceAccountWrapper(serviceAccount);
        } catch (ApiException e) {
            if (e.getStatusCode().getCode() == StatusCode.Code.ALREADY_EXISTS) {
                monitor.severe("Service account already existed: \n" + e);
                throw new GcpExtensionException("Not implemented");
            }
            monitor.severe("Unable to create service account: \n" + e);
            throw new GcpExtensionException(e);
        } catch (IOException e) {
            monitor.severe("Unable to create IAM Client: \n" + e);
            throw new GcpExtensionException(e);
        }
    }

    @Override
    public GcsAccessToken createAccessToken(ServiceAccountWrapper serviceAccount) {
        try (IamCredentialsClient iamCredentialsClient = clientFactory.createCredentialsClient()) {
            ServiceAccountName name = ServiceAccountName.of("-", serviceAccount.getEmail());
            Duration lifetime = Duration.newBuilder().setSeconds(ONE_HOUR_IN_S).build();
            GenerateAccessTokenRequest request = GenerateAccessTokenRequest.newBuilder()
                    .setName(name.toString())
                    .addAllScope(OAUTH_SCOPE)
                    .setLifetime(lifetime)
                    .build();
            GenerateAccessTokenResponse response = iamCredentialsClient.generateAccessToken(request);
            monitor.info("Created access token for " + serviceAccount.getEmail());
            long expirationMillis = response.getExpireTime().getSeconds() * 1000;
            return new GcsAccessToken(response.getAccessToken(), expirationMillis);
        } catch (Exception e) {
            throw new GcpExtensionException("Error creating service account token:\n" + e);
        }
    }

    @Override
    public void deleteServiceAccountIfExists(ServiceAccountWrapper serviceAccount) {
        try (var client = clientFactory.createIamClient()) {
            client.deleteServiceAccount(serviceAccount.getName());
            monitor.info("Deleted service account: " + serviceAccount.getEmail());
        } catch (ApiException e) {
            if (e.getStatusCode().getCode() == StatusCode.Code.NOT_FOUND) {
                monitor.severe("Service account Not Found: \n" + e);
                return;
            }
            monitor.severe("Unable to delete service account: \n" + e);
            throw new GcpExtensionException(e);
        } catch (IOException e) {
            monitor.severe("Unable to create IAM Client: \n" + e);
            throw new GcpExtensionException(e);
        }
    }
}