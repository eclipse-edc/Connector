/*
 *  Copyright (c) 2022 Microsoft Corporation
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Microsoft Corporation - initial API and implementation
 *
 */

package org.eclipse.dataspaceconnector.extension.jersey.validation;

import java.lang.reflect.Method;

public class TestObject {
    public static Method getAnswerMethod() {
        try {
            return TestObject.class.getDeclaredMethod("whatsTheAnswer");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method getMethodWithArg() {
        try {
            return TestObject.class.getDeclaredMethod("isAnswerCorrect", Integer.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isAnswerCorrect(Integer answer) {
        return answer == whatsTheAnswer();
    }

    public int whatsTheAnswer() {
        return 42; // it is the answer to everything
    }
}
