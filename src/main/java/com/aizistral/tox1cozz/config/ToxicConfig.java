package com.aizistral.tox1cozz.config;

import java.nio.file.Paths;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

public class ToxicConfig extends AsyncJSONConfig<ToxicConfig.Data> {
    public static final ToxicConfig INSTANCE = new ToxicConfig();

    @SneakyThrows
    private ToxicConfig() {
        super(Paths.get("./config/config.json"), 600_000L, Data.class, Data::new);
    }

    @NonNull
    public String getAccessToken() {
        try {
            this.readLock.lock();
            return this.getData().accessToken != null ? this.getData().accessToken : "";
        } finally {
            this.readLock.unlock();
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    static final class Data {
        private String accessToken = "";
    }

}
