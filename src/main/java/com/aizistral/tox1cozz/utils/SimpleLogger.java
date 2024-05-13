package com.aizistral.tox1cozz.utils;

import lombok.Getter;

@Getter
public class SimpleLogger {
    protected final String name;

    public SimpleLogger(String name) {
        this.name = name;
    }

    public void error(String error) {
        System.err.println("[" + this.name + "]: " + error);
    }

    public void log(String log) {
        System.out.println("[" + this.name + "]: " + log);
    }

    public void error(String error, Object... args) {
        System.err.println("[" + this.name + "]: " + String.format(error, args));
    }

    public void log(String log, Object... args) {
        System.out.println("[" + this.name + "]: " + String.format(log, args));
    }

}
