package org.eclipse.dataspaceconnector.contract.validation;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionResult;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class ContractValidationServiceImplTest {
    private ContractValidationServiceImpl validationService;
    private ParticipantAgentService agentService;
    private ContractDefinitionService definitionService;
    private AssetIndex assetIndex;

    @Test
    void verifyContractOfferValidation() {
        EasyMock.expect(agentService.createFor(EasyMock.isA(ClaimToken.class))).andReturn(new ParticipantAgent(emptyMap(), emptyMap()));

        var originalPolicy = Policy.Builder.newInstance().build();

        var newPolicy = Policy.Builder.newInstance().build();

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(Policy.Builder.newInstance().build())
                .contractPolicy(newPolicy)
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();
        var definitionResult = new ContractDefinitionResult(contractDefinition);

        EasyMock.expect(definitionService.definitionFor(EasyMock.isA(ParticipantAgent.class), EasyMock.eq("1"))).andReturn(definitionResult);

        //noinspection unchecked
        EasyMock.expect(assetIndex.queryAssets(EasyMock.isA(List.class))).andReturn(Stream.of());

        EasyMock.replay(agentService, definitionService, assetIndex);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = ContractOffer.Builder.newInstance().policy(originalPolicy).id("1:2").build();

        var result = validationService.validate(claimToken, offer);

        assertThat(result.getValidatedOffer()).isNotNull();
        assertThat(result.getValidatedOffer().getPolicy()).isNotSameAs(originalPolicy); // verify the returned policy is the sanitized one

        EasyMock.verify(agentService, definitionService, assetIndex);
    }

    @Test
    void verifyContractAgreementValidation() {
        EasyMock.expect(agentService.createFor(EasyMock.isA(ClaimToken.class))).andReturn(new ParticipantAgent(emptyMap(), emptyMap()));

        var originalPolicy = Policy.Builder.newInstance().build();

        var newPolicy = Policy.Builder.newInstance().build();

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(Policy.Builder.newInstance().build())
                .contractPolicy(newPolicy)
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();
        var definitionResult = new ContractDefinitionResult(contractDefinition);

        EasyMock.expect(definitionService.definitionFor(EasyMock.isA(ParticipantAgent.class), EasyMock.eq("1"))).andReturn(definitionResult);

        EasyMock.replay(agentService, definitionService, assetIndex);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var agreement = ContractAgreement.Builder.newInstance().id("1")
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .policy(originalPolicy)
                .contractSigningDate(LocalDate.MIN.toEpochDay())
                .contractStartDate(LocalDate.MIN.toEpochDay())
                .contractEndDate(LocalDate.MAX.toEpochDay())
                .id("1:2").build();

        assertThat(validationService.validate(claimToken, agreement)).isTrue();
        EasyMock.verify(agentService, definitionService, assetIndex);
    }

    @BeforeEach
    void setUp() {
        agentService = EasyMock.createMock(ParticipantAgentService.class);
        definitionService = EasyMock.createMock(ContractDefinitionService.class);
        assetIndex = EasyMock.createMock(AssetIndex.class);
        validationService = new ContractValidationServiceImpl(agentService, () -> definitionService, assetIndex);
    }
}
