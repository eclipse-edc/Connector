/*
 *  Copyright (c) 2021 - 2022 Daimler TSS GmbH, Fraunhofer Institute for Software and Systems Engineering
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
 *       Fraunhofer Institute for Software and Systems Engineering - refactoring
 *
 */

package org.eclipse.edc.protocol.ids.api.multipart;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Contract;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.ContractAgreementMessage;
import de.fraunhofer.iais.eis.ContractAgreementMessageBuilder;
import de.fraunhofer.iais.eis.ContractRejectionMessage;
import de.fraunhofer.iais.eis.ContractRejectionMessageBuilder;
import de.fraunhofer.iais.eis.ContractRequestBuilder;
import de.fraunhofer.iais.eis.ContractRequestMessage;
import de.fraunhofer.iais.eis.ContractRequestMessageBuilder;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.PermissionBuilder;
import jakarta.ws.rs.core.MediaType;
import net.javacrumbs.jsonunit.assertj.JsonAssertions;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.MultipartReader;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.ProviderContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.offer.ContractOfferResolver;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.junit.annotations.ComponentTest;
import org.eclipse.edc.junit.extensions.EdcExtension;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.protocol.ids.api.multipart.controller.MultipartController;
import org.eclipse.edc.protocol.ids.serialization.IdsTypeManagerUtil;
import org.eclipse.edc.protocol.ids.spi.types.IdsId;
import org.eclipse.edc.protocol.ids.spi.types.IdsType;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.http.EdcHttpClient;
import org.eclipse.edc.spi.iam.ClaimToken;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.testfixtures.TestUtils.getFreePort;
import static org.eclipse.edc.protocol.ids.spi.domain.IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
@ExtendWith(EdcExtension.class)
class MultipartControllerIntegrationTest {
    private static final int IDS_PORT = getFreePort();
    private static final String CONNECTOR_ID = UUID.randomUUID().toString();
    private static final String CATALOG_ID = UUID.randomUUID().toString();
    private static final String INFOMODEL_VERSION = "4.1.3";
    private final ObjectMapper objectMapper = getCustomizedObjectMapper();

    private final ContractOfferResolver contractOfferResolver = mock(ContractOfferResolver.class);
    private final ConsumerContractNegotiationManager consumerContractNegotiationManager =
            mock(ConsumerContractNegotiationManager.class);
    private final ProviderContractNegotiationManager providerContractNegotiationManager =
            mock(ProviderContractNegotiationManager.class);

    @BeforeEach
    void init(EdcExtension extension) {
        extension.setConfiguration(Map.of(
                "web.http.port", String.valueOf(getFreePort()),
                "web.http.path", "/api",
                "web.http.ids.port", String.valueOf(IDS_PORT),
                "web.http.ids.path", "/api/v1/ids",
                "edc.ids.id", "urn:connector:" + CONNECTOR_ID,
                "edc.ids.catalog.id", "urn:catalog:" + CATALOG_ID
        ));

        extension.registerSystemExtension(ServiceExtension.class, new TestExtension());
        extension.registerServiceMock(ContractOfferResolver.class, contractOfferResolver);
        extension.registerServiceMock(ProviderContractNegotiationManager.class, providerContractNegotiationManager);
        extension.registerServiceMock(ConsumerContractNegotiationManager.class, consumerContractNegotiationManager);
    }

    @Test
    void requestConnectorSelfDescriptionWithoutId(EdcHttpClient httpClient) throws Exception {
        var request = createRequest(getDescriptionRequestMessage());

        var response = httpClient.execute(request);

        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload,
                StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:BaseConnector");
        jsonPayload.inPath("$.@id").isString().matches("urn:connector:" + CONNECTOR_ID);
        jsonPayload.inPath("$.ids:version").isString().isEqualTo("0.0.1-SNAPSHOT");
        jsonPayload.inPath("$.ids:resourceCatalog").isPresent().isArray().hasSizeGreaterThanOrEqualTo(1);
        jsonPayload.inPath("$.ids:resourceCatalog[0].@type").isString().isEqualTo("ids" +
                ":ResourceCatalog");
        jsonPayload.inPath("$.ids:resourceCatalog[0].@id").isString().isEqualTo("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:hasDefaultEndpoint").isPresent().isObject();
        jsonPayload.inPath("$.ids:hasDefaultEndpoint.@type").isString().isEqualTo("ids" +
                ":ConnectorEndpoint");
        jsonPayload.inPath("$.ids:securityProfile").isObject();
        jsonPayload.inPath("$.ids:securityProfile.@id").isString().isEqualTo("https://w3id" +
                ".org/idsa/code/BASE_SECURITY_PROFILE");
        jsonPayload.inPath("$.ids:inboundModelVersion").isArray().contains(INFOMODEL_VERSION);
        jsonPayload.inPath("$.ids:outboundModelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonPayload.inPath("$.ids:title").isArray().hasSize(1);
        jsonPayload.inPath("$.ids:description").isArray().hasSize(1);
    }

    @Test
    void requestConnectorSelfDescriptionWithId(EdcHttpClient httpClient) throws Exception {
        var request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(CONNECTOR_ID).type(IdsType.CONNECTOR).build()
        ));

        var response = httpClient.execute(request);

        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload,
                StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:BaseConnector");
        jsonPayload.inPath("$.@id").isString().matches("urn:connector:" + CONNECTOR_ID);
        jsonPayload.inPath("$.ids:version").isString().isEqualTo("0.0.1-SNAPSHOT");
        jsonPayload.inPath("$.ids:resourceCatalog").isPresent().isArray().hasSizeGreaterThanOrEqualTo(1);
        jsonPayload.inPath("$.ids:resourceCatalog[0].@type").isString().isEqualTo("ids" +
                ":ResourceCatalog");
        jsonPayload.inPath("$.ids:resourceCatalog[0].@id").isString().isEqualTo("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:hasDefaultEndpoint").isPresent().isObject();
        jsonPayload.inPath("$.ids:hasDefaultEndpoint.@type").isString().isEqualTo("ids" +
                ":ConnectorEndpoint");
        jsonPayload.inPath("$.ids:securityProfile").isObject();
        jsonPayload.inPath("$.ids:securityProfile.@id").isString().isEqualTo("https://w3id" +
                ".org/idsa/code/BASE_SECURITY_PROFILE");
        jsonPayload.inPath("$.ids:inboundModelVersion").isArray().contains(INFOMODEL_VERSION);
        jsonPayload.inPath("$.ids:outboundModelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonPayload.inPath("$.ids:title").isArray().hasSize(1);
        jsonPayload.inPath("$.ids:description").isArray().hasSize(1);
    }

    @Test
    void requestDataCatalogWithAssets(EdcHttpClient httpClient, AssetIndex assetIndex) throws Exception {
        var assetId = UUID.randomUUID().toString();
        var asset = Asset.Builder.newInstance()
                .id(assetId)
                .property("asset:prop:fileName", "test.txt")
                .property("asset:prop:byteSize", BigInteger.valueOf(10))
                .property("asset:prop:fileExtension", "txt")
                .build();
        assetIndex.accept(asset, DataAddress.Builder.newInstance().type("test").build());
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .asset(asset)
                .policy(createEverythingAllowedPolicy())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now().plusMonths(1))
                .build();
        when(contractOfferResolver.queryContractOffers(any())).thenReturn(Stream.of(contractOffer));

        var request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(CATALOG_ID).type(IdsType.CATALOG).build()
        ));

        var response = httpClient.execute(request);

        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload,
                StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.@id").isString().matches("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:offeredResource[0].@id").isString().matches("urn:resource:" + assetId);
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].@type").isString().isEqualTo("ids:ContractOffer");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].@id").isString().matches("urn:contractoffer:.*");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].ids:permission[0]" +
                ".@type").isString().isEqualTo("ids:Permission");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].ids:permission[0].@id").isString().matches("urn:permission:.*");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:contractOffer[0].ids:permission[0]" +
                ".ids:action[0].@id").isString().isEqualTo("https://w3id.org/idsa/code/USE");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].@type").isString().isEqualTo("ids:Representation");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].@id").isString().matches("urn:representation:" + assetId);
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:instance[0].@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:instance[0].@id").isString().matches("urn:artifact:" + assetId);
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:instance[0]" +
                ".ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:instance[0]" +
                ".ids:byteSize.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#integer");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:instance[0]" +
                ".ids:byteSize.@value").isString().isEqualTo("10");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:mediaType.@type").isString().isEqualTo("ids:CustomMediaType");
        jsonPayload.inPath("$.ids:offeredResource[0].ids:representation[0].ids:mediaType" +
                ".ids:filenameExtension").isString().isEqualTo("txt");
    }

    @Test
    void requestDataCatalogNoAssets(EdcHttpClient httpClient) throws Exception {
        var request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(CATALOG_ID).type(IdsType.CATALOG).build()
        ));

        var response = httpClient.execute(request);

        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload,
                StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:ResourceCatalog");
        jsonPayload.inPath("$.@id").isString().matches("urn:catalog:" + CATALOG_ID);
        jsonPayload.inPath("$.ids:offeredResource.objectList[0]").isAbsent();
    }

    @Test
    void requestArtifact(EdcHttpClient httpClient, AssetIndex assetIndex) throws Exception {
        String assetId = UUID.randomUUID().toString();
        Asset asset = Asset.Builder.newInstance()
                .id(assetId)
                .property("asset:prop:fileName", "test.txt")
                .property("asset:prop:byteSize", BigInteger.valueOf(10))
                .property("asset:prop:fileExtension", "txt")
                .build();
        assetIndex.accept(asset, DataAddress.Builder.newInstance().type("test").build());

        var request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(assetId).type(IdsType.ARTIFACT).build()
        ));

        var response = httpClient.execute(request);

        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload,
                StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.@id").isString().matches("urn:artifact:" + assetId);
        jsonPayload.inPath("$.ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:byteSize.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#integer");
        jsonPayload.inPath("$.ids:byteSize.@value").isString().isEqualTo("10");
    }

    @Test
    void requestRepresentation(EdcHttpClient httpClient, AssetIndex assetIndex) throws Exception {
        String assetId = UUID.randomUUID().toString();
        Asset asset = Asset.Builder.newInstance()
                .id(assetId)
                .property("asset:prop:fileName", "test.txt")
                .property("asset:prop:byteSize", BigInteger.valueOf(10))
                .property("asset:prop:fileExtension", "txt")
                .build();
        assetIndex.accept(asset, DataAddress.Builder.newInstance().type("test").build());

        var request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(assetId).type(IdsType.REPRESENTATION).build()
        ));

        var response = httpClient.execute(request);

        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload,
                StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:Representation");
        jsonPayload.inPath("$.@id").isString().matches("urn:representation:" + assetId);
        jsonPayload.inPath("$.ids:mediaType.@type").isString().isEqualTo("ids:CustomMediaType");
        jsonPayload.inPath("$.ids:mediaType.ids:filenameExtension").isString().isEqualTo("txt");
        jsonPayload.inPath("$.ids:instance[0].@type").isString().isEqualTo("ids:Artifact");
        jsonPayload.inPath("$.ids:instance[0].@id").isString().matches("urn:artifact:" + assetId);
        jsonPayload.inPath("$.ids:instance[0].ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:instance[0].ids:byteSize.@type").isString().isEqualTo("http" +
                "://www.w3.org/2001/XMLSchema#integer");
        jsonPayload.inPath("$.ids:instance[0].ids:byteSize.@value").isString().isEqualTo("10");
    }

    @Test
    void requestResource(EdcHttpClient httpClient, AssetIndex assetIndex) throws Exception {
        String assetId = UUID.randomUUID().toString();
        Asset asset = Asset.Builder.newInstance()
                .id(assetId)
                .property("asset:prop:fileName", "test.txt")
                .property("asset:prop:byteSize", BigInteger.valueOf(10))
                .property("asset:prop:fileExtension", "txt")
                .build();
        assetIndex.accept(asset, DataAddress.Builder.newInstance().type("test").build());
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .asset(asset)
                .policy(createEverythingAllowedPolicy())
                .contractStart(ZonedDateTime.now())
                .contractEnd(ZonedDateTime.now().plusMonths(1))
                .build();
        when(contractOfferResolver.queryContractOffers(any())).thenReturn(Stream.of(contractOffer));

        var request = createRequest(getDescriptionRequestMessage(
                IdsId.Builder.newInstance().value(assetId).type(IdsType.RESOURCE).build()
        ));

        var response = httpClient.execute(request);

        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);

        var payload = content.stream().filter(n -> "payload".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonPayload = JsonAssertions.assertThatJson(new String(payload,
                StandardCharsets.UTF_8));

        jsonPayload.inPath("$.@type").isString().isEqualTo("ids:Resource");
        jsonPayload.inPath("$.@id").isString().matches("urn:resource:" + assetId);
        jsonPayload.inPath("$.ids:contractOffer[0].@type").isString().isEqualTo("ids" +
                ":ContractOffer");
        jsonPayload.inPath("$.ids:contractOffer[0].@id").isString().matches("urn:contractoffer:.*");
        jsonPayload.inPath("$.ids:contractOffer[0].ids:permission[0].@type").isString().isEqualTo("ids:Permission");
        jsonPayload.inPath("$.ids:contractOffer[0].ids:permission[0].@id").isString().matches(
                "urn:permission:.*");
        jsonPayload.inPath("$.ids:contractOffer[0].ids:permission[0].ids:action[0].@id").isString().matches("https://w3id.org/idsa/code/USE");
        jsonPayload.inPath("$.ids:representation[0].@type").isString().isEqualTo("ids" +
                ":Representation");
        jsonPayload.inPath("$.ids:representation[0].@id").isString().matches("urn:representation" +
                ":" + assetId);
        jsonPayload.inPath("$.ids:representation[0].ids:instance[0].@type").isString().isEqualTo(
                "ids:Artifact");
        jsonPayload.inPath("$.ids:representation[0].ids:instance[0].@id").isString().matches("urn" +
                ":artifact:" + assetId);
        jsonPayload.inPath("$.ids:representation[0].ids:instance[0].ids:fileName").isString().isEqualTo("test.txt");
        jsonPayload.inPath("$.ids:representation[0].ids:mediaType.@type").isString().isEqualTo(
                "ids:CustomMediaType");
        jsonPayload.inPath("$.ids:representation[0].ids:mediaType.ids:filenameExtension").isString().matches("txt");
    }

    @Test
    void testHandleContractRequest(EdcHttpClient httpClient, AssetIndex assetIndex) throws Exception {
        when(providerContractNegotiationManager.requested(any(), any())).thenReturn(StatusResult.success(createContractNegotiation("id")));
        var assetId = "1234";
        var request = createRequestWithPayload(getContractRequestMessage(),
                new ContractRequestBuilder(URI.create("urn:contractrequest:2345"))
                        ._provider_(URI.create("http://provider"))
                        ._consumer_(URI.create("http://consumer"))
                        ._permission_(new PermissionBuilder()
                                ._target_(URI.create("urn:artifact:" + assetId))
                                .build())
                        ._contractStart_(CalendarUtil.gregorianNow())
                        ._contractEnd_(CalendarUtil.gregorianNow())
                        .build());
        var asset = Asset.Builder.newInstance().id(assetId).build();
        assetIndex.accept(asset, DataAddress.Builder.newInstance().type("test").build());

        var response = httpClient.execute(request);

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
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
    }

    @Test
    void testHandleContractAgreement(EdcHttpClient httpClient, AssetIndex assetIndex) throws Exception {
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
                        ._contractDate_(CalendarUtil.gregorianNow())
                        .build());
        var asset = Asset.Builder.newInstance().id(assetId).build();
        assetIndex.accept(asset, DataAddress.Builder.newInstance().type("test").build());
        when(consumerContractNegotiationManager.confirmed(any(), any(), any(), any())).thenReturn(StatusResult.success(createContractNegotiation("id")));

        var response = httpClient.execute(request);

        assertThat(response).isNotNull().extracting(Response::code).isEqualTo(200);

        var content = extractNamedMultipartContent(response);

        assertThat(content)
                .hasSize(1)
                .extracting(NamedMultipartContent::getName)
                .containsExactly("header");

        var header = content.stream().filter(n -> "header".equalsIgnoreCase(n.getName()))
                .map(NamedMultipartContent::getContent)
                .findFirst()
                .orElseThrow();

        var jsonHeader = JsonAssertions.assertThatJson(new String(header, StandardCharsets.UTF_8));

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids" +
                ":MessageProcessedNotificationMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        verify(consumerContractNegotiationManager).confirmed(any(), any(), argThat(agreement -> assetId.equals(agreement.getAssetId())), any());
    }

    @Test
    void testHandleContractRejection(EdcHttpClient httpClient) throws Exception {
        when(providerContractNegotiationManager.declined(any(), any())).thenReturn(StatusResult.success(createContractNegotiation("id")));
        var request = createRequest(getContractRejectionMessage());

        var response = httpClient.execute(request);

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

        jsonHeader.inPath("$.@type").isString().isEqualTo("ids" +
                ":MessageProcessedNotificationMessage");
        jsonHeader.inPath("$.@id").isString().matches("urn:message:.*");
        jsonHeader.inPath("$.ids:modelVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:contentVersion").isString().isEqualTo(INFOMODEL_VERSION);
        jsonHeader.inPath("$.ids:issued.@type").isString().isEqualTo("http://www.w3" +
                ".org/2001/XMLSchema#dateTimeStamp");
        jsonHeader.inPath("$.ids:issued.@value").isString().satisfies(date -> {
            assertThat(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date)).isNotNull();
        });
        jsonHeader.inPath("$.ids:issuerConnector.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
        jsonHeader.inPath("$.ids:senderAgent.@id").isString().isEqualTo("urn:connector:" + CONNECTOR_ID);
    }

    protected String getUrl() {
        return String.format("http://localhost:%s/api/v1/ids%s", IDS_PORT,
                MultipartController.PATH);
    }

    protected DynamicAttributeToken getDynamicAttributeToken() {
        return new DynamicAttributeTokenBuilder()._tokenValue_("fake").build();
    }

    protected String toJson(Message message) throws Exception {
        return objectMapper.writeValueAsString(message);
    }

    protected String toJson(Contract contract) throws Exception {
        return objectMapper.writeValueAsString(contract);
    }

    protected DescriptionRequestMessage getDescriptionRequestMessage() {
        return getDescriptionRequestMessage(null);
    }

    protected DescriptionRequestMessage getDescriptionRequestMessage(IdsId idsId) {
        DescriptionRequestMessageBuilder builder = new DescriptionRequestMessageBuilder()
                ._securityToken_(getDynamicAttributeToken())
                ._issuerConnector_(URI.create("issuerConnector"))
                ._senderAgent_(URI.create("senderAgent"));

        if (idsId != null) {
            builder._requestedElement_(idsId.toUri());
        }
        return builder.build();
    }

    protected ContractRequestMessage getContractRequestMessage() {
        var message = new ContractRequestMessageBuilder()
                ._correlationMessage_(URI.create("urn:message:1"))
                ._securityToken_(getDynamicAttributeToken())
                ._senderAgent_(URI.create("senderAgent"))
                ._issuerConnector_(URI.create("issuerConnector"))
                .build();
        message.setProperty(IDS_WEBHOOK_ADDRESS_PROPERTY, "http://someUrl");
        return message;
    }

    protected ContractAgreementMessage getContractAgreementMessage() {
        return new ContractAgreementMessageBuilder()
                ._correlationMessage_(URI.create("urn:message:1"))
                ._securityToken_(getDynamicAttributeToken())
                ._issuerConnector_(URI.create("issuerConnector"))
                ._senderAgent_(URI.create("senderAgent"))
                .build();
    }

    protected ContractRejectionMessage getContractRejectionMessage() {
        return new ContractRejectionMessageBuilder()
                ._correlationMessage_(URI.create("urn:message:1"))
                ._transferContract_(URI.create("urn:contractagreement:1"))
                ._securityToken_(getDynamicAttributeToken())
                ._issuerConnector_(URI.create("issuerConnector"))
                ._senderAgent_(URI.create("senderAgent"))
                .build();
    }

    // create the multipart-form-data request having the given message in its "header" multipart
    // payload
    protected Request createRequest(Message message) throws Exception {
        Objects.requireNonNull(message);

        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(okhttp3.MediaType.get(MediaType.MULTIPART_FORM_DATA))
                .addPart(createIdsMessageHeaderMultipart(message))
                .build();

        return new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.parse(getUrl())))
                .addHeader("Content-Type", MediaType.MULTIPART_FORM_DATA)
                .post(multipartBody)
                .build();
    }

    protected Request createRequestWithPayload(Message message, Contract payload) throws Exception {
        Objects.requireNonNull(message);

        MultipartBody multipartBody = new MultipartBody.Builder()
                .setType(okhttp3.MediaType.get(MediaType.MULTIPART_FORM_DATA))
                .addPart(createIdsMessageHeaderMultipart(message))
                .addPart(createIdsMessagePayloadMultipart(payload))
                .build();

        return new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.parse(getUrl())))
                .addHeader("Content-Type", MediaType.MULTIPART_FORM_DATA)
                .post(multipartBody)
                .build();
    }

    // extract response to list of NamedMultipartContent container object
    protected List<NamedMultipartContent> extractNamedMultipartContent(Response response) throws Exception {
        List<NamedMultipartContent> namedMultipartContentList = new LinkedList<>();
        try (MultipartReader multipartReader =
                     new MultipartReader(Objects.requireNonNull(response.body()))) {
            MultipartReader.Part part;
            while ((part = multipartReader.nextPart()) != null) {
                HttpHeaders httpHeaders = HttpHeaders.of(
                        part.headers().toMultimap(),
                        (a, b) -> a.equalsIgnoreCase("Content-Disposition")
                );

                String value = httpHeaders.firstValue("Content-Disposition").orElse(null);
                if (value == null) {
                    continue;
                }

                ContentDisposition contentDisposition = new ContentDisposition(value);
                String multipartName = contentDisposition.getParameters().get("name");
                if (multipartName == null) {
                    continue;
                }

                namedMultipartContentList.add(new NamedMultipartContent(multipartName,
                        part.body().readByteArray()));
            }
        }

        return namedMultipartContentList;
    }

    private Policy createEverythingAllowedPolicy() {
        var policyBuilder = Policy.Builder.newInstance();
        var permissionBuilder = Permission.Builder.newInstance();
        var actionBuilder = Action.Builder.newInstance();

        policyBuilder.type(PolicyType.CONTRACT);
        actionBuilder.type("USE");
        permissionBuilder.target("1");

        permissionBuilder.action(actionBuilder.build());
        policyBuilder.permission(permissionBuilder.build());

        policyBuilder.target("1");
        return policyBuilder.build();
    }

    // create the "header" multipart payload
    private MultipartBody.Part createIdsMessageHeaderMultipart(Message message) throws Exception {
        Headers headers = new Headers.Builder()
                .add("Content-Disposition", "form-data; name=\"header\"")
                .build();

        RequestBody requestBody = RequestBody.create(toJson(message),
                okhttp3.MediaType.get(MediaType.APPLICATION_JSON));

        return MultipartBody.Part.create(headers, requestBody);
    }

    // create the "header" multipart payload
    private MultipartBody.Part createIdsMessagePayloadMultipart(Contract contract) throws Exception {
        Headers headers = new Headers.Builder()
                .add("Content-Disposition", "form-data; name=\"payload\"")
                .build();

        RequestBody requestBody = RequestBody.create(toJson(contract),
                okhttp3.MediaType.get(MediaType.APPLICATION_JSON));

        return MultipartBody.Part.create(headers, requestBody);
    }

    private ObjectMapper getCustomizedObjectMapper() {
        return IdsTypeManagerUtil.getIdsObjectMapper(new TypeManager());
    }

    private ContractNegotiation createContractNegotiation(String id) {
        return ContractNegotiation.Builder.newInstance()
                .id(id)
                .counterPartyId(UUID.randomUUID().toString())
                .counterPartyAddress("address")
                .protocol("protocol")
                .build();
    }

    public static class NamedMultipartContent {
        private final String name;
        private final byte[] content;

        NamedMultipartContent(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        public String getName() {
            return name;
        }

        public byte[] getContent() {
            return content;
        }
    }

    public static class TestExtension implements ServiceExtension {

        @Provider
        public IdentityService identityService() {
            var identityService = mock(IdentityService.class);
            var tokenResult = TokenRepresentation.Builder.newInstance().token("token").build();
            var claimToken = ClaimToken.Builder.newInstance().claim("key", "value").build();
            when(identityService.obtainClientCredentials(any())).thenReturn(Result.success(tokenResult));
            when(identityService.verifyJwtToken(any(), any())).thenReturn(Result.success(claimToken));
            return identityService;
        }

    }

}
