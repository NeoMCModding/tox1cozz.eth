package com.aizistral.tox1cozz.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.jetbrains.annotations.NotNull;

import com.aizistral.tox1cozz.ToxicBot;
import com.aizistral.tox1cozz.utils.StandardLogger;
import com.google.common.base.Supplier;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.SneakyThrows;

public abstract class AsyncJSONConfig<T> {
    private static final StandardLogger LOGGER = new StandardLogger("JsonHandler");
    protected static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    protected final Path file;
    protected final Class<T> dataClass;
    protected final Supplier<T> dataFactory;
    protected final AtomicBoolean needsSaving;
    protected final ReentrantReadWriteLock lock;
    protected final ReadLock readLock;
    protected final WriteLock writeLock;
    protected final long saveDelay;

    private Thread saveChecker = null;
    private boolean init = false;
    private T data = null;

    @SneakyThrows
    protected AsyncJSONConfig(Path filePath, long saveDelay, Class<T> dataClass, Supplier<T> dataFactory) {
        this.file = filePath.toFile().getCanonicalFile().toPath();
        this.dataFactory = dataFactory;
        this.saveDelay = saveDelay;
        this.dataClass = dataClass;
        this.lock = new ReentrantReadWriteLock();
        this.readLock = this.lock.readLock();
        this.writeLock = this.lock.writeLock();
        this.needsSaving = new AtomicBoolean(false);
    }

    private void saveCheck() {
        while (true) {
            if (this.needsSaving.getAndSet(false)) {
                try {
                    this.saveFile();
                } catch (IOException ex) {
                    ToxicBot.INSTANCE.terminate(ex);
                }
            }

            try {
                Thread.sleep(this.saveDelay);
            } catch (InterruptedException ex) {
                // NO-OP
            }
        }
    }

    public void init() throws IOException {
        try {
            this.writeLock.lock();

            if (this.init)
                throw new IllegalStateException("Init was already called");

            LOGGER.log("Reading %s...", this.file.getFileName().toString());

            this.init = true;
            Files.createDirectories(this.file.getParent());

            this.data = readFile(this.file, this.dataClass).orElseGet(() -> {
                this.needsSaving.set(true);
                return this.dataFactory.get();
            });

            this.saveChecker = new Thread(this::saveCheck);
            this.saveChecker.start();
        } finally {
            this.writeLock.unlock();
        }
    }

    public void scheduleSave() {
        this.needsSaving.set(true);
    }

    public void forceSave() {
        try {
            this.needsSaving.set(false);
            this.saveFile();
        } catch (IOException ex) {
            ToxicBot.INSTANCE.terminate(ex);
        }
    }

    @NotNull
    protected T getData() {
        if (this.data == null)
            throw new RuntimeException("Fatal JSON error, tried to get data with no data loaded");

        return this.data;
    }

    private void saveFile() throws IOException {
        try {
            this.writeLock.lock();
            LOGGER.log("Saving %s...", this.file.getFileName().toString());
            writeFile(this.file, this.getData());
        } finally {
            this.writeLock.unlock();
        }
    }

    private static <T> Optional<T> readFile(Path file, Class<T> dataClass) {
        if (!Files.isRegularFile(file))
            return Optional.empty();

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return Optional.of(GSON.fromJson(reader, dataClass));
        } catch (Exception ex) {
            LOGGER.error("Could not read file: %s", file);
            LOGGER.error("This likely indicates the file is corrupted. Full stacktrace:", ex);

            ToxicBot.INSTANCE.terminate(new RuntimeException(ex));
            return Optional.empty();
        }
    }

    private static <T> void writeFile(Path file, T config) {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception ex) {
            LOGGER.log("Could not write config file: %s", file);
            ToxicBot.INSTANCE.terminate(new RuntimeException(ex));
        }
    }

}
