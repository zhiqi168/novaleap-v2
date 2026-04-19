package com.novaleap.api.service;

import java.util.Map;
import java.util.Set;

public interface LeaderboardService {

    void recordQuestionDone(String username, Long questionId);

    void recordGameScore(String username, int score);

    void recordWishPost(String username);

    Map<String, Object> getLeaderboard();

    Map<String, Object> getUserSnapshot(String username);

    Set<Long> getDoneQuestionIds(String username);
}
