package com.novaleap.api.module.admin.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novaleap.api.common.exception.NotFoundException;
import com.novaleap.api.entity.CustomQuestionBank;
import com.novaleap.api.entity.Question;
import com.novaleap.api.entity.UserQuestionMastery;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.CustomQuestionBankMapper;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.mapper.UserQuestionMasteryMapper;
import com.novaleap.api.mapper.UserMapper;
import com.novaleap.api.module.auth.support.AuthTokenStateSupport;
import com.novaleap.api.module.admin.user.assembler.AdminUserViewAssembler;
import com.novaleap.api.module.admin.user.dto.AdminUserSaveRequest;
import com.novaleap.api.module.admin.user.vo.AdminUserVO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AdminUserApplicationService {

    private static final Set<String> USER_ROLES = Set.of("ADMIN", "USER", "GUEST");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CustomQuestionBankMapper customQuestionBankMapper;
    private final QuestionMapper questionMapper;
    private final UserQuestionMasteryMapper userQuestionMasteryMapper;
    private final JdbcTemplate jdbcTemplate;
    private final AuthTokenStateSupport authTokenStateSupport;

    public AdminUserApplicationService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            CustomQuestionBankMapper customQuestionBankMapper,
            QuestionMapper questionMapper,
            UserQuestionMasteryMapper userQuestionMasteryMapper,
            JdbcTemplate jdbcTemplate,
            AuthTokenStateSupport authTokenStateSupport
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.customQuestionBankMapper = customQuestionBankMapper;
        this.questionMapper = questionMapper;
        this.userQuestionMasteryMapper = userQuestionMasteryMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.authTokenStateSupport = authTokenStateSupport;
    }

    public Page<AdminUserVO> getUserList(Integer page, Integer size, String keyword, String role) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (!isBlank(keyword)) {
            wrapper.like(User::getUsername, keyword.trim()).or().like(User::getNickname, keyword.trim());
        }
        if (!isBlank(role)) {
            wrapper.eq(User::getRole, normalizeRole(role));
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(pageParam, wrapper);
        return toUserPage(result);
    }

    public AdminUserVO getUserDetail(Long id) {
        return AdminUserViewAssembler.toVO(loadUser(id));
    }

    public AdminUserVO createUser(AdminUserSaveRequest request) {
        String username = trim(request.getUsername());
        String password = request.getPassword();
        String nickname = trim(request.getNickname());
        String role = normalizeRole(request.getRole());

        if (isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (isBlank(password)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (isBlank(role)) {
            role = "USER";
        }
        validateRole(role);
        ensureUniqueUsername(username);

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(isBlank(nickname) ? username : nickname);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        insertUser(user);
        return AdminUserViewAssembler.toVO(user);
    }

    public AdminUserVO updateUser(Long id, AdminUserSaveRequest request) {
        User user = loadUser(id);

        String username = trim(request.getUsername());
        String nickname = trim(request.getNickname());
        String role = normalizeRole(request.getRole());
        String password = request.getPassword();

        if (!isBlank(username) && !username.equals(user.getUsername())) {
            ensureUniqueUsername(username);
            user.setUsername(username);
        }
        if (!isBlank(nickname)) {
            user.setNickname(nickname);
        }
        if (!isBlank(role)) {
            validateRole(role);
            user.setRole(role);
        }
        if (!isBlank(password)) {
            user.setPassword(passwordEncoder.encode(password));
        }

        updateUserRecord(user);
        return AdminUserViewAssembler.toVO(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = loadUser(id);
        Long userId = user.getId();

        List<Long> customBankIds = customQuestionBankMapper.selectList(
                        new LambdaQueryWrapper<CustomQuestionBank>()
                                .select(CustomQuestionBank::getId)
                                .eq(CustomQuestionBank::getUserId, userId)
                ).stream()
                .map(CustomQuestionBank::getId)
                .filter(Objects::nonNull)
                .toList();

        // note_comments uses logical delete in code but a physical FK in DB, so cleanup must be hard delete.
        jdbcTemplate.update("DELETE FROM note_comments WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM game_score_logs WHERE user_id = ?", userId);

        userQuestionMasteryMapper.delete(new LambdaQueryWrapper<UserQuestionMastery>()
                .eq(UserQuestionMastery::getUserId, userId));

        if (!customBankIds.isEmpty()) {
            questionMapper.delete(new LambdaQueryWrapper<Question>()
                    .in(Question::getCustomBankId, customBankIds));
        }
        questionMapper.delete(new LambdaQueryWrapper<Question>()
                .eq(Question::getOwnerUserId, userId));
        customQuestionBankMapper.delete(new LambdaQueryWrapper<CustomQuestionBank>()
                .eq(CustomQuestionBank::getUserId, userId));

        try {
            userMapper.deleteById(userId);
            authTokenStateSupport.invalidateUserTokens(user.getUsername());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("该用户仍有关联业务数据，暂时无法删除，请先清理关联记录后重试");
        }
    }

    private User loadUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new NotFoundException("用户不存在");
        }
        return user;
    }

    private void validateRole(String role) {
        if (!USER_ROLES.contains(role)) {
            throw new IllegalArgumentException("角色不合法");
        }
    }

    private void ensureUniqueUsername(String username) {
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username)) != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }

    private void insertUser(User user) {
        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }

    private void updateUserRecord(User user) {
        try {
            userMapper.updateById(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("用户名已存在");
        }
    }

    private Page<AdminUserVO> toUserPage(Page<User> sourcePage) {
        Page<AdminUserVO> targetPage = new Page<>(sourcePage.getCurrent(), sourcePage.getSize(), sourcePage.getTotal());
        List<AdminUserVO> records = sourcePage.getRecords() == null
                ? Collections.emptyList()
                : sourcePage.getRecords().stream().map(AdminUserViewAssembler::toVO).toList();
        targetPage.setRecords(records);
        return targetPage;
    }

    private String normalizeRole(String role) {
        if (isBlank(role)) {
            return null;
        }
        return role.trim().toUpperCase();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
