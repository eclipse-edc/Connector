/*
 *  Copyright (c) 2021 Daimler TSS GmbH
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - Initial API and Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.IdsId;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;

class DutyToDutyTransformerTest {

    private static final URI PERMISSION_ID = URI.create("urn:permission:456uz984390236s");
    private static final String TARGET = "https://target.com";
    private static final URI TARGET_URI = URI.create(TARGET);
    private static final String ASSIGNER = "https://assigner.com";
    private static final URI ASSIGNER_URI = URI.create(ASSIGNER);
    private static final String ASSIGNEE = "https://assignee.com";
    private static final URI ASSIGNEE_URI = URI.create(ASSIGNEE);

    // subject
    private DutyToDutyTransformer transformer;

    // mocks
    private Duty duty;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new DutyToDutyTransformer();
        duty = EasyMock.createMock(Duty.class);
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(duty, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(duty, context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(duty, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(duty, context);

        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulMap() {
        // prepare
        Action edcAction = EasyMock.createMock(Action.class);
        de.fraunhofer.iais.eis.Action idsAction = de.fraunhofer.iais.eis.Action.READ;
        Constraint edcConstraint = EasyMock.createMock(Constraint.class);
        de.fraunhofer.iais.eis.Constraint idsConstraint = EasyMock.createMock(de.fraunhofer.iais.eis.Constraint.class);

        EasyMock.expect(duty.getTarget()).andReturn(TARGET);
        EasyMock.expect(duty.getAssigner()).andReturn(ASSIGNER);
        EasyMock.expect(duty.getAssignee()).andReturn(ASSIGNEE);

        EasyMock.expect(duty.getConstraints()).andReturn(Collections.singletonList(edcConstraint));
        EasyMock.expect(duty.getAction()).andReturn(edcAction);
        EasyMock.expect(context.transform(EasyMock.eq(edcAction), EasyMock.eq(de.fraunhofer.iais.eis.Action.class))).andReturn(idsAction);
        EasyMock.expect(context.transform(EasyMock.eq(edcConstraint), EasyMock.eq(de.fraunhofer.iais.eis.Constraint.class))).andReturn(idsConstraint);
        EasyMock.expect(context.transform(EasyMock.isA(IdsId.class), EasyMock.eq(URI.class))).andReturn(PERMISSION_ID);

        // record
        EasyMock.replay(duty, context);

        // invoke
        var result = transformer.transform(duty, context);

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
    void tearDown() {
        EasyMock.verify(duty, context);
    }

}
