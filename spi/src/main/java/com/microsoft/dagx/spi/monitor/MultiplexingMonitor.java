package com.microsoft.dagx.spi.monitor;

import com.microsoft.dagx.spi.monitor.Monitor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class MultiplexingMonitor implements Monitor {

    private Collection<Monitor> internalMonitors;

    public MultiplexingMonitor() {
    }

    public MultiplexingMonitor(List<Monitor> monitors){
        internalMonitors= monitors;
    }

    @Override
    public void severe(Supplier<String> supplier, Throwable... errors) {
        internalMonitors.forEach(m -> m.severe(supplier, errors));
    }

    @Override
    public void severe(String message, Throwable... errors) {
        internalMonitors.forEach(m -> m.severe(message, errors));
    }

    @Override
    public void severe(Map<String, Object> data) {
        internalMonitors.forEach(m -> m.severe(data));
    }

    @Override
    public void info(Supplier<String> supplier, Throwable... errors) {
        internalMonitors.forEach(m -> m.info(supplier, errors));
    }

    @Override
    public void info(String message, Throwable... errors) {
        internalMonitors.forEach(m -> m.info(message, errors));
    }

    @Override
    public void debug(Supplier<String> supplier, Throwable... errors) {
        internalMonitors.forEach(m -> m.debug(supplier, errors));
    }

    @Override
    public void debug(String message, Throwable... errors) {
        internalMonitors.forEach(m -> m.debug(message, errors));
    }
}
