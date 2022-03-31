/*
 *  Copyright (c) 2021-2022 Daimler TSS GmbH, Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Daimler TSS GmbH - Initial API and Implementation
 *       Fraunhofer Institute for Software and Systems Engineering - add tests
 *       Daimler TSS GmbH - introduce factory to create RequestInProcessMessage
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.ContractOfferBuilder;
import de.fraunhofer.iais.eis.ContractRequestBuilder;
import de.fraunhofer.iais.eis.PermissionBuilder;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.common.annotations.ComponentTest;
import org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.testOkHttpClient;

@ComponentTest
public class MultipartControllerIntegrationTest extends AbstractMultipartControllerIntegrationTest {
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private static final String CATALOG_ID = UUID.randomUUID().toString();

    private static OkHttpClient httpClient;

    @BeforeAll
    static void setUp() {
        httpClient = testOkHttpClient();
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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.2.7");
        //TODO once https://github.com/eclipse-dataspaceconnector/DataSpaceConnector/issues/236 is done
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:BaseConnector");
        jsonPayload.inPath("$.@id").isString().matches("urn:connector:" + CONNECTOR_ID);
        jsonPayload.inPath("$.ids:version").isString().isEqualTo("0.0.1");
        jsonPayload.inPath("$.ids:resourceCatalog").isPresent().isArray().hasSizeGreaterThanOrEqualTo(1);
        jsonPayload.inPath("$.ids:resourceCatalog[0].@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.ids:resourceCatalog[0].@id").isString().isEqualTo("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:hasDefaultEndpoint").isPresent().isObject();
        jsonPayload.inPath("$.ids:hasDefaultEndpoint.@type").isString().isEqualTo("ids:ConnectorEndpoint");
        jsonPayload.inPath("$.ids:securityProfile").isObject();
        jsonPayload.inPath("$.ids:securityProfile.@id").isString().isEqualTo("https://w3id.org/idsa/code/BASE_SECURITY_PROFILE");
        jsonPayload.inPath("$.ids:inboundModelVersion").isArray().contains("4.2.7");
        jsonPayload.inPath("$.ids:outboundModelVersion").isString().isEqualTo("4.2.7");
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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:BaseConnector");
        jsonPayload.inPath("$.@id").isString().matches("urn:connector:" + CONNECTOR_ID);
        jsonPayload.inPath("$.ids:version").isString().isEqualTo("0.0.1");
        jsonPayload.inPath("$.ids:resourceCatalog").isPresent().isArray().hasSizeGreaterThanOrEqualTo(1);
        jsonPayload.inPath("$.ids:resourceCatalog[0].@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.ids:resourceCatalog[0].@id").isString().isEqualTo("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:hasDefaultEndpoint").isPresent().isObject();
        jsonPayload.inPath("$.ids:hasDefaultEndpoint.@type").isString().isEqualTo("ids:ConnectorEndpoint");
        jsonPayload.inPath("$.ids:securityProfile").isObject();
        jsonPayload.inPath("$.ids:securityProfile.@id").isString().isEqualTo("https://w3id.org/idsa/code/BASE_SECURITY_PROFILE");
        jsonPayload.inPath("$.ids:inboundModelVersion").isArray().contains("4.2.7");
        jsonPayload.inPath("$.ids:outboundModelVersion").isString().isEqualTo("4.2.7");
        jsonPayload.inPath("$.ids:title").isArray().hasSize(1);
        jsonPayload.inPath("$.ids:description").isArray().hasSize(1);
    }

    @Test
    void testRequestDataCatalogWithAssets() throws Exception {
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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.@id").isString().matches("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].@id").isString().matches("urn:resource:" + assetId);
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:contractOffer[0].@type").isString().isEqualTo("ids:ContractOffer");
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:contractOffer[0].@id").isString().matches("urn:contractoffer:.*");
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:contractOffer[0].ids:permission[0].@type").isString().isEqualTo("ids:Permission");
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:contractOffer[0].ids:permission[0].@id").isString().matches("urn:permission:.*");
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:contractOffer[0].ids:permission[0].ids:action[0].@id").isString().isEqualTo("https://w3id.org/idsa/code/USE");
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:representation[0].@type").isString().isEqualTo("ids:Representation");
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:representation[0].@id").isString().matches("urn:representation:" + assetId);
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:representation[0].ids:instance[0].@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:representation[0].ids:instance[0].@id").isString().matches("urn:artifact:" + assetId);
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:representation[0].ids:instance[0].ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:representation[0].ids:instance[0].ids:byteSize").isIntegralNumber().isEqualTo(10);
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:representation[0].ids:mediaType.@type").isString().isEqualTo("ids:CustomMediaType");
        jsonPayload.inPath("$.ids:offeredResource.objectList[0].ids:representation[0].ids:mediaType.ids:filenameExtension").isString().isEqualTo("txt");
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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.@id").isString().matches("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:offeredResource.objectList[0]").isAbsent();
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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.@id").isString().matches("urn:artifact:" + assetId);
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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:Representation");
        jsonPayload.inPath("$.@id").isString().matches("urn:representation:" + assetId);
        jsonPayload.inPath("$.ids:mediaType.@type").isString().isEqualTo("ids:CustomMediaType");
        jsonPayload.inPath("$.ids:mediaType.ids:filenameExtension").isString().isEqualTo("txt");
        jsonPayload.inPath("$.ids:instance[0].@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.ids:instance[0].@id").isString().matches("urn:artifact:" + assetId);
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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload, StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:Resource");
        jsonPayload.inPath("$.@id").isString().matches("urn:resource:" + assetId);
        jsonPayload.inPath("$.ids:contractOffer[0].@type").isString().isEqualTo("ids:ContractOffer");
        jsonPayload.inPath("$.ids:contractOffer[0].@id").isString().matches("urn:contractoffer:.*");
        jsonPayload.inPath("$.ids:contractOffer[0].ids:permission[0].@type").isString().isEqualTo("ids:Permission");
        jsonPayload.inPath("$.ids:contractOffer[0].ids:permission[0].@id").isString().matches("urn:permission:.*");
        jsonPayload.inPath("$.ids:contractOffer[0].ids:permission[0].ids:action[0].@id").isString().matches("https://w3id.org/idsa/code/USE");
        jsonPayload.inPath("$.ids:representation[0].@type").isString().isEqualTo("ids:Representation");
        jsonPayload.inPath("$.ids:representation[0].@id").isString().matches("urn:representation:" + assetId);
        jsonPayload.inPath("$.ids:representation[0].ids:instance[0].@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.ids:representation[0].ids:instance[0].@id").isString().matches("urn:artifact:" + assetId);
        jsonPayload.inPath("$.ids:representation[0].ids:instance[0].ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:representation[0].ids:mediaType.@type").isString().isEqualTo("ids:CustomMediaType");
        jsonPayload.inPath("$.ids:representation[0].ids:mediaType.ids:filenameExtension").isString().matches("txt");
    }

    @Test
    void testHandleContractRequest() throws Exception {
        // prepare
        var assetId = "1234";
        var request = createRequestWithPayload(getContractRequestMessage(),
                new ContractRequestBuilder(URI.create("urn:contractrequest:2345"))
                        ._provider_(URI.create("http://provider"))
                        ._consumer_(URI.create("http://consumer"))
                        ._permission_(new PermissionBuilder()
                                ._target_(URI.create("urn:artifact:" + assetId))
                                .build())
                        .build());
        addAsset(Asset.Builder.newInstance().id(assetId).build());


        // invoke
        var response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(1)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:RequestInProcessMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
    }

    @Test
    void testHandleContractOffer() throws Exception {
        // prepare
        var request = createRequestWithPayload(getContractOfferMessage(),
                new ContractOfferBuilder().build());

        // invoke
        var response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(1)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:RequestInProcessMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
    }

    @Test
    void testHandleContractAgreement() throws Exception {
        // prepare
        var assetId = "1234";
        var request = createRequestWithPayload(getContractAgreementMessage(),
                new ContractAgreementBuilder(URI.create("urn:contractagreement:1"))
                        ._provider_(URI.create("http://provider"))
                        ._consumer_(URI.create("http://consumer"))
                        ._permission_(new PermissionBuilder()
                                ._target_(URI.create("urn:artifact:" + assetId))
                                .build())
                        ._contractStart_(CalendarUtil.gregorianNow())
                        ._contractEnd_(CalendarUtil.gregorianNow())
                        ._contractDate_(CalendarUtil.gregorianNow()) // TODO Throws exception, but mandatory
                        .build());
        addAsset(Asset.Builder.newInstance().id(assetId).build());

        // invoke
        var response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(1)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:MessageProcessedNotificationMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
    }

    @Test
    void testHandleContractRejection() throws Exception {
        // prepare
        var request = createRequest(getContractRejectionMessage());

        // invoke
        var response = httpClient.newCall(request).execute();

        // verify
        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

        List<NamedMultipartContent> content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(1)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids:MessageProcessedNotificationMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo("4.2.7");
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo("4.2.7");
        //jsonHeader.inPath("$.ids:issued").isString().matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}UTC$");
        jsonHeader.inPath("$.ids:issuerConnector").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
    }

    @Override
    protected Map<String, String> getSystemProperties() {
        return new HashMap<>() {
            {
                put("web.http.port", String.valueOf(getPort()));
                put("web.http.path", "/api");
                put("web.http.ids.port", String.valueOf(getIdsPort()));
                put("web.http.ids.path", "/api/v1/ids");
                put("edc.ids.id", "urn:connector:" + CONNECTOR_ID);
                put("edc.ids.catalog.id", "urn:catalog:" + CATALOG_ID);
            }
        };
    }

}
