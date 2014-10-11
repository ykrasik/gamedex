package com.github.ykrasik.indexter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Lifecycle;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @author Yevgeny Krasik
 */
public abstract class AbstractService implements Lifecycle {
    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    private volatile boolean started;

    @Override
    @PostConstruct
    public final void start() {
        if (started) {
            return;
        }

        try {
            LOG.info("Starting...");
            started = true;
            doStart();
            LOG.info("Started.");
        } catch (Exception e) {
            LOG.warn("Failed to start!", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    @PreDestroy
    public final void stop() {
        if (!started) {
            return;
        }

        try {
            LOG.info("Stopping...");
            doStop();
            started = false;
            LOG.info("Stopped.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isRunning() {
        return started;
    }

    protected abstract void doStart() throws Exception;
    protected abstract void doStop() throws Exception;
}
