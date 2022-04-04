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

import de.fraunhofer.iais.eis.DutyBuilder;
import org.eclipse.dataspaceconnector.ids.core.policy.IdsConstraintBuilder;
import org.eclipse.dataspaceconnector.policy.model.AtomicConstraint;
import org.eclipse.dataspaceconnector.policy.model.Constraint;
import org.eclipse.dataspaceconnector.policy.model.Duty;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class IdsPermissionToPermissionTransformerTest {

    private static final String TARGET = "urn:artifact:1";
    private static final String ACTION = "USE";
    private static final URI TARGET_URI = URI.create(TARGET);
    private static final String ASSIGNER = "https://assigner.com";
    private static final URI ASSIGNER_URI = URI.create(ASSIGNER);
    private static final String ASSIGNEE = "https://assignee.com";
    private static final URI ASSIGNEE_URI = URI.create(ASSIGNEE);

    private IdsPermissionToPermissionTransformer transformer;

    private de.fraunhofer.iais.eis.Permission idsPermission;
    private de.fraunhofer.iais.eis.Duty idsDuty;
    private de.fraunhofer.iais.eis.Constraint idsConstraint;
    private TransformerContext context;

    @BeforeEach
    void setUp() {
        transformer = new IdsPermissionToPermissionTransformer();
        idsDuty = new DutyBuilder().build();
        idsConstraint = new IdsConstraintBuilder().build();
        idsPermission = new de.fraunhofer.iais.eis.PermissionBuilder()
                ._action_(new ArrayList<>(Collections.singletonList(de.fraunhofer.iais.eis.Action.USE)))
                ._target_(TARGET_URI)
                ._preDuty_(new ArrayList<>(Collections.singletonList(idsDuty)))
                ._constraint_(new ArrayList<>(Collections.singletonList(idsConstraint)))
                ._assignee_(new ArrayList<>(Collections.singletonList(ASSIGNEE_URI)))
                ._assigner_(new ArrayList<>(Collections.singletonList(ASSIGNER_URI)))
                .build();
        context = mock(TransformerContext.class);
    }

    @Test
    void testThrowsNullPointerExceptionForAll() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(null, null);
        });
    }

    @Test
    void testThrowsNullPointerExceptionForContext() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            transformer.transform(idsPermission, null);
        });
    }

    @Test
    void testReturnsNull() {
        var result = transformer.transform(null, context);

        Assertions.assertNull(result);
    }

    @Test
    void testSuccessfulSimple() {
        var edcConstraint = AtomicConstraint.Builder.newInstance().build();
        var edcDuty = Duty.Builder.newInstance().build();

        when(context.transform(eq(idsDuty), eq(Duty.class))).thenReturn(edcDuty);
        when(context.transform(eq(idsConstraint), eq(Constraint.class))).thenReturn(edcConstraint);

        var result = transformer.transform(idsPermission, context);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getAction());
        Assertions.assertNotNull(result.getConstraints());
        Assertions.assertNotNull(result.getDuties());
        Assertions.assertEquals(ACTION, result.getAction().getType());
        Assertions.assertNotEquals(TARGET, result.getTarget());
        Assertions.assertTrue(TARGET.contains(result.getTarget()));
        Assertions.assertEquals(ASSIGNER, result.getAssigner());
        Assertions.assertEquals(ASSIGNEE, result.getAssignee());
        Assertions.assertEquals(1, result.getConstraints().size());
        Assertions.assertEquals(edcConstraint, result.getConstraints().get(0));
        Assertions.assertEquals(1, result.getDuties().size());
        Assertions.assertEquals(edcDuty, result.getDuties().get(0));
        verify(context).transform(eq(idsDuty), eq(Duty.class));
        verify(context).transform(eq(idsConstraint), eq(Constraint.class));
    }
}
