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

package org.eclipse.edc.protocol.ids.jsonld;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.Action;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactBuilder;
import de.fraunhofer.iais.eis.AuthInfo;
import de.fraunhofer.iais.eis.AuthInfoBuilder;
import de.fraunhofer.iais.eis.AuthStandard;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.BaseConnectorBuilder;
import de.fraunhofer.iais.eis.BinaryOperator;
import de.fraunhofer.iais.eis.ConnectorUpdateMessage;
import de.fraunhofer.iais.eis.ContentType;
import de.fraunhofer.iais.eis.ContractAgreementBuilder;
import de.fraunhofer.iais.eis.ContractOffer;
import de.fraunhofer.iais.eis.ContractOfferBuilder;
import de.fraunhofer.iais.eis.CustomMediaType;
import de.fraunhofer.iais.eis.CustomMediaTypeBuilder;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.DynamicAttributeTokenBuilder;
import de.fraunhofer.iais.eis.Frequency;
import de.fraunhofer.iais.eis.Language;
import de.fraunhofer.iais.eis.LeftOperand;
import de.fraunhofer.iais.eis.LogicalConstraint;
import de.fraunhofer.iais.eis.LogicalConstraintBuilder;
import de.fraunhofer.iais.eis.PaymentModality;
import de.fraunhofer.iais.eis.Permission;
import de.fraunhofer.iais.eis.PermissionBuilder;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.RejectionMessageBuilder;
import de.fraunhofer.iais.eis.RejectionReason;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RepresentationBuilder;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceBuilder;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.ResourceCatalogBuilder;
import de.fraunhofer.iais.eis.Token;
import de.fraunhofer.iais.eis.TokenBuilder;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.iais.eis.util.RdfResource;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.edc.protocol.ids.serialization.IdsConstraintBuilder;
import org.eclipse.edc.protocol.ids.serialization.IdsConstraintImpl;
import org.eclipse.edc.protocol.ids.serialization.IdsTypeManagerUtil;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.protocol.ids.util.CalendarUtil;
import org.eclipse.edc.spi.types.TypeManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SerializationTest {
    private ObjectMapper objectMapper;

    private final String string = "example";
    private final String language = "en";
    private final String type = "json";
    private final BigInteger bigInt = BigInteger.valueOf(365);
    private final BigDecimal bigDecimal = BigDecimal.valueOf(365);
    private final XMLGregorianCalendar date = CalendarUtil.gregorianNow();
    private final URI uri = URI.create("http://example");

    private final TypedLiteral typedLiteral = new TypedLiteral(string, language);
    private final CustomMediaType mediaType = new CustomMediaTypeBuilder()._filenameExtension_(type).build();

    private final Language idsLanguage = Language.EN;
    private final Frequency idsFrequency = Frequency.DAILY;
    private final ContentType idsContentType = ContentType.SCHEMA_DEFINITION;
    private final PaymentModality idsPaymentModality = PaymentModality.FIXED_PRICE;
    private final RejectionReason reason = RejectionReason.BAD_PARAMETERS;
    private final TokenFormat format = TokenFormat.JWT;
    private final AuthStandard standard = AuthStandard.OAUTH2_JWT;
    private final Action action = Action.USE;
    private final LeftOperand leftOperand = LeftOperand.COUNT;
    private final BinaryOperator binaryOperator = BinaryOperator.AFTER;

    private final Token token = new TokenBuilder()._tokenFormat_(format)._tokenValue_(string).build();
    private final DynamicAttributeToken dat = new DynamicAttributeTokenBuilder()._tokenFormat_(format)._tokenValue_(string).build();
    private final AuthInfo authInfo = new AuthInfoBuilder()._authService_(uri)._authStandard_(standard).build();

    @BeforeEach
    void setUp() {
        objectMapper = IdsTypeManagerUtil.getIdsObjectMapper(new TypeManager());
    }

    @Test
    void serialize() throws IOException {
        var obj = new ContractAgreementBuilder(URI.create("urn:contractagreement:1"))
                ._provider_(URI.create("http://provider"))
                ._consumer_(URI.create("http://consumer"))
                ._permission_(new PermissionBuilder()
                        ._target_(URI.create("urn:artifact:"))
                        .build())
                ._contractStart_(CalendarUtil.gregorianNow())
                ._contractEnd_(CalendarUtil.gregorianNow())
                ._contractDate_(CalendarUtil.gregorianNow())
                .build();

        var result = objectMapper.writeValueAsString(obj);
        assertTrue(result.contains("@context"));

        var agreement = objectMapper.readValue(result, JsonNode.class);

        assertTrue(agreement.get("@id").asText().contains("urn:contractagreement:1"));
        assertTrue(agreement.get("@type").asText().contains("ids:ContractAgreement"));
    }

    @Test
    void deserializeDscMessage() throws IOException {
        var obj = "{\n" +
                "  \"@context\" : {\n" +
                "  \"ids\" : \"https://w3id.org/idsa/core/\",\n" +
                "  \"idsc\" : \"https://w3id.org/idsa/code/\"\n" +
                "  },\n" +
                "  \"@type\" : \"ids:ConnectorUpdateMessage\",\n" +
                "  \"@id\" : \"" + uri + "\",\n" +
                "  \"ids:affectedConnector\" : {\n" +
                "    \"@id\" : \"" + uri + "\"\n" +
                "  },\n" +
                "  \"ids:issuerConnector\" : {\n" +
                "    \"@id\" : \"" + uri + "\"\n" +
                "  },\n" +
                "  \"ids:modelVersion\" : \"" + string + "\",\n" +
                "  \"ids:senderAgent\" : {\n" +
                "    \"@id\" : \"" + uri + "\"\n" +
                "  },\n" +
                "  \"ids:issued\" : {\n" +
                "    \"@value\" : \"" + date + "\",\n" +
                "    \"@type\" : \"http://www.w3.org/2001/XMLSchema#dateTimeStamp\"\n" +
                "  }\n" +
                "}";

        var result = objectMapper.readValue(obj, ConnectorUpdateMessage.class);
        assertNotNull(result);

        assertEquals(uri, result.getId());
        assertEquals(uri, result.getAffectedConnector());
        assertEquals(uri, result.getIssuerConnector());
        assertEquals(uri, result.getSenderAgent());
        assertEquals(string, result.getModelVersion());
        assertEquals(date, result.getIssued());
        assertNull(result.getCorrelationMessage());
        assertNull(result.getContentVersion());
        assertNull(result.getAuthorizationToken());
    }

    @Test
    void serialize_deserialize_rejectionMessage() throws IOException {
        var obj = new RejectionMessageBuilder()
                ._authorizationToken_(token)
                ._contentVersion_(string)
                ._correlationMessage_(uri)
                ._issued_(date)
                ._issuerConnector_(uri)
                ._modelVersion_(string)
                ._recipientAgent_(uri)
                ._recipientConnector_(uri)
                ._rejectionReason_(reason)
                ._securityToken_(dat)
                ._senderAgent_(uri)
                ._transferContract_(uri)
                .build();
        obj.setProperty(IdsConstants.IDS_WEBHOOK_ADDRESS_PROPERTY, "http://webhook");

        var resultString = objectMapper.writeValueAsString(obj);
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, RejectionMessage.class);
        assertNotNull(resultObj);
        assertEquals(token, resultObj.getAuthorizationToken());
        assertEquals(format, resultObj.getAuthorizationToken().getTokenFormat());
        assertEquals(string, resultObj.getAuthorizationToken().getTokenValue());
        assertEquals(string, resultObj.getContentVersion());
        assertEquals(uri, resultObj.getCorrelationMessage());
        assertEquals(date, resultObj.getIssued());
        assertEquals(uri, resultObj.getIssuerConnector());
        assertEquals(string, resultObj.getModelVersion());
        assertEquals(uri, resultObj.getRecipientAgent().get(0));
        assertEquals(uri, resultObj.getRecipientConnector().get(0));
        assertEquals(reason, resultObj.getRejectionReason());
        assertEquals(dat, resultObj.getSecurityToken());
        assertEquals(format, resultObj.getSecurityToken().getTokenFormat());
        assertEquals(string, resultObj.getSecurityToken().getTokenValue());
        assertEquals(uri, resultObj.getSenderAgent());
        assertEquals(uri, resultObj.getTransferContract());

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_artifact() throws IOException {
        var resultString = objectMapper.writeValueAsString(getArtifact());
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, Artifact.class);
        assertNotNull(resultObj);

        assertEquals(bigInt, (resultObj).getByteSize());
        assertEquals(string, (resultObj).getCheckSum());
        assertEquals(bigDecimal, (resultObj).getDuration());
        assertEquals(date, (resultObj).getCreationDate());
        assertEquals(string, (resultObj).getFileName());

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_representation() throws IOException {
        var resultString = objectMapper.writeValueAsString(getRepresentation());
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, Representation.class);
        assertNotNull(resultObj);

        assertEquals(date, resultObj.getCreated());
        assertEquals(1, resultObj.getDescription().size());
        assertEquals(typedLiteral, resultObj.getDescription().get(0));
        assertEquals(language, resultObj.getDescription().get(0).getLanguage());
        assertEquals(string, resultObj.getDescription().get(0).getValue());
        assertEquals(idsLanguage, resultObj.getLanguage());
        assertEquals(mediaType, resultObj.getMediaType());
        assertEquals(type, resultObj.getMediaType().getFilenameExtension());
        assertEquals(date, resultObj.getModified());
        assertEquals(uri, resultObj.getRepresentationStandard());
        assertEquals(uri, resultObj.getShapesGraph());
        assertEquals(1, resultObj.getTitle().size());
        assertEquals(typedLiteral, resultObj.getTitle().get(0));
        assertEquals(language, resultObj.getTitle().get(0).getLanguage());
        assertEquals(string, resultObj.getTitle().get(0).getValue());

        assertTrue(resultObj.getInstance().get(0) instanceof Artifact);

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_resource() throws IOException {
        var resultString = objectMapper.writeValueAsString(getResource());
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, Resource.class);
        assertNotNull(resultObj);

        assertEquals(idsFrequency, resultObj.getAccrualPeriodicity());
        assertEquals(uri, resultObj.getContentStandard());
        assertEquals(idsContentType, resultObj.getContentType());
        assertEquals(date, resultObj.getCreated());
        assertEquals(uri, resultObj.getCustomLicense());
        assertEquals(typedLiteral, resultObj.getDescription().get(0));
        assertEquals(typedLiteral, resultObj.getKeyword().get(0));
        assertEquals(idsLanguage, resultObj.getLanguage().get(0));
        assertEquals(date, resultObj.getModified());
        assertEquals(idsPaymentModality, resultObj.getPaymentModality());
        assertEquals(uri, resultObj.getPublisher());
        assertEquals(uri, resultObj.getShapesGraph());
        assertEquals(uri, resultObj.getSovereign());
        assertEquals(uri, resultObj.getStandardLicense());
        assertEquals(idsFrequency, resultObj.getTemporalResolution());
        assertEquals(uri, resultObj.getTheme().get(0));
        assertEquals(typedLiteral, resultObj.getTitle().get(0));
        assertEquals(string, resultObj.getVersion());

        assertNotNull(resultObj.getRepresentation().get(0));

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_catalog() throws IOException {
        var resultString = objectMapper.writeValueAsString(getCatalog());
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, ResourceCatalog.class);
        assertNotNull(resultObj);
        assertEquals(1, resultObj.getOfferedResource().size());

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_connector() throws IOException {
        var resultString = objectMapper.writeValueAsString(getConnector());
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, BaseConnector.class);
        assertNotNull(resultObj);

        assertEquals(authInfo, resultObj.getAuthInfo());
        assertEquals(uri, resultObj.getCurator());
        assertEquals(typedLiteral, resultObj.getDescription().get(0));
        assertEquals(uri, resultObj.getHasAgent().get(0));
        assertEquals(string, resultObj.getInboundModelVersion().get(0));
        assertEquals(uri, resultObj.getMaintainer());
        assertEquals(string, resultObj.getOutboundModelVersion());
        assertEquals(typedLiteral, resultObj.getTitle().get(0));

        assertNotNull(resultObj.getResourceCatalog().get(0));

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void serialize_deserialize_contract() throws IOException {
        var resultString = objectMapper.writeValueAsString(getContract());
        assertNotNull(resultString);

        var resultObj = objectMapper.readValue(resultString, ContractOffer.class);
        assertNotNull(resultObj);
        assertEquals(uri, resultObj.getConsumer());
        assertEquals(uri, resultObj.getProvider());

        var permission = resultObj.getPermission().get(0);
        assertNotNull(permission);
        assertEquals(action, permission.getAction().get(0));
        assertEquals(uri, permission.getTarget());
        assertEquals(uri, permission.getAssignee().get(0));
        assertEquals(uri, permission.getAssigner().get(0));

        var constraint = permission.getConstraint().get(0);
        assertNotNull(constraint);
        assertTrue(constraint instanceof IdsConstraintImpl);
        assertThat(((IdsConstraintImpl) constraint).getLeftOperand()).isNull();
        assertThat(constraint.getId()).isEqualTo(uri);
        assertThat(((IdsConstraintImpl) constraint).getLeftOperandAsString()).isEqualTo(leftOperand.name());
        assertThat(((IdsConstraintImpl) constraint).getOperator()).isEqualTo(binaryOperator);
        assertThat(((IdsConstraintImpl) constraint).getPipEndpoint()).isEqualTo(uri);
        assertThat(((IdsConstraintImpl) constraint).getRightOperand().getType()).isEqualTo(string);
        assertThat(((IdsConstraintImpl) constraint).getRightOperand().getValue()).isEqualTo(string);
        assertThat(((IdsConstraintImpl) constraint).getRightOperandReference()).isNull();
        assertThat(((IdsConstraintImpl) constraint).getUnit()).isEqualTo(uri);

        assertEquals(resultString, objectMapper.writeValueAsString(resultObj));
    }

    @Test
    void logicalConstraint() throws JsonProcessingException {
        var resource = new RdfResource();
        resource.setValue(string);
        resource.setType(string);

        var constraint = new IdsConstraintBuilder(uri)
                .leftOperand(leftOperand.name())
                .operator(binaryOperator)
                .rightOperand(resource)
                .unit(uri)
                .pipEndpoint(uri)
                .build();
        var orConstraint = new LogicalConstraintBuilder()._or_(constraint).build();

        var resultString = objectMapper.writeValueAsString(orConstraint);
        var logicalConstraint = objectMapper.readValue(resultString, LogicalConstraint.class);

        assertThat(logicalConstraint.getOr()).hasSize(1);
    }

    // build objects

    private ContractOffer getContract() {
        return new ContractOfferBuilder()
                ._consumer_(uri)
                ._provider_(uri)
                ._permission_(getPermission())
                .build();
    }

    private Permission getPermission() {
        var resource = new RdfResource();
        resource.setValue(string);
        resource.setType(string);

        var constraint = (IdsConstraintImpl) new IdsConstraintBuilder(uri)
                .leftOperand(leftOperand.name())
                .operator(binaryOperator)
                .rightOperand(resource)
                .unit(uri)
                .pipEndpoint(uri)
                .build();

        return new PermissionBuilder()
                ._action_(action)
                ._constraint_(constraint)
                ._assignee_(uri)
                ._assigner_(uri)
                ._target_(uri)
                .build();
    }

    private BaseConnector getConnector() {
        return new BaseConnectorBuilder()
                ._authInfo_(authInfo)
                ._curator_(uri)
                ._description_(typedLiteral)
                ._hasAgent_(uri)
                ._inboundModelVersion_(string)
                ._maintainer_(uri)
                ._outboundModelVersion_(string)
                ._resourceCatalog_(getCatalog())
                ._title_(typedLiteral)
                .build();
    }

    private ResourceCatalog getCatalog() {
        return new ResourceCatalogBuilder()._offeredResource_(getResource()).build();
    }

    private Artifact getArtifact() {
        var obj = new ArtifactBuilder()
                ._byteSize_(bigInt)
                ._checkSum_(string)
                ._duration_(bigDecimal)
                ._creationDate_(date)
                ._fileName_(string)
                .build();
        obj.setProperty("key", "value");
        return obj;
    }

    private Representation getRepresentation() {
        var obj = new RepresentationBuilder()
                ._created_(date)
                ._description_(typedLiteral)
                ._instance_(getArtifact())
                ._language_(idsLanguage)
                ._mediaType_(mediaType)
                ._modified_(date)
                ._representationStandard_(uri)
                ._shapesGraph_(uri)
                ._title_(typedLiteral)
                .build();
        obj.setProperty("key", "value");
        return obj;
    }

    private Resource getResource() {
        var obj = new ResourceBuilder()
                ._accrualPeriodicity_(idsFrequency)
                ._contentStandard_(uri)
                ._contentType_(idsContentType)
                ._contractOffer_(getContract())
                ._created_(date)
                ._customLicense_(uri)
                ._description_(typedLiteral)
                ._keyword_(typedLiteral)
                ._language_(idsLanguage)
                ._modified_(date)
                ._paymentModality_(idsPaymentModality)
                ._publisher_(uri)
                ._representation_(getRepresentation())
                ._shapesGraph_(uri)
                ._sovereign_(uri)
                ._standardLicense_(uri)
                ._temporalResolution_(idsFrequency)
                ._theme_(uri)
                ._title_(typedLiteral)
                ._version_(string)
                .build();
        obj.setProperty("key", "value");
        return obj;
    }
}
