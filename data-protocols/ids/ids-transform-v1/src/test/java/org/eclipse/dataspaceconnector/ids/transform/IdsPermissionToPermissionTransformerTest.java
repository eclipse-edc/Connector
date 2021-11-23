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
 *       Daimler TSS GmbH - Initial Implementation
 *
 */

package org.eclipse.dataspaceconnector.ids.transform;

import de.fraunhofer.iais.eis.ConstraintBuilder;
import de.fraunhofer.iais.eis.DutyBuilder;
import org.easymock.EasyMock;
import org.eclipse.dataspaceconnector.ids.spi.transform.TransformerContext;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

public class IdsPermissionToPermissionTransformerTest {

    private static final String TARGET = "https://target.com";
    private static final String ACTION = "USE";
    private static final URI TARGET_URI = URI.create(TARGET);
    private static final String ASSIGNER = "https://assigner.com";
    private static final URI ASSIGNER_URI = URI.create(ASSIGNER);
    private static final String ASSIGNEE = "https://assignee.com";
    private static final URI ASSIGNEE_URI = URI.create(ASSIGNEE);

    // subject
    private IdsPermissionToPermissionTransformer transformer;

    // mocks
    private de.fraunhofer.iais.eis.Permission idsPermission;
    private de.fraunhofer.iais.eis.Duty idsDuty;
    private de.fraunhofer.iais.eis.Constraint idsConstraint;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new IdsPermissionToPermissionTransformer();
        idsDuty = new DutyBuilder().build();
        idsConstraint = new ConstraintBuilder().build();
        idsPermission = new de.fraunhofer.iais.eis.PermissionBuilder()
                ._action_(new ArrayList<>(Collections.singletonList(de.fraunhofer.iais.eis.Action.USE)))
                ._target_(TARGET_URI)
                ._preDuty_(new ArrayList<>(Collections.singletonList(idsDuty)))
                ._constraint_(new ArrayList<>(Collections.singletonList(idsConstraint)))
                ._assignee_(new ArrayList<>(Collections.singletonList(ASSIGNEE_URI)))
                ._assigner_(new ArrayList<>(Collections.singletonList(ASSIGNER_URI)))
                .build();
        context = EasyMock.createMock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        EasyMock.replay(context);

        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(idsPermission, null);
        });
    }

    @Test
    void testReturnsNull() {
        EasyMock.replay(context);

        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        // prepare
        Constraint edcConstraint = EasyMock.createMock(Constraint.class);
        Duty edcDuty = EasyMock.createMock(Duty.class);

        EasyMock.expect(context.transform(EasyMock.eq(idsDuty), EasyMock.eq(Duty.class))).andReturn(edcDuty);
        EasyMock.expect(context.transform(EasyMock.eq(idsConstraint), EasyMock.eq(Constraint.class))).andReturn(edcConstraint);

        // record
        EasyMock.replay(context);

        // invoke
        var result = transformer.transform(idsPermission, context);

        // verify
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getAction());
        Assertions.assertNotNull(result.getConstraints());
        Assertions.assertNotNull(result.getDuties());
        Assertions.assertEquals(ACTION, result.getAction().getType());
        Assertions.assertEquals(TARGET, result.getTarget());
        Assertions.assertEquals(ASSIGNER, result.getAssigner());
        Assertions.assertEquals(ASSIGNEE, result.getAssignee());
        Assertions.assertEquals(1, result.getConstraints().size());
        Assertions.assertEquals(edcConstraint, result.getConstraints().get(0));
        Assertions.assertEquals(1, result.getDuties().size());
        Assertions.assertEquals(edcDuty, result.getDuties().get(0));
    }

    @AfterEach
    void teardown() {
        EasyMock.verify(context);
    }
}
