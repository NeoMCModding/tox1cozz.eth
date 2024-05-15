package com.aizistral.tox1cozz.feature.challenge;

import java.util.UUID;

import com.aizistral.tox1cozz.utils.SimpleDuration;

import lombok.Value;

@Value
public class Challenge {
    private final UUID id;
    private final long challengerID, challengedID, createdAt;
    private final SimpleDuration punishment;

    public ChallengeOutcome rollOutcome() {
        return ChallengeList.INSTANCE.rollOutcome(this);
    }

}
