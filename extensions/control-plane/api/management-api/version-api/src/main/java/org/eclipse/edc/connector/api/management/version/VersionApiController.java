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

package org.eclipse.edc.connector.api.management.version;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.util.Map;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path("/version")
public class VersionApiController implements VersionApi {
    public static final String API_VERSION_JSON_FILE = "api-version.json";
    public static final String VERSION_PROPERTY = "version";
    public static final String DATE_PROPERTY = "date";
    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {
    };
    private final ClassLoader resourceClassLoader;
    private final JsonBuilderFactory jsonBuilderFactory;
    private final ObjectMapper objectMapper;
    private JsonObject versionObject;

    public VersionApiController(ClassLoader resourceClassLoader, JsonBuilderFactory jsonBuilderFactory, ObjectMapper objectMapper) {
        this.resourceClassLoader = resourceClassLoader;
        this.jsonBuilderFactory = jsonBuilderFactory;
        this.objectMapper = objectMapper;
    }

    @GET
    @Path("/")
    @Override
    public JsonObject getVersion() {
        if (versionObject == null) {
            try (var versionContent = resourceClassLoader.getResourceAsStream(API_VERSION_JSON_FILE)) {
                if (versionContent == null) {
                    throw new EdcException("Version file not found or not readable.");
                }
                var content = objectMapper.readValue(versionContent, MAP_TYPE);
                versionObject = jsonBuilderFactory.createObjectBuilder()
                        .add(EDC_NAMESPACE + VERSION_PROPERTY, content.get(VERSION_PROPERTY))
                        .add(EDC_NAMESPACE + DATE_PROPERTY, content.get(DATE_PROPERTY))
                        .build();

            } catch (IOException e) {
                throw new EdcException(e);
            }
        }
        return versionObject;
    }
}
