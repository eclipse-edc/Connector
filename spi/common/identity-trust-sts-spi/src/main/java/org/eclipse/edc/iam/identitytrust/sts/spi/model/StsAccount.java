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

import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.security.Vault;

import java.util.Objects;

/**
 * The {@link StsAccount} contains information about STS clients.
 */
public class StsAccount extends Entity {
    private String clientId;
    private String did;
    private String name;
    private String secretAlias;
    private String privateKeyAlias;
    private String publicKeyReference;

    private StsAccount() {
    }

    /**
     * The alias of the {@link StsAccount} secret stored in the {@link Vault}
     *
     * @return The secret alias
     */
    public String getSecretAlias() {
        return secretAlias;
    }

    /**
     * The alias of the {@link StsAccount} private key stored in the {@link Vault}
     *
     * @return The private key alias
     */
    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }


    /**
     * The name of the {@link StsAccount}
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * The client_id of the {@link StsAccount}
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
     * A reference, e.g. a URL, where the public key that corresponds to the {@link StsAccount#getPrivateKeyAlias()} can be obtained.
     * In most situations this will be a DID with a key identifier, e.g. "did:web:foo:bar#key-1".
     * <p>
     * This can be null, in which case there has to be an out-of-band public key exchange (PKI), for example a well-known location.
     *
     * @return A reference to where the public key is available.
     */
    public String getPublicKeyReference() {
        return publicKeyReference;
    }


    public static class Builder extends Entity.Builder<StsAccount, Builder> {


        private Builder() {
            super(new StsAccount());
        }

        public static Builder newInstance() {
            return new Builder();
        }


        public Builder id(String id) {
            entity.id = id;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        @Override
        public StsAccount build() {
            Objects.requireNonNull(entity.id, "Client id");
            Objects.requireNonNull(entity.clientId, "Client client_id");
            Objects.requireNonNull(entity.name, "Client name");
            Objects.requireNonNull(entity.did, "Client DID");
            Objects.requireNonNull(entity.secretAlias, "Client secret alias");
            Objects.requireNonNull(entity.privateKeyAlias, "Client private key alias");
            Objects.requireNonNull(entity.publicKeyReference, "Client public key reference");
            return super.build();
        }

        public Builder clientId(String clientId) {
            entity.clientId = clientId;
            return this;
        }

        public Builder name(String name) {
            entity.name = name;
            return this;
        }

        public Builder did(String did) {
            entity.did = did;
            return this;
        }

        public Builder secretAlias(String secretAlias) {
            entity.secretAlias = secretAlias;
            return this;
        }

        public Builder privateKeyAlias(String privateKeyAlias) {
            entity.privateKeyAlias = privateKeyAlias;
            return this;
        }

        public Builder publicKeyReference(String publicKeyReference) {
            entity.publicKeyReference = publicKeyReference;
            return this;
        }

    }
}
