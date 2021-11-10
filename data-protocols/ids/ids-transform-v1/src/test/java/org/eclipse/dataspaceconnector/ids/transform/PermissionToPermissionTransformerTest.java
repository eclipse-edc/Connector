package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

public class PermissionToPermissionTransformerTest {

    private static final URI PERMISSION_ID = URI.create("urn:permission:456uz984390236s");
    private static final String TARGET = "https://target.com";
    private static final URI TARGET_URI = URI.create(TARGET);
    private static final String ASSIGNER = "https://assigner.com";
    private static final URI ASSIGNER_URI = URI.create(ASSIGNER);
    private static final String ASSIGNEE = "https://assignee.com";
    private static final URI ASSIGNEE_URI = URI.create(ASSIGNEE);

    // subject
    private PermissionToPermissionTransformer permissionToPermissionTransformer;

    // mocks
    private Permission permission;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        permissionToPermissionTransformer = new PermissionToPermissionTransformer();
        permission = EasyMock.createMock(Permission.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(permission, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            permissionToPermissionTransformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(permission, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            permissionToPermissionTransformer.transform(permission, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(permission, context);

        var result = permissionToPermissionTransformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // prepare
        Action edcAction = EasyMock.createMock(Action.class);
        de.fraunhofer.iais.eis.Action idsAction = de.fraunhofer.iais.eis.Action.READ;
        Constraint edcConstraint = EasyMock.createMock(Constraint.class);
        de.fraunhofer.iais.eis.Constraint idsConstraint = EasyMock.createMock(de.fraunhofer.iais.eis.Constraint.class);
        Duty edcDuty = EasyMock.createMock(Duty.class);
        de.fraunhofer.iais.eis.Duty idsDuty = EasyMock.createMock(de.fraunhofer.iais.eis.Duty.class);

        EasyMock.expect(permission.getTarget()).andReturn(TARGET);
        EasyMock.expect(permission.getAssigner()).andReturn(ASSIGNER);
        EasyMock.expect(permission.getAssignee()).andReturn(ASSIGNEE);

        EasyMock.expect(permission.getConstraints()).andReturn(Collections.singletonList(edcConstraint)).anyTimes();
        EasyMock.expect(permission.getDuty()).andReturn(edcDuty).anyTimes();
        EasyMock.expect(permission.getAction()).andReturn(edcAction).anyTimes();
        EasyMock.expect(context.transform(EasyMock.eq(edcAction), EasyMock.eq(de.fraunhofer.iais.eis.Action.class))).andReturn(idsAction);
        EasyMock.expect(context.transform(EasyMock.eq(edcConstraint), EasyMock.eq(de.fraunhofer.iais.eis.Constraint.class))).andReturn(idsConstraint);
        EasyMock.expect(context.transform(EasyMock.isA(IdsId.class), EasyMock.eq(URI.class))).andReturn(PERMISSION_ID);

        context.reportProblem(EasyMock.anyString());
        EasyMock.expectLastCall().once();

        // record
        EasyMock.replay(permission, context);

        // invoke
        var result = permissionToPermissionTransformer.transform(permission, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertEquals(PERMISSION_ID, result.getId());
        Assertions.assertEquals(TARGET_URI, result.getTarget());
        Assertions.assertEquals(1, result.getAssigner().size());
        Assertions.assertEquals(ASSIGNER_URI, result.getAssigner().get(0));
        Assertions.assertEquals(1, result.getAssignee().size());
        Assertions.assertEquals(ASSIGNEE_URI, result.getAssignee().get(0));
        Assertions.assertEquals(1, result.getAction().size());
        Assertions.assertEquals(idsAction, result.getAction().get(0));
        Assertions.assertEquals(1, result.getConstraint().size());
        Assertions.assertEquals(idsConstraint, result.getConstraint().get(0));
    }

    @AfterEach
    void teardown() {
        EasyMock.verify(permission, context);
    }
}
