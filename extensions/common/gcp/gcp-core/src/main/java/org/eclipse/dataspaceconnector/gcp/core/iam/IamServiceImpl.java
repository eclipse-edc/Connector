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

package org.eclipse.dataspaceconnector.gcp.core.iam;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.iam.admin.v1.IAMClient;
import com.google.cloud.iam.credentials.v1.GenerateAccessTokenRequest;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.cloud.iam.credentials.v1.ServiceAccountName;
import com.google.common.collect.ImmutableList;
import com.google.iam.admin.v1.CreateServiceAccountRequest;
import com.google.iam.admin.v1.ProjectName;
import com.google.iam.admin.v1.ServiceAccount;
import com.google.protobuf.Duration;
import org.eclipse.dataspaceconnector.gcp.core.common.GcpException;
import org.eclipse.dataspaceconnector.gcp.core.common.ServiceAccountWrapper;
import org.eclipse.dataspaceconnector.gcp.core.storage.GcsAccessToken;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class IamServiceImpl implements IamService {

    public static final ImmutableList<String> OAUTH_SCOPE = ImmutableList.of("https://www.googleapis.com/auth/cloud-platform");
    private static final long ONE_HOUR_IN_S = TimeUnit.HOURS.toSeconds(1);
    private final String gcpProjectId;
    private final Supplier<IAMClient> iamClientSupplier;
    private final Supplier<IamCredentialsClient> iamCredentialsClientSupplier;
    private final Monitor monitor;

    private IamServiceImpl(Monitor monitor,
                           String gcpProjectId,
                           Supplier<IAMClient> iamClientSupplier,
                           Supplier<IamCredentialsClient> iamCredentialsClientSupplier
    ) {
        this.monitor = monitor;
        this.gcpProjectId = gcpProjectId;
        this.iamClientSupplier = iamClientSupplier;
        this.iamCredentialsClientSupplier = iamCredentialsClientSupplier;
    }

    @Override
    public ServiceAccountWrapper getOrCreateServiceAccount(String serviceAccountName, String serviceAccountDescription) {
        var requestedServiceAccount = ServiceAccount.newBuilder()
                .setDisplayName(serviceAccountName)
                .setDescription(serviceAccountDescription)
                .build();
        var request = CreateServiceAccountRequest.newBuilder()
                .setName(ProjectName.of(gcpProjectId).toString())
                .setAccountId(serviceAccountName)
                .setServiceAccount(requestedServiceAccount)
                .build();

        try (var client = iamClientSupplier.get()) {
            var serviceAccount = client.createServiceAccount(request);
            monitor.debug("Created service account: " + serviceAccount.getEmail());
            return new ServiceAccountWrapper(serviceAccount.getEmail(), serviceAccount.getName(), serviceAccountDescription);
        } catch (ApiException e) {
            if (e.getStatusCode().getCode() == StatusCode.Code.ALREADY_EXISTS) {
                return getServiceAccount(serviceAccountName, serviceAccountDescription);
            }
            monitor.severe("Unable to create service account", e);
            throw new GcpException("Unable to create service account", e);
        }
    }

    private ServiceAccountWrapper getServiceAccount(String serviceAccountName, String serviceAccountDescription) {
        try (var client = iamClientSupplier.get()) {
            var serviceAccountEmail = getServiceAccountEmail(serviceAccountName, gcpProjectId);
            var name = ServiceAccountName.of(gcpProjectId, serviceAccountEmail).toString();
            var response = client.getServiceAccount(name);
            if (!response.getDescription().equals(serviceAccountDescription)) {
                String errorMessage = "A service account with the same name but different description existed already. Please ensure a unique name is used for every transfer process";
                monitor.severe(errorMessage);
                throw new GcpException(errorMessage);
            }
            return new ServiceAccountWrapper(response.getEmail(), response.getName(), response.getDescription());
        }
    }

    @Override
    public GcsAccessToken createAccessToken(ServiceAccountWrapper serviceAccount) {
        try (var iamCredentialsClient = iamCredentialsClientSupplier.get()) {
            var name = ServiceAccountName.of("-", serviceAccount.getEmail());
            var lifetime = Duration.newBuilder().setSeconds(ONE_HOUR_IN_S).build();
            var request = GenerateAccessTokenRequest.newBuilder()
                    .setName(name.toString())
                    .addAllScope(OAUTH_SCOPE)
                    .setLifetime(lifetime)
                    .build();
            var response = iamCredentialsClient.generateAccessToken(request);
            monitor.debug("Created access token for " + serviceAccount.getEmail());
            var expirationMillis = response.getExpireTime().getSeconds() * 1000;
            return new GcsAccessToken(response.getAccessToken(), expirationMillis);
        } catch (Exception e) {
            throw new GcpException("Error creating service account token:\n" + e);
        }
    }

    @Override
    public void deleteServiceAccountIfExists(ServiceAccountWrapper serviceAccount) {
        try (var client = iamClientSupplier.get()) {
            client.deleteServiceAccount(serviceAccount.getName());
            monitor.debug("Deleted service account: " + serviceAccount.getEmail());
        } catch (ApiException e) {
            if (e.getStatusCode().getCode() == StatusCode.Code.NOT_FOUND) {
                monitor.severe("Service account not found", e);
                return;
            }
            monitor.severe("Unable to delete service account", e);
            throw new GcpException(e);
        }
    }

    private String getServiceAccountEmail(String name, String project) {
        return String.format("%s@%s.iam.gserviceaccount.com", name, project);
    }

    public static class Builder {
        private final String gcpProjectId;
        private final Monitor monitor;
        private Supplier<IAMClient> iamClientSupplier;
        private Supplier<IamCredentialsClient> iamCredentialsClientSupplier;

        private Builder(Monitor monitor, String gcpProjectId) {
            this.gcpProjectId = gcpProjectId;
            this.monitor = monitor;
        }

        public static IamServiceImpl.Builder newInstance(Monitor monitor, String gcpProjectId) {
            return new Builder(monitor, gcpProjectId);
        }

        public Builder iamClientSupplier(Supplier<IAMClient> iamClientSupplier) {
            this.iamClientSupplier = iamClientSupplier;
            return this;
        }

        public Builder iamCredentialsClientSupplier(Supplier<IamCredentialsClient> iamCredentialsClientSupplier) {
            this.iamCredentialsClientSupplier = iamCredentialsClientSupplier;
            return this;
        }

        public IamServiceImpl build() {
            Objects.requireNonNull(gcpProjectId, "gcpProjectId");
            Objects.requireNonNull(monitor, "monitor");
            if (iamClientSupplier == null) {
                iamClientSupplier = defaultIamClientSupplier();
            }
            if (iamCredentialsClientSupplier == null) {
                iamCredentialsClientSupplier = defaultIamCredentialsClientSupplier();
            }
            return new IamServiceImpl(monitor, gcpProjectId, iamClientSupplier, iamCredentialsClientSupplier);
        }

        /**
         * Supplier of {@link IAMClient} using application default credentials
         */
        private Supplier<IAMClient> defaultIamClientSupplier() {
            return () -> {
                try {
                    return IAMClient.create();
                } catch (IOException e) {
                    throw new GcpException("Error while creating IAMClient", e);
                }
            };
        }

        /**
         * Supplier of {@link IamCredentialsClient} using application default credentials
         */
        private Supplier<IamCredentialsClient> defaultIamCredentialsClientSupplier() {
            return () -> {
                try {
                    return IamCredentialsClient.create();
                } catch (IOException e) {
                    throw new GcpException("Error while creating IamCredentialsClient", e);
                }
            };
        }
    }
}
