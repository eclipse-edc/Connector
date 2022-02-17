package org.eclipse.dataspaceconnector.contract.validation;

import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.asset.AssetIndex;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgent;
import org.eclipse.dataspaceconnector.spi.contract.agent.ParticipantAgentService;
import org.eclipse.dataspaceconnector.spi.contract.offer.ContractDefinitionService;
import org.eclipse.dataspaceconnector.spi.iam.ClaimToken;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;

import static java.time.Instant.MAX;
import static java.time.Instant.MIN;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContractValidationServiceImplTest {
    private ContractValidationServiceImpl validationService;
    private ParticipantAgentService agentService;
    private ContractDefinitionService definitionService;
    private AssetIndex assetIndex;

    @Test
    void verifyContractOfferValidation() {
        var originalPolicy = Policy.Builder.newInstance().build();
        var newPolicy = Policy.Builder.newInstance().build();
        var asset = Asset.Builder.newInstance().id("1").build();
        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(Policy.Builder.newInstance().build())
                .contractPolicy(newPolicy)
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionService.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);
        when(assetIndex.queryAssets(isA(QuerySpec.class))).thenReturn(Stream.of(asset));

        var claimToken = ClaimToken.Builder.newInstance().build();
        var offer = ContractOffer.Builder.newInstance().id("1:2")
                .asset(asset)
                .policy(originalPolicy)
                .provider(URI.create("provider"))
                .consumer(URI.create("consumer"))
                .build();

        var result = validationService.validate(claimToken, offer);

        assertThat(result.getContent()).isNotNull();
        assertThat(result.getContent().getPolicy()).isNotSameAs(originalPolicy); // verify the returned policy is the sanitized one
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(definitionService).definitionFor(isA(ParticipantAgent.class), eq("1"));
        verify(assetIndex).queryAssets(isA(QuerySpec.class));
    }

    @Test
    void verifyContractAgreementValidation() {
        var originalPolicy = Policy.Builder.newInstance().build();
        var newPolicy = Policy.Builder.newInstance().build();

        var contractDefinition = ContractDefinition.Builder.newInstance()
                .id("1")
                .accessPolicy(Policy.Builder.newInstance().build())
                .contractPolicy(newPolicy)
                .selectorExpression(AssetSelectorExpression.SELECT_ALL)
                .build();

        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));
        when(definitionService.definitionFor(isA(ParticipantAgent.class), eq("1"))).thenReturn(contractDefinition);

        var claimToken = ClaimToken.Builder.newInstance().build();
        var agreement = ContractAgreement.Builder.newInstance().id("1")
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .policy(originalPolicy)
                .asset(Asset.Builder.newInstance().build())
                .contractSigningDate(LocalDate.MIN.toEpochDay())
                .contractStartDate(LocalDate.MIN.toEpochDay())
                .contractEndDate(LocalDate.MAX.toEpochDay())
                .id("1:2").build();

        assertThat(validationService.validate(claimToken, agreement)).isTrue();
        verify(agentService).createFor(isA(ClaimToken.class));
        verify(definitionService).definitionFor(isA(ParticipantAgent.class), eq("1"));
    }

    @Test
    void verifyContractAgreementExpired() {
        var isValid =
                validateAgreementDate(MIN.getEpochSecond(), MIN.getEpochSecond(), Instant.now().getEpochSecond() - 1);

        assertThat(isValid).isFalse();
    }

    @Test
    void verifyContractAgreementNotStartedYet() {
        var isValid = validateAgreementDate(MIN.getEpochSecond(), MAX.getEpochSecond(), MAX.getEpochSecond());

        assertThat(isValid).isFalse();
    }

    @BeforeEach
    void setUp() {
        agentService = mock(ParticipantAgentService.class);
        definitionService = mock(ContractDefinitionService.class);
        assetIndex = mock(AssetIndex.class);
        validationService = new ContractValidationServiceImpl(agentService, () -> definitionService, assetIndex);
    }

    private boolean validateAgreementDate(long signingDate, long startDate, long endDate) {
        when(agentService.createFor(isA(ClaimToken.class))).thenReturn(new ParticipantAgent(emptyMap(), emptyMap()));

        var originalPolicy = Policy.Builder.newInstance().build();

        var claimToken = ClaimToken.Builder.newInstance().build();
        var agreement = ContractAgreement.Builder.newInstance().id("1")
                .providerAgentId("provider")
                .consumerAgentId("consumer")
                .policy(originalPolicy)
                .asset(Asset.Builder.newInstance().build())
                .contractSigningDate(signingDate)
                .contractStartDate(startDate)
                .contractEndDate(endDate)
                .id("1:2").build();

        return validationService.validate(claimToken, agreement);
    }
}
