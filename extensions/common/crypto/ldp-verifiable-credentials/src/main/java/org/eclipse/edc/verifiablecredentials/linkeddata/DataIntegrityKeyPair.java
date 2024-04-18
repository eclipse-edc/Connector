/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

package org.eclipse.edc.verifiablecredentials.linkeddata;


import com.apicatalog.ld.signature.VerificationMethod;

import java.net.URI;
import java.util.Objects;

class DataIntegrityKeyPair implements VerificationMethod {
    private final URI id;
    private final URI type;
    private final URI controller;
    private final byte[] privateKey;
    private final byte[] publicKey;

    public DataIntegrityKeyPair(URI id, URI type, URI controller, byte[] privateKey, byte[] publicKey) {
        super();
        this.id = id;
        this.type = type;
        this.controller = controller;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public DataIntegrityKeyPair(URI id, URI type, URI controller, byte[] privateKey) {
        this(id, type, controller, privateKey, null);
    }

    public URI id() {
        return id;
    }

    public URI type() {
        return type;
    }

    public URI controller() {
        return controller;
    }

    public byte[] privateKey() {
        return privateKey;
    }

    public byte[] publicKey() {
        return publicKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, controller, privateKey, publicKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DataIntegrityKeyPair) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.controller, that.controller) &&
                Objects.equals(this.privateKey, that.privateKey) &&
                Objects.equals(this.publicKey, that.publicKey);
    }

    @Override
    public String toString() {
        return "DataIntegrityKeyPair[" +
                "id=" + id + ", " +
                "type=" + type + ", " +
                "controller=" + controller + ", " +
                "privateKey=" + privateKey + ", " +
                "publicKey=" + publicKey + ']';
    }

}
