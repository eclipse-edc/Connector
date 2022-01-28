package org.eclipse.dataspaceconnector.e2e;

import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.spi.types.TypeManager;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.agreement.ContractAgreement;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractOffer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.dataspaceconnector.policy.model.PolicyType.SET;
import static org.eclipse.dataspaceconnector.spi.types.domain.contract.negotiation.ContractOfferRequest.Type.INITIAL;
import static org.hamcrest.Matchers.notNullValue;

@IntegrationTest
public class IdsEndToEndTest {

    @Test
    void verifyContractNegotiationBetweenTwoConnectors() {
        var consumer = new Connector("http://localhost:8181");
        var provider = new Connector("http://localhost:9191");

        await().atMost(60, SECONDS).untilAsserted(() -> {
            consumer.checkHealth();
            provider.checkHealth();
        });

        String negotiationId = consumer.createNegotiation("provider", "http://provider:8181", "assetId");

        var contractAgreement = consumer.getContractAgreement(negotiationId);

        assertThat(contractAgreement.getId()).isNotNull();
    }

    private static class Connector {

        private final String baseUri;

        public Connector(String baseUri) {
            this.baseUri = baseUri;
        }

        public void checkHealth() {
            given()
                    .baseUri(baseUri)
                    .when()
                    .get("/api/check/liveness")
                    .then()
                    .statusCode(200);
        }

        public String createNegotiation(String providerId, String providerBaseUri, String assetId) {
            var request = ContractOfferRequest.Builder.newInstance()
                    .protocol("ids-multipart")
                    .connectorId(providerId)
                    .connectorAddress(providerBaseUri + "/api/ids/multipart")
                    .type(INITIAL)
                    .contractOffer(contractOfferFor(assetId))
                    .build();

            return given()
                    .baseUri(baseUri)
                    .when()
                    .contentType(JSON)
                    .body(new TypeManager().writeValueAsBytes(request))
                    .header("X-Api-Key", "123456")
                    .post("/api/control/negotiation")
                    .then()
                    .statusCode(200)
                    .extract().body().asString();
        }

        public ContractAgreement getContractAgreement(String negotiationId) {
            var agreement = new AtomicReference<ContractAgreement>();

            await().untilAsserted(() -> {
                var retrieved = given()
                        .baseUri(baseUri)
                        .when()
                        .contentType(JSON)
                        .header("X-Api-Key", "123456")
                        .get("/api/control/negotiation/{id}", negotiationId)
                        .then()
                        .statusCode(200)
                        .body("contractAgreement", notNullValue())
                        .extract().body().jsonPath()
                        .getObject("contractAgreement", ContractAgreement.class);

                agreement.set(retrieved);
            });

            return agreement.get();
        }

        @NotNull
        private ContractOffer contractOfferFor(String assetId) {
            var permission = Permission.Builder.newInstance()
                    .target(assetId)
                    .action(Action.Builder.newInstance().type("USE").build())
                    .build();

            var policy = Policy.Builder.newInstance()
                    .id("956e172f-2de1-4501-8881-057a57fd0e69")
                    .permission(permission)
                    .type(SET)
                    .build();

            return ContractOffer.Builder.newInstance()
                    .id("1:3a75736e-001d-4364-8bd4-9888490edb58")
                    .policy(policy)
                    .asset(Asset.Builder.newInstance().id(assetId).build())
                    .build();
        }
    }
}
