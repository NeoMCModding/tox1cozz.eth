package com.aizistral.tox1cozz.utils;

import java.util.concurrent.TimeUnit;

import com.aizistral.tox1cozz.config.Localization;

import lombok.Value;

@Value
public class SimpleDuration {
    private final long duration;
    private final TimeUnit timeUnit;

    public boolean greaterThan(SimpleDuration other) {
        long thisMillis = TimeUnit.MILLISECONDS.convert(this.duration, this.timeUnit);
        long otherMillis = TimeUnit.MILLISECONDS.convert(other.duration, other.timeUnit);
        return thisMillis > otherMillis;
    }

    public String getLocalized() {
        return Localization.translate("time." + this.timeUnit.name().toLowerCase(), this.duration);
    }

    public static SimpleDuration fromString(String str) throws IllegalArgumentException {
        try {
            long time = Long.parseLong(str.substring(0, str.length() - 1));
            String unit = str.substring(str.length() - 1, str.length()).toLowerCase();

            System.out.println("time: " + time + ", unit: " + unit);
            TimeUnit timeUnit;

            switch (unit) {
            case "s":
                timeUnit = TimeUnit.SECONDS;
                break;
            case "m":
                timeUnit = TimeUnit.MINUTES;
                break;
            case "h":
                timeUnit = TimeUnit.HOURS;
                break;
            case "d":
                timeUnit = TimeUnit.DAYS;
                break;
            default:
                throw new IllegalArgumentException("No such unit: " + unit);
            }

            return new SimpleDuration(time, timeUnit);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not parse duration string: " + str, ex);
        }
    }
}
