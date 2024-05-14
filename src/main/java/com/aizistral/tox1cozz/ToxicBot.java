package com.aizistral.tox1cozz;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.aizistral.tox1cozz.config.ToxicConfig;
import com.aizistral.tox1cozz.utils.StandardLogger;
import com.aizistral.tox1cozz.config.Localization;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import lombok.var;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
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
                ).queue();

        Runtime.getRuntime().addShutdownHook(new Thread(this::trySave));

        this.jda.awaitReady();

        var channel = this.jda.getGuildChannelById(1238896261159981116L);

        //((TextChannel) channel).sendMessage("Меньше смазки потребуется").queue();
    }

    private void awake() {
        this.jda.addEventListener(this);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();

        if (command.equals("ping")) {
            long time = System.currentTimeMillis();

            event.reply(Localization.translate("cmd.ping.reply1"))
            .flatMap(v -> event.getHook().editOriginalFormat(
                    Localization.translate("cmd.ping.reply2", System.currentTimeMillis() - time)
                    )).queue();
        } else if (command.equals("version")) {
            event.reply(Localization.translate("cmd.version.reply", this.getVersion())).queue();
        } else if (command.equals("uptime")) {
            long uptime = System.currentTimeMillis() - this.startupTime;

            long hrs = TimeUnit.MILLISECONDS.toHours(uptime);
            uptime -= hrs * 60L * 60L * 1000L;
            long mins = TimeUnit.MILLISECONDS.toMinutes(uptime);
            uptime -= mins * 60L * 1000L;
            long secs = TimeUnit.MILLISECONDS.toSeconds(uptime);

            event.reply(Localization.translate("cmd.uptime.reply", hrs, mins, secs)).queue();
        } else if (command.equals("sendmsg")) {
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

            action.queue(msg -> {
                event.reply(Localization.translate("cmd.sendmsg.reply.success")).setEphemeral(true).queue();
            }, error -> {
                event.reply(Localization.translate("cmd.sendmsg.reply.fail")).setEphemeral(true).queue();
            });
        } else {
            event.reply("Бро ты галлюцинируешь, нет такой команды");
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
