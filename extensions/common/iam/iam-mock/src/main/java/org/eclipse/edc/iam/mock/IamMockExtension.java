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
 *       Cofinity-X - make participant id extraction dependent on dataspace profile context
 *
 */

package org.eclipse.edc.iam.mock;

import org.eclipse.edc.participantcontext.spi.config.ParticipantContextConfig;
import org.eclipse.edc.protocol.spi.DefaultParticipantIdExtractionFunction;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.iam.AudienceResolver;
import org.eclipse.edc.spi.iam.IdentityService;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;

/**
 * An IAM provider mock used for testing.
 */
@Provides(IdentityService.class)
@Extension(value = IamMockExtension.NAME)
public class IamMockExtension implements ServiceExtension {

    public static final String NAME = "Mock IAM";

    public static final String DEFAULT_MOCK_REGION = "eu";
    @Setting(description = "Configures the region to be used in the mock tokens", defaultValue = DEFAULT_MOCK_REGION)
    public static final String EDC_MOCK_REGION = "edc.mock.region";

    @Setting(description = "Configures the participant id this runtime is operating on behalf of")
    public static final String PARTICIPANT_ID = "edc.participant.id";

    public static final String DEFAULT_FAULTY_CLIENT_ID = "faultyClientId";
    @Setting(description = "Configures the faulty participant id that the requests to fail (for testing purposes)", defaultValue = DEFAULT_FAULTY_CLIENT_ID)
    public static final String EDC_MOCK_FAULTY_CLIENT_ID = "edc.mock.faulty_client_id";

    public static final String DEFAULT_IDENTITY_CLAIM_KEY = "client_id";

    @Setting(key = "edc.agent.identity.key", description = "The name of the claim key used to determine the participant identity", defaultValue = DEFAULT_IDENTITY_CLAIM_KEY)
    private String agentIdentityKey;
    @Inject
    private TypeManager typeManager;

    @Inject
    private ParticipantContextConfig contextConfig;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        context.registerService(IdentityService.class, new MockIdentityService(contextConfig, typeManager));
    }

    @Provider(isDefault = true)
    public DefaultParticipantIdExtractionFunction defaultParticipantIdExtractionFunction() {
        return new MockParticipantIdExtractionFunction(agentIdentityKey);
    }

    @Provider
    public AudienceResolver audienceResolver() {
        return (msg) -> Result.success(msg.getCounterPartyAddress());
    }
}
