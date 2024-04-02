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

import com.apicatalog.jsonld.lang.Keywords;
import com.apicatalog.ld.schema.adapter.LdValueAdapter;
import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.eclipse.edc.jsonld.spi.JsonLdKeywords;

class ByteArrayAdapter implements LdValueAdapter<JsonValue, byte[]> {
    @Override
    public byte[] read(JsonValue value) {
        if (value.getValueType().equals(JsonValue.ValueType.OBJECT)) {
            var obj = value.asJsonObject();
            return obj.getString(JsonLdKeywords.VALUE).getBytes();
        }
        return value.toString().getBytes();
    }

    @Override
    public JsonValue write(byte[] value) {
        return Json.createObjectBuilder()
                .add(Keywords.VALUE, new String(value))
                .build();
    }

}
