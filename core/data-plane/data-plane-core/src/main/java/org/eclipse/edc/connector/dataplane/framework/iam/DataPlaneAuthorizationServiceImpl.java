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

package org.eclipse.edc.connector.dataplane.framework.iam;

import org.eclipse.edc.connector.dataplane.spi.Endpoint;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessControlService;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAccessTokenService;
import org.eclipse.edc.connector.dataplane.spi.iam.DataPlaneAuthorizationService;
import org.eclipse.edc.connector.dataplane.spi.iam.PublicEndpointGeneratorService;
import org.eclipse.edc.jwt.spi.JwtRegisteredClaimNames;
import org.eclipse.edc.spi.iam.TokenParameters;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import static org.eclipse.edc.spi.result.Result.success;

public class DataPlaneAuthorizationServiceImpl implements DataPlaneAuthorizationService {
    public static final String CLAIM_AGREEMENT_ID = "agreement_id";
    public static final String CLAIM_ASSET_ID = "asset_id";
    public static final String CLAIM_PROCESS_ID = "process_id";
    public static final String CLAIM_TRANSFER_TYPE = "transfer_type";
    private final DataPlaneAccessTokenService accessTokenService;
    private final PublicEndpointGeneratorService endpointGenerator;
    private final DataPlaneAccessControlService accessControlService;
    private final String ownParticipantId;
    private final Clock clock;

    public DataPlaneAuthorizationServiceImpl(DataPlaneAccessTokenService accessTokenService,
                                             PublicEndpointGeneratorService endpointGenerator,
                                             DataPlaneAccessControlService accessControlService,
                                             String ownParticipantId,
                                             Clock clock) {
        this.accessTokenService = accessTokenService;
        this.endpointGenerator = endpointGenerator;
        this.accessControlService = accessControlService;
        this.ownParticipantId = ownParticipantId;
        this.clock = clock;
    }

    @Override
    public Result<DataAddress> createEndpointDataReference(DataFlowStartMessage message) {
        var endpoint = endpointGenerator.generateFor(message.getSourceDataAddress());

        return endpoint.compose(e -> accessTokenService.obtainToken(createTokenParams(message), message.getSourceDataAddress()))
                .compose(tokenRepresentation -> createDataAddress(tokenRepresentation, endpoint.getContent()));
    }

    @Override
    public Result<DataAddress> authorize(String token, Map<String, Object> requestData) {
        var accessTokenDataResult = accessTokenService.resolve(token);

        return accessTokenDataResult
                .compose(atd -> accessControlService.checkAccess(atd.claimToken(), atd.dataAddress(), requestData))
                .map(u -> accessTokenDataResult.getContent().dataAddress());
    }

    private Result<DataAddress> createDataAddress(TokenRepresentation tokenRepresentation, Endpoint publicEndpoint) {
        var address = DataAddress.Builder.newInstance()
                .type(publicEndpoint.endpointType())
                .property("endpoint", publicEndpoint.endpoint())
                .property("endpointType", publicEndpoint.endpointType()) //this is duplicated in the type() field, but will make serialization easier
                .properties(tokenRepresentation.getAdditional()) // would contain the "authType = bearer" entry
                .property("authorization", tokenRepresentation.getToken())
                .build();

        return success(address);
    }

    private TokenParameters createTokenParams(DataFlowStartMessage message) {
        return TokenParameters.Builder.newInstance()
                .claims(JwtRegisteredClaimNames.JWT_ID, UUID.randomUUID().toString())
                .claims(JwtRegisteredClaimNames.AUDIENCE, message.getParticipantId())
                .claims(JwtRegisteredClaimNames.ISSUER, ownParticipantId)
                .claims(JwtRegisteredClaimNames.SUBJECT, ownParticipantId)
                .claims(JwtRegisteredClaimNames.ISSUED_AT, clock.instant().toEpochMilli()) // todo: milli or second?
                .claims(CLAIM_AGREEMENT_ID, message.getAgreementId())
                .claims(CLAIM_ASSET_ID, message.getAssetId())
                .claims(CLAIM_PROCESS_ID, message.getProcessId())
                .claims(CLAIM_TRANSFER_TYPE, message.getTransferType())
                .build();
    }

}
