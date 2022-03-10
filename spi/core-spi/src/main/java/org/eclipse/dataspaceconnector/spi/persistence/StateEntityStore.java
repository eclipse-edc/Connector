package org.eclipse.dataspaceconnector.spi.persistence;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Define a store that can be used within a state machine
 */
public interface StateEntityStore<T> {

    /**
     * Returns a list of entities that are in a specific state.
     * <br/>
     * Implementors MUST handle these requirements: <br/>
     * <ul>
     *     <il>
     *         * entities should be fetched from the oldest to the newest, by a timestamp that reports the last state transition on the entity
     *         <br/><br/>
     *     </il>
     *     <il>
     *         * fetched entities should be leased for a configurable timeout, that will be released after the timeout expires or when the entity will be updated.
     *         This will avoid consecutive fetches in the state machine loop
     *         <br/><br/>
     *     </il>
     * </ul>
     *
     * @param state The state that the processes of interest should be in.
     * @param max   The maximum amount of result items.
     * @return A list of entities (at most _max_) that are in the desired state.
     */
    @NotNull
    List<T> nextForState(int state, int max);

}
