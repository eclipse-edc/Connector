/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class MultipartControllerIntegrationTest extends AbstractMultipartControllerIntegrationTest {
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private static final String CATALOG_ID = UUID.randomUUID().toString();

    private static OkHttpClient httpClient;

    @BeforeAll
    static void setUp() {
        httpClient = new OkHttpClient.Builder()
                .build();
    }

    @Override
    protected Map<String, String> getSystemProperties() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(getPort()));
                put("edc.ids.id", "urn:connector:" + CONNECTOR_ID);
                put("edc.ids.catalog.id", "urn:catalog:" + CATALOG_ID);
            }
        };
    }

    @Test
    void testRequestConnectorSelfDescriptionWithoutId() throws Exception {
        // prepare
        Request request = createRequest(getDescriptionRequestMessage());

        // invoke
        Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull()
                .extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(2)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header", "payload");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:DescriptionResponseMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:BaseConnector");
        jsonPayload.inPath("$.@id").isString().matches("urn:connector:.*");
        jsonPayload.inPath("$.ids:version").isString().isEqualTo("0.0.1");
        jsonPayload.inPath("$.ids:resourceCatalog").isPresent().isArray().hasSizeGreaterThanOrEqualTo(1);
        jsonPayload.inPath("$.ids:resourceCatalog[0].@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.ids:resourceCatalog[0].@id").isString().isEqualTo("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:hasDefaultEndpoint").isPresent().isObject();
        jsonPayload.inPath("$.ids:hasDefaultEndpoint.@type").isString().isEqualTo("ids:ConnectorEndpoint");
        jsonPayload.inPath("$.ids:securityProfile").isObject();
        jsonPayload.inPath("$.ids:securityProfile.@id").isString().isEqualTo("idsc:BASE_SECURITY_PROFILE");
        jsonPayload.inPath("$.ids:inboundModelVersion").isArray().contains("4.0.0");
        jsonPayload.inPath("$.ids:outboundModelVersion").isString().isEqualTo("4.0.0");
        jsonPayload.inPath("$.ids:title").isArray().hasSize(1);
        jsonPayload.inPath("$.ids:description").isArray().hasSize(1);
    }

    @Test
    void testRequestConnectorSelfDescriptionWithId() throws Exception {
        // prepare
        Request request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(CONNECTOR_ID).type(IdsType.CONNECTOR).build()
        ));

        // invoke
        Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull()
                .extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(2)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header", "payload");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:DescriptionResponseMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:BaseConnector");
        jsonPayload.inPath("$.@id").isString().matches("urn:connector:.*");
        jsonPayload.inPath("$.ids:version").isString().isEqualTo("0.0.1");
        jsonPayload.inPath("$.ids:resourceCatalog").isPresent().isArray().hasSizeGreaterThanOrEqualTo(1);
        jsonPayload.inPath("$.ids:resourceCatalog[0].@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.ids:resourceCatalog[0].@id").isString().isEqualTo("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:hasDefaultEndpoint").isPresent().isObject();
        jsonPayload.inPath("$.ids:hasDefaultEndpoint.@type").isString().isEqualTo("ids:ConnectorEndpoint");
        jsonPayload.inPath("$.ids:securityProfile").isObject();
        jsonPayload.inPath("$.ids:securityProfile.@id").isString().isEqualTo("idsc:BASE_SECURITY_PROFILE");
        jsonPayload.inPath("$.ids:inboundModelVersion").isArray().contains("4.0.0");
        jsonPayload.inPath("$.ids:outboundModelVersion").isString().isEqualTo("4.0.0");
        jsonPayload.inPath("$.ids:title").isArray().hasSize(1);
        jsonPayload.inPath("$.ids:description").isArray().hasSize(1);
    }

    @Test
    void testRequestDataCatalogWithAssets() throws Exception {
        // prepare
        Asset asset = Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .property("ids:fileName", "test.txt")
                .property("ids:byteSize", 10)
                .property("ids:fileExtension", "txt")
                .build();
        addAsset(asset);

        Request request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(CATALOG_ID).type(IdsType.CATALOG).build()
        ));

        // invoke
        Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull()
                .extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(2)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header", "payload");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:DescriptionResponseMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.@id").isString().matches("urn:catalog:.*");
        jsonPayload.inPath("$.ids:offeredResource[0].@type").isString().isEqualTo("ids:Resource");
        jsonPayload.inPath("$.ids:offeredResource[0].@id").isString().matches("urn:resource:.*");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].@type").isString().isEqualTo("ids:ContractOffer");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].@id").isString().matches("urn:contractoffer:.*");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].ids:permission[0].@type").isString().isEqualTo("ids:Permission");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].ids:permission[0].@id").isString().matches("urn:permission:.*");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].ids:permission[0].ids:action[0].@id").isString().isEqualTo("idsc:USE");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].@type").isString().isEqualTo("ids:Representation");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].@id").isString().matches("urn:representation:.*");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:instance[0].@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:instance[0].@id").isString().matches("urn:artifact:.*");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:instance[0].ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:instance[0].ids:byteSize").isIntegralNumber().isEqualTo(10);
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:mediaType.@type").isString().isEqualTo("ids:CustomMediaType");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:mediaType.ids:filenameExtension").isString().isEqualTo("txt");
    }

    @Test
    void testRequestDataCatalogNoAssets() throws Exception {
        // prepare
        Request request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(CATALOG_ID).type(IdsType.CATALOG).build()
        ));

        // invoke
        Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull()
                .extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(2)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header", "payload");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:DescriptionResponseMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.@id").isString().matches("urn:catalog:.*");
        jsonPayload.inPath("$.ids:offeredResource[0]").isAbsent();
    }

    @Test
    void testRequestArtifact() throws Exception {
        // prepare
        String assetId = UUID.randomUUID().toString();
        Asset asset = Asset.Builder.newInstance()
                .id(assetId)
                .property("ids:fileName", "test.txt")
                .property("ids:byteSize", 10)
                .property("ids:fileExtension", "txt")
                .build();
        addAsset(asset);

        Request request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(assetId).type(IdsType.ARTIFACT).build()
        ));

        // invoke
        Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull()
                .extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(2)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header", "payload");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:DescriptionResponseMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.@id").isString().matches("urn:artifact:.*");
        jsonPayload.inPath("$.ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:byteSize").isIntegralNumber().isEqualTo(10);
    }

    @Test
    void testRequestRepresentation() throws Exception {
        // prepare
        String assetId = UUID.randomUUID().toString();
        Asset asset = Asset.Builder.newInstance()
                .id(assetId)
                .property("ids:fileName", "test.txt")
                .property("ids:byteSize", 10)
                .property("ids:fileExtension", "txt")
                .build();
        addAsset(asset);

        Request request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(assetId).type(IdsType.REPRESENTATION).build()
        ));

        // invoke
        Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull()
                .extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(2)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header", "payload");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:DescriptionResponseMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:Representation");
        jsonPayload.inPath("$.@id").isString().matches("urn:representation:.*");
        jsonPayload.inPath("$.ids:mediaType.@type").isString().isEqualTo("ids:CustomMediaType");
        jsonPayload.inPath("$.ids:mediaType.ids:filenameExtension").isString().isEqualTo("txt");
        jsonPayload.inPath("$.ids:instance[0].@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.ids:instance[0].@id").isString().matches("urn:artifact:.*");
        jsonPayload.inPath("$.ids:instance[0].ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:instance[0].ids:byteSize").isIntegralNumber().isEqualTo(10);
    }

    @Test
    void testRequestResource() throws Exception {
        // prepare
        String assetId = UUID.randomUUID().toString();
        Asset asset = Asset.Builder.newInstance()
                .id(assetId)
                .property("ids:fileName", "test.txt")
                .property("ids:byteSize", 10)
                .property("ids:fileExtension", "txt")
                .build();
        addAsset(asset);

        Request request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(assetId).type(IdsType.RESOURCE).build()
        ));

        // invoke
        Response response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull()
                .extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(2)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header", "payload");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:DescriptionResponseMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.0.0");
        jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:Resource");
        jsonPayload.inPath("$.@id").isString().matches("urn:resource:.*");
        jsonPayload.inPath("$.ids:contractOffer[0].@type").isString().isEqualTo("ids:ContractOffer");
        jsonPayload.inPath("$.ids:contractOffer[0].@id").isString().matches("urn:contractoffer:.*");
        jsonPayload.inPath("$.ids:contractOffer[0].ids:permission[0].@type").isString().isEqualTo("ids:Permission");
        jsonPayload.inPath("$.ids:contractOffer[0].ids:permission[0].@id").isString().matches("urn:permission:.*");
        jsonPayload.inPath("$.ids:contractOffer[0].ids:permission[0].ids:action[0].@id").isString().matches("idsc:USE");
        jsonPayload.inPath("$.ids:representation[0].@type").isString().isEqualTo("ids:Representation");
        jsonPayload.inPath("$.ids:representation[0].@id").isString().matches("urn:representation:.*");
        jsonPayload.inPath("$.ids:representation[0].ids:instance[0].@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.ids:representation[0].ids:instance[0].@id").isString().matches("urn:artifact:.*");
        jsonPayload.inPath("$.ids:representation[0].ids:instance[0].ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:representation[0].ids:mediaType.@type").isString().isEqualTo("ids:CustomMediaType");
        jsonPayload.inPath("$.ids:representation[0].ids:mediaType.ids:filenameExtension").isString().matches("txt");
    }
}
