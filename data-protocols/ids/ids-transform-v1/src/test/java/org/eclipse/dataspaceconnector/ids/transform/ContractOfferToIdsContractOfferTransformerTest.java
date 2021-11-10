package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.ContractOffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

public class ContractOfferToIdsContractOfferTransformerTest {
    private static final URI OFFER_ID = URI.create("urn:offer:456uz984390236s");
    private static final URI PROVIDER_URI = URI.create("https://provider.com/");

    // subject
    private ContractOfferToIdsContractOfferTransformer contractOfferToIdsContractOfferTransformer;

    // mocks
    private Policy policy;
    private ContractOffer contractOffer;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        contractOfferToIdsContractOfferTransformer = new ContractOfferToIdsContractOfferTransformer();
        contractOffer = EasyMock.createMock(ContractOffer.class);
        policy = EasyMock.createMock(Policy.class);
        context = EasyMock.createMock(TransformerContext.class);

        EasyMock.expect(contractOffer.getPolicy()).andReturn(policy);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(contractOffer, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            contractOfferToIdsContractOfferTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(contractOffer, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            contractOfferToIdsContractOfferTransformer.transform(contractOffer, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(contractOffer, context);

        var result = contractOfferToIdsContractOfferTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // prepare

        Permission edcPermission = EasyMock.createMock(Permission.class);
        de.fraunhofer.iais.eis.Permission idsPermission = EasyMock.createMock(de.fraunhofer.iais.eis.Permission.class);
        Prohibition edcProhibition = EasyMock.createMock(Prohibition.class);
        de.fraunhofer.iais.eis.Prohibition idsProhibition = EasyMock.createMock(de.fraunhofer.iais.eis.Prohibition.class);
        Duty edcObligation = EasyMock.createMock(Duty.class);
        de.fraunhofer.iais.eis.Duty idsObligation = EasyMock.createMock(de.fraunhofer.iais.eis.Duty.class);

        EasyMock.expect(contractOffer.getProvider()).andReturn(PROVIDER_URI);
        EasyMock.expect(contractOffer.getConsumer()).andReturn(null);
        EasyMock.expect(contractOffer.getContractStart()).andReturn(null);
        EasyMock.expect(contractOffer.getContractEnd()).andReturn(null);
        EasyMock.expect(policy.getPermissions()).andReturn(Collections.singletonList(edcPermission)).times(2);
        EasyMock.expect(policy.getProhibitions()).andReturn(Collections.singletonList(edcProhibition)).times(2);
        EasyMock.expect(policy.getObligations()).andReturn(Collections.singletonList(edcObligation)).times(2);

        EasyMock.expect(context.transform(EasyMock.anyObject(Permission.class), EasyMock.eq(de.fraunhofer.iais.eis.Permission.class))).andReturn(idsPermission);
        EasyMock.expect(context.transform(EasyMock.anyObject(Prohibition.class), EasyMock.eq(de.fraunhofer.iais.eis.Prohibition.class))).andReturn(idsProhibition);
        EasyMock.expect(context.transform(EasyMock.anyObject(Duty.class), EasyMock.eq(de.fraunhofer.iais.eis.Duty.class))).andReturn(idsObligation);
        EasyMock.expect(context.transform(EasyMock.isA(IdsId.class), EasyMock.eq(URI.class))).andReturn(OFFER_ID);

        context.reportProblem(EasyMock.anyString());
        EasyMock.expectLastCall().atLeastOnce();

        // record
        EasyMock.replay(contractOffer, policy, context);

        // invoke
        var result = contractOfferToIdsContractOfferTransformer.transform(contractOffer, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(OFFER_ID, result.getId());
        Assertions.assertEquals(PROVIDER_URI, result.getProvider());
        Assertions.assertEquals(1, result.getObligation().size());
        Assertions.assertEquals(idsObligation, result.getObligation().get(0));
        Assertions.assertEquals(1, result.getPermission().size());
        Assertions.assertEquals(idsPermission, result.getPermission().get(0));
        Assertions.assertEquals(1, result.getProhibition().size());
        Assertions.assertEquals(idsProhibition, result.getProhibition().get(0));
    }
}
