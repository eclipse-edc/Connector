package com.microsoft.dagx.spi.types.domain.transfer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 */
class TransferProcessTest {

    @Test
    void verifyDeserialization() throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        TransferProcess process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, process);

        TransferProcess deserialized = mapper.readValue(writer.toString(), TransferProcess.class);

        assertEquals(process, deserialized);
    }

    @Test
    void verifyCopy() {
        TransferProcess process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).state(TransferProcessStates.COMPLETED.code()).stateCount(1).stateTimestamp(1).build();
        TransferProcess copy = process.copy();

        assertEquals(process.getState(), copy.getState());
        assertEquals(process.getStateCount(), copy.getStateCount());
        assertEquals(process.getStateTimestamp(), copy.getStateTimestamp());

        assertEquals(process, copy);
    }

    @Test
    void verifyTransitions() {
        TransferProcess process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).build();

        assertThrows(IllegalStateException.class, process::transitionProvisioning);
        process.transitionInitial();

        assertThrows(IllegalStateException.class, process::transitionProvisioned);

        process.transitionProvisioning();
        process.transitionProvisioned();

        process.transitionRequested();

        assertThrows(IllegalStateException.class, process::transitionEnded);

        process.transitionDeprovisioning();
        process.transitionDeprovisioned();
        process.transitionEnded();
    }

    @Test
    void verifyTransitionRollback() {
        TransferProcess process = TransferProcess.Builder.newInstance().id(UUID.randomUUID().toString()).build();
        process.transitionInitial();
        process.transitionProvisioning();

        process.rollbackState(TransferProcessStates.INITIAL);

        assertEquals(TransferProcessStates.INITIAL.code(), process.getState());
        assertEquals(1, process.getStateCount());
    }
}
