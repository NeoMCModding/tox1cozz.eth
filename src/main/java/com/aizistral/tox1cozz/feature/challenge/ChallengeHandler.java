package com.aizistral.tox1cozz.feature.challenge;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.aizistral.tox1cozz.config.Localization;
import com.aizistral.tox1cozz.utils.SimpleDuration;
import com.aizistral.tox1cozz.utils.StandardLogger;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChallengeHandler extends ListenerAdapter {
    public static final ChallengeHandler INSTANCE = new ChallengeHandler();
    protected static final StandardLogger LOGGER = new StandardLogger("ChallengeHandler");
    public static final SimpleDuration MAX_CHALLENGE_TIMEOUT = new SimpleDuration(14, TimeUnit.DAYS);
    private final ChallengeList challenges = ChallengeList.INSTANCE;

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Guild guild = event.getGuild();
        Message msg = event.getMessage();
        String buttonID = event.getButton().getId();
        InteractionHook hook = event.getHook();

        ButtonType type;
        UUID challengeID;

        try {
            type = ButtonType.fromString(buttonID.split(":")[0]);
            challengeID = UUID.fromString(buttonID.split(":")[1]);
        } catch (Exception ex) {
            LOGGER.error("Failed to parse challenge button ID: " + buttonID);
            return;
        }

        this.challenges.findChallenge(challengeID).ifPresentOrElse(challenge -> {
            if (challenge.getChallengedID() != event.getUser().getIdLong()) {
                event.reply(Localization.translate("cmd.challenge.interaction.badUser"))
                .setEphemeral(true).queue();
                return;
            }

            guild.retrieveMembersByIds(challenge.getChallengerID(), challenge.getChallengedID())
            .onSuccess(members -> {
                if (members.size() != 2) {
                    event.reply(Localization.translate("cmd.challenge.interaction.invalidMemberCount"))
                    .queue();
                    return;
                }

                Member challenger = members.stream()
                        .filter(m -> m.getIdLong() == challenge.getChallengerID())
                        .findAny().get();
                Member challenged = members.stream()
                        .filter(m -> m.getIdLong() == challenge.getChallengedID())
                        .findAny().get();

                this.challenges.removeChallenge(challengeID);

                if (type == ButtonType.ACCEPT) {
                    ChallengeOutcome outcome = challenge.rollOutcome();
                    SimpleDuration punishment = challenge.getPunishment();

                    Member winner = outcome == ChallengeOutcome.CHALLENGER_WINS ? challenger : challenged;
                    Member loser = outcome == ChallengeOutcome.CHALLENGER_WINS ? challenged : challenger;

                    try {
                        guild.timeoutFor(loser, punishment.getDuration(), punishment.getTimeUnit())
                        .queue(success -> {
                            hook.sendMessage(this.localizeOutcome(challenger, challenged, winner, loser,
                                    punishment, true))
                            .setFiles(FileUpload.fromData(Path.of("../assets/terpila.png")))
                            .queue();
                        }, failure -> {
                            hook.sendMessage(this.localizeOutcome(challenger, challenged, winner, loser,
                                    punishment, false)).queue();
                        });

                    } catch (Exception ex) {
                        LOGGER.error("Muting " + loser.getUser().getName() + " failed.");
                        hook.sendMessage(this.localizeOutcome(challenger, challenged, winner, loser,
                                punishment, false)).queue();
                    }
                } else {
                    hook.sendMessage(Localization.translate("cmd.challenge.interaction.denied",
                            challenged.getId())).queue();
                }
            }).onError(error -> {
                LOGGER.error("Failed to retrieve challenge members:", error);
                event.reply(Localization.translate("cmd.challenge.interaction.memberError")).queue();
            });
        }, () -> {
            hook.sendMessage(Localization.translate("cmd.challenge.interaction.challengeNotFound"))
            .setEphemeral(true).queue();
        });


        if (!event.isAcknowledged()) {
            event.editComponents(msg.getComponents().stream().map(LayoutComponent::asDisabled).toList())
            .queue();
        }
    }

    private String localizeOutcome(Member challenger, Member challenged, Member winner,
            Member loser, SimpleDuration punishment, boolean success) {
        String postfix = success ? "acceptedSuccess" : "acceptedFail";
        return Localization.translate(
                "cmd.challenge.interaction." + postfix,
                challenged.getId(),
                punishment.getLocalized(),
                challenger.getId(),
                challenged.getId(),
                winner.getId(),
                loser.getId()
                );
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String command = event.getName();


        if (!command.equals("challenge"))
            return;

        if (event.getGuild() == null) {
            event.reply(Localization.translate("cmd.challenge.reply.nodm")).queue();
            return;
        }

        Guild guild = event.getGuild();
        User challenger = event.getUser();
        User challenged = event.getOption("user").getAsUser();
        String duration = event.getOption("duration", OptionMapping::getAsString);

        if (duration == null) {
            duration = "1h";
        }

        if (challenged.isBot()) {
            event.reply(Localization.translate("cmd.challenge.reply.nobot")).queue();
            return;
        }

        SimpleDuration punishment;

        try {
            punishment = SimpleDuration.fromString(duration);
        } catch (IllegalArgumentException ex) {
            event.reply(Localization.translate("cmd.challenge.reply.badDuration", duration)).queue();
            return;
        }

        if (punishment.greaterThan(MAX_CHALLENGE_TIMEOUT)) {
            event.reply(Localization.translate("cmd.challenge.reply.muteTooLong")).queue();
            return;
        }

        guild.retrieveMembersByIds(challenger.getIdLong(), challenged.getIdLong()).onSuccess(members -> {
            if (members.size() != 2) {
                if (members.size() == 1) {
                    event.reply(Localization.translate("cmd.challenge.reply.noself")).queue();
                } else {
                    event.reply(Localization.translate("cmd.challenge.reply.invalidMemberCount")).queue();
                }

                return;
            }

            Member challengerMember = members.stream()
                    .filter(m -> m.getIdLong() == challenger.getIdLong()).findAny().get();
            Member challengedMember = members.stream()
                    .filter(m -> m.getIdLong() == challenged.getIdLong()).findAny().get();

            Challenge challenge = this.challenges.createChallenge(challenger, challenged, punishment);
            UUID challengeID = challenge.getId();

            String localizedReply = Localization.translate("cmd.challenge.reply.success",
                    challengedMember.getEffectiveName(), challengedMember.getId(),
                    challengerMember.getEffectiveName(), challengerMember.getId(),
                    punishment.getLocalized());

            event.reply(localizedReply).addActionRow(
                    Button.primary("challenge.accept:" + challengeID,
                            Localization.translate("cmd.challenge.button.accept"))
                    .withEmoji(Emoji.fromUnicode("💪")),
                    Button.secondary("challenge.deny:" + challengeID,
                            Localization.translate("cmd.challenge.button.deny"))
                    .withEmoji(Emoji.fromCustom("peepo_clown", 1238880262729895996L, false))
                    )
            .mentionUsers(challenged.getIdLong()).queue();
        }).onError(error -> {
            LOGGER.error("Failed to retrieve challenge members:", error);
            event.reply(Localization.translate("cmd.challenge.reply.memberError")).queue();
        });
    }

}
