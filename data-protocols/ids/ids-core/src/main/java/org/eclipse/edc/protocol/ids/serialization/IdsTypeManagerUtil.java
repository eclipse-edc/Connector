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

package org.eclipse.edc.protocol.ids.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iais.eis.AbstractConstraint;
import de.fraunhofer.iais.eis.Action;
import de.fraunhofer.iais.eis.Artifact;
import de.fraunhofer.iais.eis.ArtifactRequestMessage;
import de.fraunhofer.iais.eis.BaseConnector;
import de.fraunhofer.iais.eis.Constraint;
import de.fraunhofer.iais.eis.Contract;
import de.fraunhofer.iais.eis.ContractAgreement;
import de.fraunhofer.iais.eis.ContractAgreementMessage;
import de.fraunhofer.iais.eis.ContractOffer;
import de.fraunhofer.iais.eis.ContractOfferMessage;
import de.fraunhofer.iais.eis.ContractRejectionMessage;
import de.fraunhofer.iais.eis.ContractRequestMessage;
import de.fraunhofer.iais.eis.CustomMediaType;
import de.fraunhofer.iais.eis.DescriptionRequestMessage;
import de.fraunhofer.iais.eis.Duty;
import de.fraunhofer.iais.eis.DynamicAttributeToken;
import de.fraunhofer.iais.eis.LogicalConstraint;
import de.fraunhofer.iais.eis.MessageProcessedNotificationMessage;
import de.fraunhofer.iais.eis.NotificationMessage;
import de.fraunhofer.iais.eis.ParticipantUpdateMessage;
import de.fraunhofer.iais.eis.Permission;
import de.fraunhofer.iais.eis.Prohibition;
import de.fraunhofer.iais.eis.RejectionMessage;
import de.fraunhofer.iais.eis.Representation;
import de.fraunhofer.iais.eis.RequestInProcessMessage;
import de.fraunhofer.iais.eis.Resource;
import de.fraunhofer.iais.eis.ResourceCatalog;
import de.fraunhofer.iais.eis.TokenFormat;
import de.fraunhofer.iais.eis.util.TypedLiteral;
import org.eclipse.edc.protocol.ids.jsonld.JsonLd;
import org.eclipse.edc.protocol.ids.jsonld.JsonLdSerializer;
import org.eclipse.edc.protocol.ids.spi.domain.IdsConstants;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * Provides methods for ids-specific customization of the {@link TypeManager}.
 */
public final class IdsTypeManagerUtil {

    /**
     * Create JSON-LD {@link ObjectMapper} and register ids classes and custom implementations.
     *
     * @param typeManager the updated type manager.
     */
    public static void customizeTypeManager(TypeManager typeManager) {
        typeManager.registerContext("ids", JsonLd.getObjectMapper());

        registerIdsClasses(typeManager);
        registerCustomConstraintImpl(typeManager);
    }

    /**
     * Get customized ids object mapper from {@link TypeManager}.
     *
     * @param typeManager the context type manager.
     * @return customized mapper.
     */
    public static ObjectMapper getIdsObjectMapper(TypeManager typeManager) {
        customizeTypeManager(typeManager);

        return typeManager.getMapper("ids");
    }

    /**
     * Register serializers for used IDS classes.
     *
     * @param typeManager current type manager.
     */
    public static void registerIdsClasses(TypeManager typeManager) {
        // messages
        typeManager.registerSerializer("ids", ArtifactRequestMessage.class, new JsonLdSerializer<>(ArtifactRequestMessage.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", RequestInProcessMessage.class, new JsonLdSerializer<>(RequestInProcessMessage.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", MessageProcessedNotificationMessage.class, new JsonLdSerializer<>(MessageProcessedNotificationMessage.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", DescriptionRequestMessage.class, new JsonLdSerializer<>(DescriptionRequestMessage.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", NotificationMessage.class, new JsonLdSerializer<>(NotificationMessage.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", ParticipantUpdateMessage.class, new JsonLdSerializer<>(ParticipantUpdateMessage.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", RejectionMessage.class, new JsonLdSerializer<>(RejectionMessage.class, IdsConstants.CONTEXT));

        typeManager.registerSerializer("ids", ContractAgreementMessage.class, new JsonLdSerializer<>(ContractAgreementMessage.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", ContractRejectionMessage.class, new JsonLdSerializer<>(ContractRejectionMessage.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", ContractOfferMessage.class, new JsonLdSerializer<>(ContractOfferMessage.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", ContractRequestMessage.class, new JsonLdSerializer<>(ContractRequestMessage.class, IdsConstants.CONTEXT));

        typeManager.registerSerializer("ids", DynamicAttributeToken.class, new JsonLdSerializer<>(DynamicAttributeToken.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", TokenFormat.class, new JsonLdSerializer<>(TokenFormat.class, IdsConstants.CONTEXT));

        // contract/policy
        typeManager.registerSerializer("ids", ContractAgreement.class, new JsonLdSerializer<>(ContractAgreement.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", ContractOffer.class, new JsonLdSerializer<>(ContractOffer.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", Contract.class, new JsonLdSerializer<>(Contract.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", Permission.class, new JsonLdSerializer<>(Permission.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", Prohibition.class, new JsonLdSerializer<>(Prohibition.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", Duty.class, new JsonLdSerializer<>(Duty.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", Action.class, new JsonLdSerializer<>(Action.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", LogicalConstraint.class, new JsonLdSerializer<>(LogicalConstraint.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", AbstractConstraint.class, new JsonLdSerializer<>(AbstractConstraint.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", Constraint.class, new JsonLdSerializer<>(Constraint.class, IdsConstants.CONTEXT));

        // connector/offer
        typeManager.registerSerializer("ids", Artifact.class, new JsonLdSerializer<>(Artifact.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", BaseConnector.class, new JsonLdSerializer<>(BaseConnector.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", Representation.class, new JsonLdSerializer<>(Representation.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", Resource.class, new JsonLdSerializer<>(Resource.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", TypedLiteral.class, new JsonLdSerializer<>(TypedLiteral.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", ResourceCatalog.class, new JsonLdSerializer<>(ResourceCatalog.class, IdsConstants.CONTEXT));
        typeManager.registerSerializer("ids", CustomMediaType.class, new JsonLdSerializer<>(CustomMediaType.class, IdsConstants.CONTEXT));
    }

    private static void registerCustomConstraintImpl(TypeManager typeManager) {
        typeManager.registerSerializer("ids", IdsConstraintImpl.class, new JsonLdSerializer<>(IdsConstraintImpl.class, IdsConstants.CONTEXT));
        typeManager.registerTypes("ids", IdsConstraintImpl.class);
    }
}
