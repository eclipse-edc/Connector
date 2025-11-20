/*
 *  Copyright (c) 2025 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.participantcontext.spi.config;

import org.eclipse.edc.spi.EdcException;

public interface ParticipantContextConfig {

    /**
     * Returns the String representation of the value
     *
     * @param participantContextId the participant context identifier
     * @param key                  of the setting
     * @return a String representation of the setting
     * @throws EdcException if no setting is found
     */
    String getString(String participantContextId, String key);

    /**
     * Returns the String representation of the value, or the default one if not found
     *
     * @param participantContextId the participant context identifier
     * @param defaultValue         to return if the setting is not found
     * @param key                  of the setting
     * @return a String representation of the setting
     */
    String getString(String participantContextId, String key, String defaultValue);

    /**
     * Returns the Integer representation of the value
     *
     * @param participantContextId the participant context identifier
     * @param key                  of the setting
     * @return an Integer representation of the setting
     * @throws EdcException if no setting is found, or if it's not parsable
     */
    Integer getInteger(String participantContextId, String key);

    /**
     * Returns the Integer representation of the value, or the default one if not found
     *
     * @param participantContextId the participant context identifier
     * @param key                  of the setting
     * @param defaultValue         to return if the setting is not found
     * @return an Integer representation of the setting
     * @throws EdcException if the value it's not parsable
     */
    Integer getInteger(String participantContextId, String key, Integer defaultValue);

    /**
     * Returns the Long representation of the value
     *
     * @param participantContextId the participant context identifier
     * @param key                  of the setting
     * @return a Long representation of the setting
     * @throws EdcException if no setting is found, or if it's not parsable
     */
    Long getLong(String participantContextId, String key);

    /**
     * Returns the Long representation of the value, or the default one if not found
     *
     * @param participantContextId the participant context identifier
     * @param key                  of the setting
     * @param defaultValue         to return if the setting is not found
     * @return a Long representation of the setting
     * @throws EdcException if the value it's not parsable
     */
    Long getLong(String participantContextId, String key, Long defaultValue);

    /**
     * Returns the Boolean representation of the value
     *
     * @param participantContextId the participant context identifier
     * @param key                  of the setting
     * @return a Boolean representation of the setting
     * @throws EdcException if no setting is found, or if it's not parsable
     */
    Boolean getBoolean(String participantContextId, String key);

    /**
     * Returns the Boolean representation of the value, or the default one if not found
     *
     * @param participantContextId the participant context identifier
     * @param key                  of the setting
     * @param defaultValue         to return if the setting is not found
     * @return a Boolean representation of the setting
     * @throws EdcException if the value it's not parsable
     */
    Boolean getBoolean(String participantContextId, String key, Boolean defaultValue);

    /**
     * Returns the sensitive String representation of the value
     *
     * @param participantContextId the participant context identifier
     * @param key                  of the setting
     * @return a String representation of the setting
     * @throws EdcException if no setting is found
     */
    String getStringSensitive(String participantContextId, String key);
}
