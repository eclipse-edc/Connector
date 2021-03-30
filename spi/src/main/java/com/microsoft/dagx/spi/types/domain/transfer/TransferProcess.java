package com.microsoft.dagx.spi.types.domain.transfer;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Represents a data transfer process.
 */
@JsonTypeName("dagx:transferprocess")
@JsonDeserialize(builder = TransferProcess.Builder.class)
public class TransferProcess {
    private String id;

    private int state;

    private int stateCount = TransferProcessStates.UNSAVED.code();

    private long stateTimestamp;

    public String getId() {
        return id;
    }

    public int getState() {
        return state;
    }

    public int getStateCount() {
        return stateCount;
    }

    public long getStateTimestamp() {
        return stateTimestamp;
    }

    public void transitionInitial() {
        transition(TransferProcessStates.INITIAL, TransferProcessStates.UNSAVED);
    }

    public void transitionProvisioning() {
        transition(TransferProcessStates.PROVISIONING, TransferProcessStates.INITIAL, TransferProcessStates.PROVISIONING);
    }

    public void transitionProvisioned() {
        transition(TransferProcessStates.PROVISIONED, TransferProcessStates.PROVISIONING, TransferProcessStates.PROVISIONED);
    }

    public void transitionRequested() {
        transition(TransferProcessStates.REQUESTED, TransferProcessStates.PROVISIONED, TransferProcessStates.REQUESTED);
    }

    public void transitionDeprovisioning() {
        transition(TransferProcessStates.DEPROVISIONING, TransferProcessStates.REQUESTED, TransferProcessStates.DEPROVISIONING);
    }

    public void transitionDeprovisioned() {
        transition(TransferProcessStates.DEPROVISIONED, TransferProcessStates.DEPROVISIONING, TransferProcessStates.DEPROVISIONED);
    }

    public void transitionEnded() {
        transition(TransferProcessStates.ENDED, TransferProcessStates.DEPROVISIONED);
    }

    public void rollbackState(TransferProcessStates state) {
        this.state = state.code();
        stateCount = 1;
        stateTimestamp = Instant.now().toEpochMilli();
    }

    public TransferProcess copy() {
        return Builder.newInstance().id(id).state(state).stateTimestamp(stateTimestamp).stateCount(stateCount).build();
    }

    private void transition(TransferProcessStates end, TransferProcessStates... starts) {
        if (Arrays.stream(starts).noneMatch(s -> s.code() == state)) {
            throw new IllegalStateException(format("Cannot transition from state %s to %s", state, end.code()));
        }
        stateCount = state == end.code() ? stateCount + 1 : 1;
        state = end.code();
        stateTimestamp = Instant.now().toEpochMilli();
    }

    private TransferProcess() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransferProcess that = (TransferProcess) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private final TransferProcess process;

        @JsonCreator
        public static Builder newInstance() {
            return new Builder();
        }

        public TransferProcess build() {
            Objects.requireNonNull(process.id, "id");
            if (process.state == TransferProcessStates.UNSAVED.code() && process.stateTimestamp == 0) {
                process.stateTimestamp = Instant.now().toEpochMilli();
            }
            return process;
        }

        public Builder id(String id) {
            process.id = id;
            return this;
        }

        public Builder state(int value) {
            process.state = value;
            return this;
        }

        public Builder stateCount(int value) {
            process.stateCount = value;
            return this;
        }

        public Builder stateTimestamp(long value) {
            process.stateTimestamp = value;
            return this;
        }

        private Builder() {
            process = new TransferProcess();
        }

    }
}
