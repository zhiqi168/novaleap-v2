package com.novaleap.api.controller;

import com.novaleap.api.common.Result;
import com.novaleap.api.dto.LeaderboardGameScoreRequest;
import com.novaleap.api.dto.LeaderboardQuestionDoneRequest;
import com.novaleap.api.entity.User;
import com.novaleap.api.module.system.security.CurrentUserService;
import com.novaleap.api.service.LeaderboardService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final CurrentUserService currentUserService;

    public LeaderboardController(LeaderboardService leaderboardService, CurrentUserService currentUserService) {
        this.leaderboardService = leaderboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Result<Map<String, Object>> getLeaderboard(Authentication authentication) {
        Map<String, Object> data = leaderboardService.getLeaderboard();
        if (!currentUserService.isAnonymous(authentication) && !currentUserService.isGuest(authentication)) {
            data.put("currentUser", leaderboardService.getUserSnapshot(currentUserService.current(authentication).safeUsername()));
        }
        return Result.success(data);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> getCurrentUserSnapshot(Authentication authentication) {
        if (currentUserService.isAnonymous(authentication) || currentUserService.isGuest(authentication)) {
            return Result.success(Collections.emptyMap());
        }
        return Result.success(
                leaderboardService.getUserSnapshot(currentUserService.current(authentication).safeUsername())
        );
    }

    @GetMapping("/question-done")
    public Result<List<Long>> getDoneQuestions(Authentication authentication) {
        if (currentUserService.isAnonymous(authentication) || currentUserService.isGuest(authentication)) {
            return Result.success(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>(leaderboardService.getDoneQuestionIds(currentUserService.current(authentication).safeUsername()));
        ids.sort(Long::compareTo);
        return Result.success(ids);
    }

    @PostMapping("/question-done")
    public Result<Void> markQuestionDone(
            Authentication authentication,
            @RequestBody @Valid LeaderboardQuestionDoneRequest request
    ) {
        User currentUser = currentUserService.requireDatabaseUser(authentication, "游客账号不能提交排行榜记录");
        leaderboardService.recordQuestionDone(currentUser.getUsername(), request.getQuestionId());
        return Result.success();
    }

    @PostMapping("/game-score")
    public Result<Void> reportGameScore(
            Authentication authentication,
            @RequestBody @Valid LeaderboardGameScoreRequest request
    ) {
        User currentUser = currentUserService.requireDatabaseUser(authentication, "游客账号不能提交排行榜记录");
        leaderboardService.recordGameScore(currentUser.getUsername(), request.getScore());
        return Result.success();
    }
}
