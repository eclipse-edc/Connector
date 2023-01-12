/*
 *  Copyright (c) 2022 T-Systems International GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       T-Systems International GmbH
 *
 */

package org.eclipse.edc.gcp.common;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.TypeManager;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

public class GcpCredentials {

    public enum GcpCredentialType {
        DEFAULT_APPLICATION, GOOGLE_ACCESS_TOKEN, GOOGLE_SERVICE_ACCOUNT_KEY_FILE
    }

    private final Base64.Decoder b64Decoder;
    private final Vault vault;
    private final TypeManager typeManager;
    private final Monitor monitor;

    public GcpCredentials(Vault vault, TypeManager typeManager, Monitor monitor) {
        this.vault = vault;
        this.typeManager = typeManager;
        this.b64Decoder = Base64.getDecoder();
        this.monitor = monitor;
    }

    /**
     * Returns the Google Credentials which will be created based on the following order:
     * if none of the  parameters were provided then Google Credentials will be retrieved from ApplicationDefaultCredentials
     *    Otherwise it will be retrieved from a token or a Google Credentials file
     *
     * @param vaultTokenKeyName Key name of an entry in the vault containing an access token.
     * @param vaultServiceAccountKeyName key name of an entry in the vault containing a valid Google Credentials file in json format.
     * @param serviceAccountValue Content of a valid Google Credentials file in json format encoded with base64 .
     *
     * @return GoogleCredentials
     */
    public GoogleCredentials resolveGoogleCredentialsFromDataAddress(@Nullable String vaultTokenKeyName,
                                                                     @Nullable String vaultServiceAccountKeyName,
                                                                     @Nullable String serviceAccountValue) {
        if (vaultTokenKeyName != null && !vaultTokenKeyName.isEmpty()) {
            var tokenContent = vault.resolveSecret(vaultTokenKeyName);
            return createGoogleCredential(tokenContent, GcpCredentialType.GOOGLE_ACCESS_TOKEN);
        } else if (vaultServiceAccountKeyName != null && !vaultServiceAccountKeyName.isEmpty()) {
            return createGoogleCredential(vault.resolveSecret(vaultServiceAccountKeyName), GcpCredentialType.GOOGLE_SERVICE_ACCOUNT_KEY_FILE);
        } else if (serviceAccountValue != null && !serviceAccountValue.isEmpty()) {
            try {
                var serviceKeyContent = new String(b64Decoder.decode(serviceAccountValue));
                if (!serviceKeyContent.contains("service_account")) {
                    throw new GcpException("SERVICE_ACCOUNT_VALUE is not provided as a valid service account key file.");
                }
                return createGoogleCredential(serviceKeyContent, GcpCredentialType.GOOGLE_SERVICE_ACCOUNT_KEY_FILE);
            } catch (IllegalArgumentException ex) {
                throw new GcpException("SERVICE_ACCOUNT_VALUE is not provided in a valid base64 format.");
            }
        } else {
            return creatApplicationDefaultCredentials();
        }
    }

    /**
     * Returns the Google Credentials which will created based on the Application Default Credentials in the following approaches
     * - Credentials file pointed to by the GOOGLE_APPLICATION_CREDENTIALS environment variable
     * - Credentials provided by the Google Cloud SDK gcloud auth application-default login command
     * - Google App Engine built-in credentials
     * - Google Cloud Shell built-in credentials
     * - Google Compute Engine built-in credentials
     *
     * @return GoogleCredentials
     */
    public GoogleCredentials creatApplicationDefaultCredentials() {
        return createGoogleCredential("", GcpCredentialType.DEFAULT_APPLICATION);
    }


    public GoogleCredentials createGoogleCredential(String keyContent, GcpCredentialType gcpCredentialType) {
        GoogleCredentials googleCredentials;

        if (gcpCredentialType.equals(GcpCredentialType.GOOGLE_ACCESS_TOKEN)) {
            try {
                var gcpAccessToken = typeManager.readValue(keyContent, GcpAccessToken.class);
                monitor.info("Gcp: The provided token will be used to resolve the google credentials.");
                googleCredentials = GoogleCredentials.create(
                        new AccessToken(gcpAccessToken.getToken(),
                                new Date(gcpAccessToken.getExpiration())));
            } catch (EdcException ex) {
                throw new GcpException("ACCESS_TOKEN is not in a valid GcpAccessToken format.");
            } catch (Exception e) {
                throw new GcpException("Error while getting the default credentials.", e);
            }
        } else if (gcpCredentialType.equals(GcpCredentialType.GOOGLE_SERVICE_ACCOUNT_KEY_FILE)) {
            try {
                monitor.debug("Gcp: The provided credentials file will be used to resolve the google credentials.");
                googleCredentials = GoogleCredentials.fromStream(new ByteArrayInputStream(keyContent.getBytes(StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new GcpException("Error while getting the credentials from the credentials file.", e);
            }

        } else {
            try {
                monitor.debug("Gcp: The default Credentials will be used to resolve the google credentials.");
                googleCredentials = GoogleCredentials.getApplicationDefault();
            } catch (IOException e) {
                throw new GcpException("Error while getting the default credentials.", e);
            }
        }
        return googleCredentials;
    }
}