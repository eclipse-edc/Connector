/*
 *  Copyright (c) 2021 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *       Fraunhofer-Gesellschaft zur Förderung der angewandten Forschung e.V. - Improvements
 *
 */

package org.eclipse.edc.spi.system.configuration;

import org.eclipse.edc.spi.EdcException;

import java.util.Map;
import java.util.stream.Stream;

/**
 * A Config facade that offers some utility functions to work with configuration settings based on the Java Properties format:
 * <pre>
 *   group.subgroup.key = value
 * </pre>
 * The path components are separated by a dot, and the root path is considered to be an empty string.
 */
public interface Config {

    /**
     * Returns the String representation of the value
     *
     * @param key of the setting
     * @return a String representation of the setting
     * @throws EdcException if no setting is found
     */
    String getString(String key);

    /**
     * Returns the String representation of the value, or the default one if not found
     *
     * @param key of the setting
     * @return a String representation of the setting
     */
    String getString(String key, String defaultValue);

    /**
     * Returns the Integer representation of the value
     *
     * @param key of the setting
     * @return an Integer representation of the setting
     * @throws EdcException if no setting is found, or if it's not parsable
     */
    Integer getInteger(String key);

    /**
     * Returns the Integer representation of the value, or the default one if not found
     *
     * @param key of the setting
     * @return an Integer representation of the setting
     * @throws EdcException if the value it's not parsable
     */
    Integer getInteger(String key, Integer defaultValue);

    /**
     * Returns the Long representation of the value
     *
     * @param key of the setting
     * @return a Long representation of the setting
     * @throws EdcException if no setting is found, or if it's not parsable
     */
    Long getLong(String key);

    /**
     * Returns the Long representation of the value, or the default one if not found
     *
     * @param key of the setting
     * @return a Long representation of the setting
     * @throws EdcException if the value it's not parsable
     */
    Long getLong(String key, Long defaultValue);

    /**
     * Returns the Boolean representation of the value
     *
     * @param key of the setting
     * @return a Boolean representation of the setting
     * @throws EdcException if no setting is found, or if it's not parsable
     */
    Boolean getBoolean(String key);

    /**
     * Returns the Boolean representation of the value, or the default one if not found
     *
     * @param key of the setting
     * @return a Boolean representation of the setting
     * @throws EdcException if the value it's not parsable
     */
    Boolean getBoolean(String key, Boolean defaultValue);

    /**
     * Returns the Config representation relative to the specified path.
     * The entries that are not relative to the path specified will be filtered out.
     *
     * @param path that will be appended to the root path
     * @return another Config object relative to the path specified
     */
    Config getConfig(String path);

    /**
     * Return a config that will own the union of the entries (in case of duplicates, other's one will subdue).
     * The root path will be reset.
     *
     * @param other another Config object
     * @return a Config that's the merge of the current and the other
     */
    Config merge(Config other);

    /**
     * Return a stream of configs partitioned by the current group level
     *
     * @return a stream of configs
     */
    Stream<Config> partition();

    /**
     * Returns the config entries
     *
     * @return the config entries
     */
    Map<String, String> getEntries();

    /**
     * Returns the config entries relative to the current root path.
     * e.g. if the entries contains an entry that's
     * <pre>
     *     group.key = value
     * </pre>
     * and the rootPath is "group", this method will return a map that contains this entry:
     * <pre>
     *     key = value
     * </pre>
     *
     * @return a map containing the config entries relative to the current root path
     */
    Map<String, String> getRelativeEntries();

    /**
     * Returns the config entries relative to the current root path filtering out properties that
     * does not start with basePath.
     *
     * @return a map containing the config entries relative to the current root path filtered by basePath
     */
    Map<String, String> getRelativeEntries(String basePath);

    /**
     * Returns the name of the current node.
     * e.g. for a Config with a rootPath equal to "group.subgroup" the current node is "subgroup"
     *
     * @return the name of the current node
     */
    String currentNode();

    /**
     * Checks whether a particular config is a "leaf", i.e. has no more children, or not
     */
    boolean isLeaf();

    /**
     * Checks whether a (fully qualified) config key is present or not
     */
    boolean hasKey(String key);

    /**
     * Checks whether exist at least one setting with the passed root path
     */
    boolean hasPath(String path);
}
