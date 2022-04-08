/*
 *  Copyright (c) 2022 ZF Friedrichshafen AG
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       ZF Friedrichshafen AG - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.api.auth;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.security.Vault;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BasicAuthenticationService implements AuthenticationService {

    private static final String BASIC_AUTH_HEADER_NAME = "Authorization";
    private final Base64.Decoder b64Decoder;
    private final Vault vault;
    private final List<BasicAuthenticationExtension.ConfigCredentials> basicAuthUsersWithVaultKeyConfigs;
    private final Monitor monitor;

    public BasicAuthenticationService(
            Vault vault,
            List<BasicAuthenticationExtension.ConfigCredentials> basicAuthUsersWithVaultKeyConfigs,
            Monitor monitor) {
        this.vault = vault;
        this.basicAuthUsersWithVaultKeyConfigs = basicAuthUsersWithVaultKeyConfigs;
        this.monitor = monitor;
        b64Decoder = Base64.getDecoder();
    }

    /**
     * Validates if the request is authenticated
     *
     * @param headers The headers, that contains the credential to be used, in this case the Basic-Auth credentials.
     * @return True if the credentials are correct.
     */
    @Override
    public boolean isAuthenticated(Map<String, List<String>> headers) {
        Objects.requireNonNull(headers, "headers");

        return headers.keySet().stream()
                .filter(k -> k.equalsIgnoreCase(BASIC_AUTH_HEADER_NAME))
                .map(headers::get)
                .filter(list -> !list.isEmpty())
                .anyMatch(list -> list.stream()
                        .map(this::decodeAuthHeader)
                        .anyMatch(this::checkBasicAuthValid));
    }

    /**
     * Decodes the base64 request header.
     *
     * @param authHeader Base64 encoded credentials from the request header
     * @return Array with the encoded credentials. First is the username and the second the password. If there was a
     *     problem an array with 0 entries will be returned.
     */
    private Result<BasicAuthCredentials> decodeAuthHeader(String authHeader) {
        String[] authCredentials;
        var separatedAuthHeader = authHeader.split(" ");

        if (separatedAuthHeader.length != 2) {
            return Result.failure("Authorization header value is not a valid Bearer token");
        }

        try {
            authCredentials = new String(b64Decoder.decode(separatedAuthHeader[1])).split(":");
        } catch (IllegalArgumentException ex) {
            return Result.failure("Authorization header could no base64 decoded");
        }

        if (authCredentials.length != 2) {
            return Result.failure("Authorization header could be base64 decoded but is not in format of 'username:password'");
        }

        return Result.success(new BasicAuthCredentials(authCredentials[0], authCredentials[1]));
    }

    /**
     * Checks if the provided credentials are in the internal registered once and if the password is correct.
     *
     * @param authCredentials {@link org.eclipse.dataspaceconnector.api.auth.BasicAuthenticationService.BasicAuthCredentials}
     *                        used in the request.
     * @return True if credentials are correct
     */
    private boolean checkBasicAuthValid(Result<BasicAuthCredentials> authCredentials) {
        if (authCredentials.failed()) {
            authCredentials.getFailureMessages().forEach(monitor::debug);
            return false;
        }

        var creds = authCredentials.getContent();

        return basicAuthUsersWithVaultKeyConfigs.stream()
                .anyMatch(it -> it.getUsername().equals(creds.username) && Objects.equals(vault.resolveSecret(it.getVaultKey()), creds.password));
    }

    static class BasicAuthCredentials {
        String username;
        String password;

        public BasicAuthCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
