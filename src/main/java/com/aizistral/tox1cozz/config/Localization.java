package com.aizistral.tox1cozz.config;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.aizistral.tox1cozz.utils.StandardLogger;

public class Localization {
    private static final StandardLogger LOGGER = new StandardLogger("Localization");
    private static final Map<String, String> LOCALIZATION = new HashMap<>();

    public static void load() throws IOException {
        LOGGER.log("Loading localization...");

        InputStream stream = Localization.class.getResourceAsStream("/lang/local.lang");
        List<String> lines = new ArrayList<>();

        if (stream == null)
            throw new IOException("Localization file not found");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line = null;

            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }
        }

        LOCALIZATION.clear();

        for (String line : lines) {
            if (line.startsWith("#") || line.startsWith("//") || line.indexOf('=') <= 0) {
                continue;
            }

            String[] splat = line.split(Pattern.quote("="), 2);
            LOCALIZATION.put(splat[0], splat[1].replace("\\n", "\n"));
        }

        LOGGER.log("Successfully loaded %s localization entries.", LOCALIZATION.size());
    }

    public static String translate(String key) {
        return LOCALIZATION.getOrDefault(key, key);
    }

    public static String translate(String key, Object... args) {
        return String.format(translate(key), args);
    }

    public static int getVar(int number) {
        int var = 3;

        if (number >= 10 && number <= 20)
            return var;

        int lastDigit = number % 10;

        if (lastDigit == 1) {
            var = 1;
        } else if (lastDigit > 0 && lastDigit < 5) {
            var = 2;
        }

        return var;
    }

}
