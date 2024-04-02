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

package org.eclipse.edc.iam.identitytrust.sts.spi.model;

import org.eclipse.edc.spi.security.Vault;

import java.util.Objects;

/**
 * The {@link StsClient} contains information about STS clients.
 */
public class StsClient {
    private String id;
    private String clientId;
    private String did;
    private String name;
    private String secretAlias;
    private String privateKeyAlias;
    private String publicKeyReference;

    private StsClient() {
    }

    /**
     * Unique identifier of the {@link  StsClient}
     *
     * @return The ID of the Client
     */
    public String getId() {
        return id;
    }

    /**
     * The alias of the {@link StsClient} secret stored in the {@link Vault}
     *
     * @return The secret alias
     */
    public String getSecretAlias() {
        return secretAlias;
    }

    /**
     * The alias of the {@link StsClient} private key stored in the {@link Vault}
     *
     * @return The private key alias
     */
    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }


    /**
     * The name of the {@link StsClient}
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * The client_id of the {@link StsClient}
     *
     * @return The clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * The client DID
     *
     * @return The DID
     */
    public String getDid() {
        return did;
    }

    /**
     * A reference, e.g. a URL, where the public key that corresponds to the {@link StsClient#getPrivateKeyAlias()} can be obtained.
     * In most situations this will be a DID with a key identifier, e.g. "did:web:foo:bar#key-1".
     * <p>
     * This can be null, in which case there has to be an out-of-band public key exchange (PKI), for example a well-known location.
     *
     * @return A reference to where the public key is available.
     */
    public String getPublicKeyReference() {
        return publicKeyReference;
    }


    public static class Builder {

        private final StsClient client;

        private Builder(StsClient client) {
            this.client = client;
        }

        public static Builder newInstance() {
            return new Builder(new StsClient());
        }


        public Builder id(String id) {
            client.id = id;
            return this;
        }

        public Builder clientId(String clientId) {
            client.clientId = clientId;
            return this;
        }

        public Builder name(String name) {
            client.name = name;
            return this;
        }

        public Builder did(String did) {
            client.did = did;
            return this;
        }

        public Builder secretAlias(String secretAlias) {
            client.secretAlias = secretAlias;
            return this;
        }

        public Builder privateKeyAlias(String privateKeyAlias) {
            client.privateKeyAlias = privateKeyAlias;
            return this;
        }

        public Builder publicKeyReference(String publicKeyReference) {
            client.publicKeyReference = publicKeyReference;
            return this;
        }

        public StsClient build() {
            Objects.requireNonNull(client.id, "Client id");
            Objects.requireNonNull(client.clientId, "Client client_id");
            Objects.requireNonNull(client.name, "Client name");
            Objects.requireNonNull(client.did, "Client DID");
            Objects.requireNonNull(client.secretAlias, "Client secret alias");
            Objects.requireNonNull(client.privateKeyAlias, "Client private key alias");
            return client;
        }

    }
}
