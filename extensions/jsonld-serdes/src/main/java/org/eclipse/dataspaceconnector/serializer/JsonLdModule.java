/*
 *  Copyright (c) 2022 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.serializer;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.dataspaceconnector.serializer.types.UriDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.UriSerializer;
import org.eclipse.dataspaceconnector.serializer.types.number.BigDecimalDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.number.BigIntegerDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.number.FloatDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.number.LongDeserializer;
import org.eclipse.dataspaceconnector.serializer.types.number.NumSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;

/**
 * Custom Jackson module that provides all customized type de/serializers.
 */
public class JsonLdModule extends SimpleModule {

    public JsonLdModule() {
        super();

        addSerializer(URI.class, new UriSerializer());

        addSerializer(BigDecimal.class, new NumSerializer());
        addSerializer(BigInteger.class, new NumSerializer());
        addSerializer(Long.class, new NumSerializer());
        addSerializer(Float.class, new NumSerializer());

        addDeserializer(URI.class, new UriDeserializer());
        addDeserializer(BigInteger.class, new BigIntegerDeserializer());
        addDeserializer(BigDecimal.class, new BigDecimalDeserializer());
        addDeserializer(Long.class, new LongDeserializer());
        addDeserializer(Float.class, new FloatDeserializer());
    }
}
