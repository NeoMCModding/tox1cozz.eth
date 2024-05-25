package com.aizistral.tox1cozz.feature.typing;

import java.util.Random;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

@RequiredArgsConstructor
public class TypingHandler extends Thread {
    private final Random random = new Random();
    private final TextChannel channel;
    private int typingCount = 3;

    @Override
    public void run() {
        while (true) {
            if (this.random.nextDouble() < 0.0055) {
                this.typingCount = 1 + this.random.nextInt(6);
            }

            if (this.typingCount-- > 0) {
                this.channel.sendTyping().queue();
            }

            try {
                Thread.sleep(5_000L);
            } catch (InterruptedException ex) {
                // NO-OP
            }
        }
    }

}
