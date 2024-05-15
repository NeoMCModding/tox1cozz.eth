package com.aizistral.tox1cozz.utils;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DurationParser {
    private static final Pattern DUR_PAT = Pattern.compile("(\\d++)\\s*+(w|d|h|ms?|s)");

    public static Duration parse(String durationString) {
        Duration dur = Duration.ZERO;
        Matcher match = DUR_PAT.matcher(durationString);

        while(match.find()) {
            long amount = Long.parseLong(match.group(1));
            switch(match.group(2)) {
            case "w":
                dur = dur.plusDays(7*amount);
                break;
            case "d":
                dur = dur.plusDays(amount);
                break;
            case "h":
                dur = dur.plusHours(amount);
                break;
            case "m":
                dur = dur.plusMinutes(amount);
                break;
            case "s":
                dur = dur.plusSeconds(amount);
                break;
            case "ms":
                dur = dur.plusNanos(amount*1000000L);
                break;
            }
        }

        return dur;
    }

    public static long toDurationMs(String durationString) {
        return toDurationMs(parse(durationString));
    }

    public static long toDurationMs(Duration duration) {
        long out = duration.getSeconds()*1000;
        out += duration.getNano() / 1000000L;
        return out;
    }

    public static Duration toDuration(long timeInMillis) {
        return Duration.ofMillis(timeInMillis);
    }

    public static String formatDuration(long timeInMillis) {
        return formatDuration(toDuration(timeInMillis));
    }

    public static String formatDuration(long timeInMillis, boolean includeIntermed) {
        return formatDuration(toDuration(timeInMillis), includeIntermed);
    }

    public static String formatDuration(Duration dur) {
        return formatDuration(dur, false);
    }

    public static String formatDuration(Duration dur, boolean includeIntermed) {
        StringBuilder sb = new StringBuilder();
        boolean firstFound = false;
        long secs = dur.getSeconds();

        //weeks
        long tmp = secs / 604800L;
        if(tmp > 0) {
            sb.append(tmp).append("w ");
            secs %= 604800L;
            firstFound = true;
        }
        //days
        tmp = secs / 86400L;
        if ((includeIntermed && firstFound) || tmp > 0) {
            sb.append(tmp).append("d ");
            secs %= 86400L;
            firstFound = true;
        }
        //hours
        tmp = secs / 3600L;
        if ((includeIntermed && firstFound) || tmp > 0) {
            sb.append(tmp).append("h ");
            if (tmp > 0) {
                secs %= 3600L;
                firstFound = true;
            }
        }
        //minutes
        tmp = secs / 60L;
        if ((includeIntermed && firstFound) || tmp > 0) {
            sb.append(tmp).append("m ");
            if (tmp > 0) {
                secs %= 60L;
                firstFound = true;
            }
        }
        //seconds
        if ((includeIntermed && firstFound) || secs > 0) {
            sb.append(secs).append("s ");
        }
        //ms (only include if > 0, don't show when intermed is enabled but ms# is 0)
        tmp = dur.getNano() / 1000000L;
        if (tmp > 0) {
            sb.append(tmp).append("ms ");
        }
        //empty -> 0ms
        if (sb.length() == 0)
            return "0ms";
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

}
