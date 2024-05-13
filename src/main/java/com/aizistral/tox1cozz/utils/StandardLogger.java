package com.aizistral.tox1cozz.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardLogger extends SimpleLogger {
    private final Logger logger;

    public StandardLogger(String name) {
        super(name);
        this.logger = LoggerFactory.getLogger(name);
    }

    @Override
    public void error(String error) {
        this.logger.error(error);
    }

    @Override
    public void error(String error, Object... args) {
        this.logger.error(String.format(error, args));
    }

    @Override
    public void log(String log) {
        this.logger.info(log);
    }

    @Override
    public void log(String log, Object... args) {
        this.logger.info(String.format(log, args));
    }

    public void debug(String log) {
        this.logger.debug(log);
    }

    public void debug(String log, Object... args) {
        this.logger.debug(String.format(log, args));
    }

    public void error(String error, Throwable ex) {
        this.logger.error(error, ex);
    }

    public void error(String error, Throwable ex, Object... args) {
        this.logger.error(error, ex);
    }

}
