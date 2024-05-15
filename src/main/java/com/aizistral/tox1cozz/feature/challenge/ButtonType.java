package com.aizistral.tox1cozz.feature.challenge;

public enum ButtonType {
    ACCEPT, DENY;

    public static ButtonType fromString(String str) throws IllegalArgumentException {
        switch (str) {
        case "challenge.accept":
            return ACCEPT;
        case "challenge.deny":
            return DENY;
        default:
            throw new IllegalArgumentException("No such type: " + str);
        }
    }
}
