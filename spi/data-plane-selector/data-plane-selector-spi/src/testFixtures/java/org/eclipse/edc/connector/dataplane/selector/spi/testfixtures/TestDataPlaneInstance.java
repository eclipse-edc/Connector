/*
 *  Copyright (c) 2020 - 2022 Microsoft Corporation
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

package org.eclipse.edc.connector.dataplane.selector.spi.testfixtures;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.eclipse.edc.connector.dataplane.selector.spi.instance.DataPlaneInstance;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;


@JsonDeserialize(builder = TestDataPlaneInstance.Builder.class)
@JsonTypeName("dataspaceconnector:testdataplaneinstance")
public class TestDataPlaneInstance extends DataPlaneInstance {

    private Map<String, Object> properties;
    private int turnCount;
    private long lastActive;
    private URL url;
    private String id;

    private String name;

    private TestDataPlaneInstance() {
        super();
        turnCount = 0;
        lastActive = Instant.now().toEpochMilli();
        properties = new HashMap<>();
    }


    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean canHandle(DataAddress sourceAddress, DataAddress destinationAddress) {
        return true;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public int getTurnCount() {
        return turnCount;
    }

    @Override
    public long getLastActive() {
        return lastActive;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getName() {
        return name;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private final TestDataPlaneInstance instance;

        private Builder() {
            instance = new TestDataPlaneInstance();
        }

        @JsonCreator
        public static TestDataPlaneInstance.Builder newInstance() {
            return new TestDataPlaneInstance.Builder();
        }

        public TestDataPlaneInstance.Builder turnCount(int turnCount) {
            instance.turnCount = turnCount;
            return this;
        }

        public TestDataPlaneInstance.Builder lastActive(long lastActive) {
            instance.lastActive = lastActive;
            return this;
        }

        public TestDataPlaneInstance.Builder id(String id) {
            instance.id = id;
            return this;
        }


        public TestDataPlaneInstance.Builder url(URL url) {
            instance.url = url;
            return this;
        }

        public TestDataPlaneInstance.Builder url(String url) {
            try {
                instance.url = new URL(url);
            } catch (MalformedURLException e) {
                throw new EdcException(e);
            }
            return this;
        }

        public TestDataPlaneInstance build() {
            if (instance.id == null) {
                instance.id = UUID.randomUUID().toString();
            }
            Objects.requireNonNull(instance.url, "DataPlaneInstance must have an URL");

            return instance;
        }

        public TestDataPlaneInstance.Builder property(String key, Object value) {
            instance.properties.put(key, value);
            return this;
        }

        public TestDataPlaneInstance.Builder name(String name) {
            instance.name = name;
            return this;
        }


        private TestDataPlaneInstance.Builder properties(Map<String, Object> properties) {
            instance.properties = properties;
            return this;
        }
    }
}
