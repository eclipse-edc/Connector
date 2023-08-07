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

package org.eclipse.edc.security.signature.jws2020;

import com.apicatalog.ld.schema.LdObject;
import com.apicatalog.ld.schema.LdTerm;
import com.apicatalog.ld.schema.adapter.LdValueAdapter;
import com.apicatalog.ld.signature.method.VerificationMethod;
import com.apicatalog.vc.integrity.DataIntegrity;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.edc.security.signature.jws2020.Jws2020Schema.JWK_PRIVATE_KEY;
import static org.eclipse.edc.security.signature.jws2020.Jws2020Schema.JWK_PUBLIC_KEY;

/**
 * Adapter that converts between {@link LdObject} and {@link VerificationMethod}
 */
class JwkAdapter implements LdValueAdapter<LdObject, VerificationMethod> {

    @Override
    public VerificationMethod read(LdObject value) {
        URI id = value.value(LdTerm.ID);
        URI type = value.value(LdTerm.TYPE);
        URI controller = value.value(DataIntegrity.CONTROLLER);
        var keyProperty = getKeyProperty(value);
        var jwk = KeyFactory.create(keyProperty);
        return new JwkMethod(id, type, controller, jwk);
    }


    @Override
    public LdObject write(VerificationMethod method) {
        var result = new HashMap<String, Object>();
        Objects.requireNonNull(method, "VerificationMethod cannot be null!");

        if (method.id() != null) {
            result.put(LdTerm.ID.uri(), method.id());
        }
        if (method.type() != null) {
            result.put(LdTerm.TYPE.uri(), method.type());
        }
        if (method.controller() != null) {
            result.put(DataIntegrity.CONTROLLER.uri(), method.controller());
        }

        if (method instanceof JwkMethod ecKeyPair) {
            if (ecKeyPair.keyPair() != null) {
                result.put(JWK_PUBLIC_KEY.uri(), ecKeyPair.keyPair().toPublicJWK().toJSONObject());
            }
        }

        return new LdObject(result);
    }

    private Map<String, Object> getKeyProperty(LdObject value) {
        if (value.contains(JWK_PRIVATE_KEY)) {
            return value.value(JWK_PRIVATE_KEY);
        }
        return value.value(JWK_PUBLIC_KEY);
    }

}
