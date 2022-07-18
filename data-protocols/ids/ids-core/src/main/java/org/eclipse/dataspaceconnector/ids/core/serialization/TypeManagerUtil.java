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

package org.eclipse.dataspaceconnector.ids.core.serialization;

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
import org.eclipse.dataspaceconnector.ids.spi.domain.DefaultValues;
import org.eclipse.dataspaceconnector.serializer.JsonLdSerializer;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;

/**
 * Provides methods for ids-specific customization of the {@link TypeManager}.
 */
public final class TypeManagerUtil {

    /**
     * Register serializers for used IDS classes.
     *
     * @param typeManager current type manager.
     */
    public static void registerIdsClasses(TypeManager typeManager) {
        // messages
        typeManager.registerSerializer("ids", ArtifactRequestMessage.class, new JsonLdSerializer<>(ArtifactRequestMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", RequestInProcessMessage.class, new JsonLdSerializer<>(RequestInProcessMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", MessageProcessedNotificationMessage.class, new JsonLdSerializer<>(MessageProcessedNotificationMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", DescriptionRequestMessage.class, new JsonLdSerializer<>(DescriptionRequestMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", NotificationMessage.class, new JsonLdSerializer<>(NotificationMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ParticipantUpdateMessage.class, new JsonLdSerializer<>(ParticipantUpdateMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", RejectionMessage.class, new JsonLdSerializer<>(RejectionMessage.class, DefaultValues.CONTEXT));

        typeManager.registerSerializer("ids", ContractAgreementMessage.class, new JsonLdSerializer<>(ContractAgreementMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ContractRejectionMessage.class, new JsonLdSerializer<>(ContractRejectionMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ContractOfferMessage.class, new JsonLdSerializer<>(ContractOfferMessage.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ContractRequestMessage.class, new JsonLdSerializer<>(ContractRequestMessage.class, DefaultValues.CONTEXT));

        typeManager.registerSerializer("ids", DynamicAttributeToken.class, new JsonLdSerializer<>(DynamicAttributeToken.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", TokenFormat.class, new JsonLdSerializer<>(TokenFormat.class, DefaultValues.CONTEXT));

        // contract/policy
        typeManager.registerSerializer("ids", ContractAgreement.class, new JsonLdSerializer<>(ContractAgreement.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ContractOffer.class, new JsonLdSerializer<>(ContractOffer.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Contract.class, new JsonLdSerializer<>(Contract.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Permission.class, new JsonLdSerializer<>(Permission.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Prohibition.class, new JsonLdSerializer<>(Prohibition.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Duty.class, new JsonLdSerializer<>(Duty.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Action.class, new JsonLdSerializer<>(Action.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", LogicalConstraint.class, new JsonLdSerializer<>(LogicalConstraint.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", AbstractConstraint.class, new JsonLdSerializer<>(AbstractConstraint.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Constraint.class, new JsonLdSerializer<>(Constraint.class, DefaultValues.CONTEXT));

        // connector/offer
        typeManager.registerSerializer("ids", Artifact.class, new JsonLdSerializer<>(Artifact.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", BaseConnector.class, new JsonLdSerializer<>(BaseConnector.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Representation.class, new JsonLdSerializer<>(Representation.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", Resource.class, new JsonLdSerializer<>(Resource.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", TypedLiteral.class, new JsonLdSerializer<>(TypedLiteral.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", ResourceCatalog.class, new JsonLdSerializer<>(ResourceCatalog.class, DefaultValues.CONTEXT));
        typeManager.registerSerializer("ids", CustomMediaType.class, new JsonLdSerializer<>(CustomMediaType.class, DefaultValues.CONTEXT));

        registerCustomConstraintImpl(typeManager);
    }

    private static void registerCustomConstraintImpl(TypeManager typeManager) {
        typeManager.registerSerializer("ids", IdsConstraintImpl.class, new JsonLdSerializer<>(IdsConstraintImpl.class, DefaultValues.CONTEXT));
        typeManager.registerTypes("ids", IdsConstraintImpl.class);
    }
}
