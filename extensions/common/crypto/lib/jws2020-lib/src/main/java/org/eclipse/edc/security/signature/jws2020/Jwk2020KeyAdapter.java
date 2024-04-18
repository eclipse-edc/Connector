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

package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.DocumentError;
import com.apicatalog.ld.Term;
import com.apicatalog.ld.node.LdNode;
import com.apicatalog.ld.node.LdNodeBuilder;
import com.apicatalog.ld.signature.VerificationMethod;
import com.apicatalog.vc.VcVocab;
import com.apicatalog.vc.method.MethodAdapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.edc.security.token.jwt.CryptoConverter;

import java.util.Map;
import java.util.Objects;

import static java.util.Optional.ofNullable;

public class Jwk2020KeyAdapter implements MethodAdapter {
    public static final Term CONTROLLER = Term.create("controller", VcVocab.SECURITY_VOCAB);
    public static final Term PUBLIC_KEY_JWK = Term.create("publicKeyJwk", VcVocab.SECURITY_VOCAB);
    public static final Term PRIVATE_KEY_JWK = Term.create("privateKeyJwk", VcVocab.SECURITY_VOCAB);

    private final ObjectMapper mapper;
    private final JsonAdapter jsonAdapter;

    public Jwk2020KeyAdapter(ObjectMapper mapper) {
        this.mapper = mapper;
        jsonAdapter = new JsonAdapter(mapper);
    }

    @Override
    public VerificationMethod read(JsonObject document) throws DocumentError {
        if (document == null) {
            throw new IllegalArgumentException("Verification method cannot be null.");
        }
        var node = LdNode.of(document);

        var id = node.id();
        var type = node.type().link();
        var controller = node.node(CONTROLLER).id();

        var keyProperty = getKeyProperty(node);

        var jwk = CryptoConverter.create(keyProperty);
        return new JsonWebKeyPair(id, type, controller, jwk);
    }

    @Override
    public JsonObject write(VerificationMethod value) {
        Objects.requireNonNull(value, "VerificationMethod cannot be null!");

        var nodebuilder = new LdNodeBuilder();
        if (value.id() != null) {
            nodebuilder.id(value.id());
        }
        var embedded = false;
        if (value.controller() != null) {
            nodebuilder.set(CONTROLLER).id(value.controller());
            embedded = true;
        }

        if (value instanceof JsonWebKeyPair ecKeyPair) {
            if (ecKeyPair.keyPair() != null) {
                nodebuilder.set(PUBLIC_KEY_JWK).map(jsonAdapter, serialize(ecKeyPair.keyPair().toPublicJWK().toJSONObject()));
                embedded = true;
            }
        }
        if (embedded) {
            nodebuilder.type(value.type().toASCIIString());
        }
        return nodebuilder.build();
    }

    private String getKeyProperty(LdNode node) throws DocumentError {
        var str = node.scalar(PRIVATE_KEY_JWK).exists() ?
                node.scalar(PRIVATE_KEY_JWK).value() :
                node.scalar(PUBLIC_KEY_JWK).value();

        return ofNullable(str).map(jsonValue -> {
            try {
                return mapper.writeValueAsString(jsonValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }).orElse(null);
    }

    private JsonValue serialize(Map<String, Object> jsonObject) {
        return mapper.convertValue(jsonObject, JsonValue.class);
    }
}
