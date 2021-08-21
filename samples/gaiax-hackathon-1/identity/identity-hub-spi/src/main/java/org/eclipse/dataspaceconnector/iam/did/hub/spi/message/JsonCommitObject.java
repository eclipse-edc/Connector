/*
 *  Copyright (c) 2021 Microsoft Corporation
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
package org.eclipse.dataspaceconnector.iam.did.hub.spi.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Alternative in {@code https://tools.ietf.org/html/rfc7515#section-3.1} format.
 *
 * It is constructed by splitting the parts (header, body, signature) of a conventional compact serialized JWT and placing them in the relevant properties
 * (protected, payload, signature).
 */
public class JsonCommitObject {
    @JsonProperty("protected")
    private String protectedHeader;
    private String payload;
    private String signature;
    private CommitHeader header;

    public CommitHeader getHeader() {
        return header;
    }

    public String getProtectedHeader() {
        return protectedHeader;
    }

    public String getPayload() {
        return payload;
    }

    public String getSignature() {
        return signature;
    }

    public JsonCommitObject(@JsonProperty("protected") String protectedHeader,
                            @JsonProperty("payload") String payload,
                            @JsonProperty("signature") String signature,
                            @JsonProperty("header") CommitHeader header) {
        this.protectedHeader = protectedHeader;
        this.payload = payload;
        this.signature = signature;
        this.header = header;
    }

    public JsonCommitObject(String serializedJwt, CommitHeader header) {
        var tokens = serializedJwt.split("\\.");
        if (tokens.length < 3) {
            throw new IllegalArgumentException("Invalid jwt");
        }
        this.protectedHeader = tokens[0];
        this.payload = tokens[1];
        this.signature = tokens[2];
        this.header = header;

        String hased = computeRev();

        this.header.setRev(hased);
    }

    @NotNull
    private String computeRev() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            var contents = protectedHeader + "." + payload;
            byte[] encoded = digest.digest(contents.getBytes(StandardCharsets.UTF_8));
            return new String(Base64.getUrlEncoder().withoutPadding().encode(encoded));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
