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
 *       Fraunhofer Institute for Software and Systems Engineering - add methods
 *
 */

package org.eclipse.dataspaceconnector.ids.api.multipart;

import de.fraunhofer.iais.eis.Contract;
import de.fraunhofer.iais.eis.ContractAgreementMessage;
import de.fraunhofer.iais.eis.ContractAgreementMessageBuilder;
import de.fraunhofer.iais.eis.ContractOfferMessage;
import de.fraunhofer.iais.eis.ContractOfferMessageBuilder;
import de.fraunhofer.iais.eis.ContractRejectionMessage;
import de.fraunhofer.iais.eis.ContractRejectionMessageBuilder;
import de.fraunhofer.iais.eis.ContractRequestMessage;
import de.fraunhofer.iais.eis.ContractRequestMessageBuilder;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.DescriptionRequestMessageBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Message;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.iais.eis.ids.jsonld.Serializer;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.MultipartReader;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.ids.api.multipart.controller.MultipartController;
import org.eclipse.dataspaceconnector.ids.api.multipart.message.MultipartResponse;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.IdsIdParser;
import org.eclipse.dataspaceconnector.ids.spi.IdsType;
import org.eclipse.dataspaceconnector.ids.transform.IdsProtocol;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.iam.IdentityService;
import org.eclipse.dataspaceconnector.spi.iam.TokenRepresentation;
import org.eclipse.dataspaceconnector.spi.result.Result;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.dataspaceconnector.ids.core.util.CalendarUtil.gregorianNow;

@ExtendWith(EdcExtension.class)
abstract class AbstractMultipartControllerIntegrationTest {

    protected static final String CONNECTOR_ID_REQUEST_SENDER = "urn:connector:sender";
    protected static final String CONNECTOR_ID_REQUEST_RECEIVER = "urn:connector:receiver";

    public static final String HEADER = "header";
    public static final String PAYLOAD = "payload";
    private static final AtomicReference<Integer> PORT = new AtomicReference<>();
    private static final List<Asset> ASSETS = new LinkedList<>();

    private Serializer serializer;

    @BeforeEach
    protected void before(EdcExtension extension) {
        PORT.set(findUnallocatedServerPort());

        for (Map.Entry<String, String> entry : getSystemProperties().entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        extension.registerSystemExtension(ServiceExtension.class, new IdsApiMultipartEndpointV1IntegrationTestServiceExtension(ASSETS));
        serializer = new Serializer();
        IdentityService identityService = Mockito.mock(IdentityService.class);
        TokenRepresentation token = TokenRepresentation.Builder.newInstance().token("").build();
        Mockito.when(identityService.obtainClientCredentials(Mockito.any())).thenReturn(Result.success(token));
    }

    @AfterEach
    void after() {
        ASSETS.clear();

        for (String key : getSystemProperties().keySet()) {
            System.clearProperty(key);
        }

        PORT.set(null);
    }

    protected void addAsset(Asset asset) {
        ASSETS.add(asset);
    }

    protected int getPort() {
        return PORT.get();
    }

    private static int findUnallocatedServerPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected String getUrl() {
        return String.format("http://localhost:%s/api%s", getPort(), MultipartController.PATH);
    }

    protected abstract Map<String, String> getSystemProperties();

    protected DynamicAttributeToken getDynamicAttributeToken() {
        return new DynamicAttributeTokenBuilder()._tokenFormat_(TokenFormat.JWT)._tokenValue_("xxxxx.yyyyy.zzzzz").build();
    }

    protected String toJson(Message message) throws Exception {
        return serializer.serialize(message);
    }

    protected String toJson(Contract contract) throws Exception {
        return serializer.serialize(contract);
    }

    protected DescriptionRequestMessage getDescriptionRequestMessage() {
        return getDescriptionRequestMessage(null);
    }

    protected DescriptionRequestMessage getDescriptionRequestMessage(@Nullable IdsId idsId) {

        DescriptionRequestMessageBuilder builder = new DescriptionRequestMessageBuilder(getRandomMessageURI())
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(getDynamicAttributeToken())
                ._issuerConnector_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                ._senderAgent_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                // request on issuer connector because it doesn't matter
                ._recipientConnector_(Collections.singletonList(URI.create(CONNECTOR_ID_REQUEST_RECEIVER)));

        // can be null to request self description
        if (idsId != null) {
            builder._requestedElement_(
                    URI.create(String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, idsId.getType().getValue(), idsId.getValue())));
        }

        return builder.build();
    }

    protected ContractRequestMessage getContractRequestMessage() {

        var message = new ContractRequestMessageBuilder(getRandomMessageURI())
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(getDynamicAttributeToken())
                ._issuerConnector_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                ._senderAgent_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                // request on issuer connector because it doesn't matter
                ._recipientConnector_(Collections.singletonList(URI.create(CONNECTOR_ID_REQUEST_RECEIVER)))
                ._correlationMessage_(getRandomMessageURI())
                .build();
        message.setProperty("idsWebhookAddress", "http://someUrl");
        return message;
    }

    protected ContractAgreementMessage getContractAgreementMessage() {
        return new ContractAgreementMessageBuilder(getRandomMessageURI())
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(getDynamicAttributeToken())
                ._issuerConnector_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                ._senderAgent_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                // request on issuer connector because it doesn't matter
                ._recipientConnector_(Collections.singletonList(URI.create(CONNECTOR_ID_REQUEST_RECEIVER)))
                ._correlationMessage_(getRandomMessageURI())
                .build();
    }

    protected ContractRejectionMessage getContractRejectionMessage() {
        return new ContractRejectionMessageBuilder(getRandomMessageURI())
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(getDynamicAttributeToken())
                ._issuerConnector_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                ._senderAgent_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                ._recipientConnector_(Collections.singletonList(URI.create(CONNECTOR_ID_REQUEST_RECEIVER)))
                ._correlationMessage_(getRandomMessageURI())
                .build();
    }

    protected ContractOfferMessage getContractOfferMessage() {

        return new ContractOfferMessageBuilder(getRandomMessageURI())
                ._modelVersion_(IdsProtocol.INFORMATION_MODEL_VERSION)
                ._issued_(gregorianNow())
                ._securityToken_(getDynamicAttributeToken())
                ._issuerConnector_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                ._senderAgent_(URI.create(CONNECTOR_ID_REQUEST_SENDER))
                // request on issuer connector because it doesn't matter
                ._recipientConnector_(Collections.singletonList(URI.create(CONNECTOR_ID_REQUEST_RECEIVER)))
                ._correlationMessage_(getRandomMessageURI())
                .build();
    }

    // create the "header" multipart payload
    private MultipartBody.Part createIdsMessageHeaderMultipart(Message message) throws Exception {
        Headers headers = new Headers.Builder()
                .add("Content-Disposition", "form-data; name=\"header\"")
                .build();

        RequestBody requestBody = RequestBody.create(
                toJson(message),
                okhttp3.MediaType.get(MediaType.APPLICATION_JSON));

        return MultipartBody.Part.create(headers, requestBody);
    }

    // create the "header" multipart payload
    private MultipartBody.Part createIdsMessagePayloadMultipart(Contract contract) throws Exception {
        Headers headers = new Headers.Builder()
                .add("Content-Disposition", "form-data; name=\"payload\"")
                .build();

        RequestBody requestBody = RequestBody.create(
                toJson(contract),
                okhttp3.MediaType.get(MediaType.APPLICATION_JSON));

        return MultipartBody.Part.create(headers, requestBody);
    }

    // create the multipart-form-data request having the given message in its "header" multipart payload
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

    // extract response to MultipartResponse container object
    protected MultipartResponse extractMultipartResponse(Response response) throws Exception {
        Message header = null;
        byte[] payload = null;
        try (MultipartReader multipartReader = new MultipartReader(Objects.requireNonNull(response.body()))) {
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

                if (multipartName.equalsIgnoreCase(HEADER)) {
                    header = serializer.deserialize(part.body().readString(StandardCharsets.UTF_8), Message.class);
                } else if (multipartName.equalsIgnoreCase(PAYLOAD)) {
                    payload = part.body().readByteArray();
                }
            }
        }

        return MultipartResponse.Builder.newInstance().header(header).payload(payload).build();
    }

    // extract response to list of NamedMultipartContent container object
    protected List<NamedMultipartContent> extractNamedMultipartContent(Response response) throws Exception {
        List<NamedMultipartContent> namedMultipartContentList = new LinkedList<>();
        try (MultipartReader multipartReader = new MultipartReader(Objects.requireNonNull(response.body()))) {
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

                namedMultipartContentList.add(new NamedMultipartContent(multipartName, part.body().readByteArray()));
            }
        }

        return namedMultipartContentList;
    }

    private URI getRandomMessageURI() {
        IdsId messageId = IdsId.Builder.newInstance().type(IdsType.MESSAGE).value(UUID.randomUUID().toString()).build();
        return URI.create(String.join(IdsIdParser.DELIMITER, IdsIdParser.SCHEME, messageId.getType().getValue(), messageId.getValue()));
    }

    public static class NamedMultipartContent {
        private final String name;
        private final byte[] content;

        public NamedMultipartContent(String name, byte[] content) {
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
}
