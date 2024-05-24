package com.aizistral.tox1cozz;

import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.aizistral.tox1cozz.config.ToxicConfig;
import com.aizistral.tox1cozz.feature.challenge.ButtonType;
import com.aizistral.tox1cozz.feature.challenge.Challenge;
import com.aizistral.tox1cozz.feature.challenge.ChallengeHandler;
import com.aizistral.tox1cozz.feature.challenge.ChallengeList;
import com.aizistral.tox1cozz.feature.challenge.ChallengeOutcome;
import com.aizistral.tox1cozz.utils.SimpleDuration;
import com.aizistral.tox1cozz.utils.StandardLogger;
import com.aizistral.tox1cozz.config.Localization;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.messages.MessagePollData;

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

        this.jda.updateCommands().addCommands(
                Commands.slash("ping", Localization.translate("cmd.ping.desc")),
                Commands.slash("version", Localization.translate("cmd.version.desc")),
                Commands.slash("uptime", Localization.translate("cmd.uptime.desc")),
                Commands.slash("sendmsg", Localization.translate("cmd.sendmsg.desc"))
                .addOption(
                        OptionType.STRING, "message",
                        Localization.translate("cmd.sendmsg.option.message"),
                        true
                        )
                .addOption(
                        OptionType.CHANNEL, "channel",
                        Localization.translate("cmd.sendmsg.option.channel"),
                        false
                        )
                .addOption(
                        OptionType.STRING, "reference",
                        Localization.translate("cmd.sendmsg.option.reference"),
                        false
                        )
                //                Commands.slash("challenge", Localization.translate("cmd.challenge.desc"))
                //                .addOption(
                //                        OptionType.USER, "user",
                //                        Localization.translate("cmd.challenge.option.user"),
                //                        true
                //                        )
                //                .addOption(OptionType.STRING, "duration",
                //                        Localization.translate("cmd.challenge.option.duration"),
                //                        false
                //                        )
                ).queue();

        Runtime.getRuntime().addShutdownHook(new Thread(this::trySave));

        this.jda.awaitReady();

        var channel = this.jda.getGuildChannelById(1238896261159981116L);

        //((TextChannel) channel).sendMessage("Меньше смазки потребуется").queue();
    }

    private void awake() {
        this.jda.addEventListener(this);
        //        this.jda.addEventListener(ChallengeHandler.INSTANCE);
    }



    private void handlePing(SlashCommandInteractionEvent event) {
        long time = System.currentTimeMillis();

        event.reply(Localization.translate("cmd.ping.reply1"))
        .flatMap(v -> event.getHook().editOriginalFormat(
                Localization.translate("cmd.ping.reply2", System.currentTimeMillis() - time)
                )).queue();
    }

    private void handleVersion(SlashCommandInteraction event) {
        event.reply(Localization.translate("cmd.version.reply", this.getVersion())).queue();
    }

    private void handleUptime(SlashCommandInteraction event) {
        long uptime = System.currentTimeMillis() - this.startupTime;

        long hrs = TimeUnit.MILLISECONDS.toHours(uptime);
        uptime -= hrs * 60L * 60L * 1000L;
        long mins = TimeUnit.MILLISECONDS.toMinutes(uptime);
        uptime -= mins * 60L * 1000L;
        long secs = TimeUnit.MILLISECONDS.toSeconds(uptime);

        event.reply(Localization.translate("cmd.uptime.reply", hrs, mins, secs)).queue();
    }

    private void handleSendMsg(SlashCommandInteraction event) {
        var argChannel = event.getOption("channel", OptionMapping::getAsChannel);
        val reference = event.getOption("reference", OptionMapping::getAsString);
        val message = event.getOption("message", OptionMapping::getAsString);

        MessageChannel channel;

        if (argChannel != null) {
            if (argChannel instanceof MessageChannel) {
                channel = (MessageChannel) argChannel;
            } else {
                event.reply(Localization.translate("cmd.sendmsg.reply.badChannel")).setEphemeral(true)
                .queue();
                return;
            }
        } else {
            channel = event.getChannel();
        }

        var action = channel.sendMessage(message);

        if (reference != null) {
            action.setMessageReference(reference);
        }

        LOGGER.log("User %s used /sendmsg with message: %s", event.getUser().getEffectiveName(), message);

        EmbedBuilder builder = new EmbedBuilder();

        builder
        .setTitle(Localization.translate("cmd.sendmsg.log.title"))
        .appendDescription(
                Localization.translate(
                        "cmd.sendmsg.log.content",
                        event.getUser().getId(),
                        event.getUser().getEffectiveName(),
                        message
                        )
                )
        .setColor(new Color(0, 100, 85));

        event.getGuild().getTextChannelById(ToxicConfig.INSTANCE.getLogsChannelID())
        .sendMessageEmbeds(builder.build()).queue();

        action.queue(msg -> {
            event.reply(Localization.translate("cmd.sendmsg.reply.success")).setEphemeral(true).queue();
        }, error -> {
            event.reply(Localization.translate("cmd.sendmsg.reply.fail")).setEphemeral(true).queue();
        });
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();

        switch (command) {
        case "ping":
            this.handlePing(event);
            break;
        case "version":
            this.handleVersion(event);
            break;
        case "uptime":
            this.handleUptime(event);
            break;
        case "sendmsg":
            this.handleSendMsg(event);
            break;
        }
    }


    public JDA getJDA() {
        return this.jda;
    }

    public String getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        return version != null ? version : "UNKNOWN";
    }

    public void terminate(Throwable reason) {
        LOGGER.error("Toxic Bot has encountered a fatal error:", reason);
        LOGGER.error("Initiating termination sequence...");
        this.trySave();

        LOGGER.log("Database saved, calling system exit.");
        System.exit(1);
    }

    public void shutdown() {
        LOGGER.log("Toxic Bot is shutting down...");
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
