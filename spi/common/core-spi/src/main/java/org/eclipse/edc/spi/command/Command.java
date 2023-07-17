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

package org.eclipse.edc.spi.command;

/**
 * Base class for all command objects. Contains basic information such as a command ID
 * and an error count, which indicates how many times a particular command has already errored out. This is useful
 * if the command should be discarded after a few retries.
 * <p>
 * Please take note of the following guidelines:
 * <ul>
 * <li>Commands are simple POJOs, that must be (JSON-)serializable and can therefore not have references to other services.</li>
 * <li>Commands must contain all the information that a {@link CommandHandler} requires to do its job.</li>
 * <li>Commands do not have results. Any results that an operation may produce are to be handled by the {@link CommandHandler}</li>
 * </ul>
 */
public abstract class Command {

}
