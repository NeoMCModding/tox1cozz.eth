package com.aizistral.tox1cozz.feature.challenge;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.aizistral.tox1cozz.utils.SimpleDuration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChallengeList {
    public final static ChallengeList INSTANCE = new ChallengeList();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReadLock readLock = this.lock.readLock();
    private final WriteLock writeLock = this.lock.writeLock();
    private final Map<UUID, Challenge> msgToChallenge = new HashMap<>();
    private final Random random = new Random();

    public Challenge createChallenge(User challenger, User challenged, SimpleDuration punishment) {
        try {
            this.writeLock.lock();
            UUID id = UUID.randomUUID();

            Challenge challenge = new Challenge(id, challenger.getIdLong(), challenged.getIdLong(),
                    System.currentTimeMillis(), punishment);

            this.msgToChallenge.put(id, challenge);
            return challenge;
        } finally {
            this.writeLock.unlock();
        }
    }

    public Optional<Challenge> findChallenge(UUID id) {
        try {
            this.readLock.lock();
            return Optional.ofNullable(this.msgToChallenge.get(id));
        } finally {
            this.readLock.unlock();
        }
    }

    public void removeChallenge(UUID id) {
        this.msgToChallenge.remove(id);
    }

    public ChallengeOutcome rollOutcome(Challenge challenge) {
        return this.random.nextDouble() > 0.5 ?
                ChallengeOutcome.CHALLENGER_WINS : ChallengeOutcome.CHALLENGED_WINS;
    }

}
