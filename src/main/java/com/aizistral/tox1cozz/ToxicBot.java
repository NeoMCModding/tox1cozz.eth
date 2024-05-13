package com.aizistral.tox1cozz;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.aizistral.tox1cozz.config.ToxicConfig;
import com.aizistral.tox1cozz.utils.StandardLogger;
import com.aizistral.tox1cozz.config.Localization;
import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

@Getter
public class ToxicBot extends ListenerAdapter {
    protected static final StandardLogger LOGGER = new StandardLogger("ToxicBot");
    public static final ToxicBot INSTANCE = new ToxicBot(Main.JDA);

    static {
        INSTANCE.awake();
    }

    private final JDA jda;
    private final ToxicConfig config;
    private final long startupTime;


    @SneakyThrows
    private ToxicBot(JDA jda) {
        if (jda == null)
            throw new IllegalStateException("JDA cannot be null!");

        this.jda = jda;
        this.config = ToxicConfig.INSTANCE;
        this.startupTime = System.currentTimeMillis();

        Runtime.getRuntime().addShutdownHook(new Thread(this::trySave));

        this.jda.awaitReady();
    }

    private void awake() {
        this.jda.addEventListener(this);
    }


    public JDA getJDA() {
        return this.jda;
    }

    public String getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        return version != null ? version : "UNKNOWN";
    }

    public void terminate(Throwable reason) {
        LOGGER.error("Infinite Machine has encountered a fatal error:", reason);
        LOGGER.error("Initiating termination sequence...");
        this.trySave();

        LOGGER.log("Database saved, calling system exit.");
        System.exit(1);
    }

    public void shutdown() {
        LOGGER.log("Infinite Machine is shutting down...");
        this.trySave();

        LOGGER.log("Database saved, calling system exit.");
        System.exit(0);
    }

    private void trySave() {
        LOGGER.log("Trying to save the database...");

        try {
            //this.database.forceSave();
        } catch (Throwable ex) {
            LOGGER.error("Failed to save database! Stacktrace:", ex);
        }
    }

}
