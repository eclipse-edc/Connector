package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

public class ProhibitionToProhibitionTransformerTest {

    private static final String TARGET = "https://target.com";
    private static final String ASSIGNER = "https://assigner.com";
    private static final String ASSIGNEE = "https://assignee.com";

    // subject
    private ProhibitionToProhibitionTransformer prohibitionToProhibitionTransformer;

    // mocks
    private Prohibition prohibition;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        prohibitionToProhibitionTransformer = new ProhibitionToProhibitionTransformer();
        prohibition = EasyMock.createMock(Prohibition.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(prohibition, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            prohibitionToProhibitionTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(prohibition, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            prohibitionToProhibitionTransformer.transform(prohibition, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(prohibition, context);

        var result = prohibitionToProhibitionTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // prepare
        Action edcAction = EasyMock.createMock(Action.class);
        de.fraunhofer.iais.eis.Action idsAction = de.fraunhofer.iais.eis.Action.READ;
        Constraint edcConstraint = EasyMock.createMock(Constraint.class);
        de.fraunhofer.iais.eis.Constraint idsConstraint = EasyMock.createMock(de.fraunhofer.iais.eis.Constraint.class);

        EasyMock.expect(prohibition.getTarget()).andReturn(TARGET);
        EasyMock.expect(prohibition.getAssigner()).andReturn(ASSIGNER);
        EasyMock.expect(prohibition.getAssignee()).andReturn(ASSIGNEE);

        EasyMock.expect(prohibition.getConstraints()).andReturn(Collections.singletonList(edcConstraint));
        EasyMock.expect(prohibition.getAction()).andReturn(edcAction);
        EasyMock.expect(context.transform(EasyMock.eq(edcAction), EasyMock.eq(de.fraunhofer.iais.eis.Action.class))).andReturn(idsAction);
        EasyMock.expect(context.transform(EasyMock.eq(edcConstraint), EasyMock.eq(de.fraunhofer.iais.eis.Constraint.class))).andReturn(idsConstraint);
        EasyMock.expect(context.transform(EasyMock.isA(IdsId.class), EasyMock.eq(URI.class))).andReturn(URI.create("https://example.com/")).anyTimes();

        // record
        EasyMock.replay(prohibition, context);

        // invoke
        var result = prohibitionToProhibitionTransformer.transform(prohibition, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getAssigner().size());
        Assertions.assertEquals(1, result.getAssignee().size());
        Assertions.assertEquals(1, result.getAction().size());
        Assertions.assertEquals(idsAction, result.getAction().get(0));
        Assertions.assertEquals(1, result.getConstraint().size());
        Assertions.assertEquals(idsConstraint, result.getConstraint().get(0));
    }

    @AfterEach
    void teardown() {
        EasyMock.verify(prohibition, context);
    }
}
