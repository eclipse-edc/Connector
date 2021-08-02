/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.policy.model;

/**
 * The set of supported expression operators. Not all operators may be supported for particular expression types.
 */
public enum Operator {
    EQ, NEQ, GT, GEQ, LT, LEQ, IN
}
