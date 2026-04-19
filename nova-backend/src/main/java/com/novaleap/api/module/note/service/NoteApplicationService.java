package com.novaleap.api.module.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novaleap.api.common.exception.NotFoundException;
import com.novaleap.api.common.exception.UnauthorizedException;
import com.novaleap.api.dto.NoteCommentRequest;
import com.novaleap.api.dto.NoteCommentVO;
import com.novaleap.api.dto.NoteLikeVO;
import com.novaleap.api.entity.Note;
import com.novaleap.api.entity.NoteComment;
import com.novaleap.api.entity.NoteLike;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.NoteCommentMapper;
import com.novaleap.api.mapper.NoteLikeMapper;
import com.novaleap.api.mapper.NoteMapper;
import com.novaleap.api.module.note.assembler.NoteViewAssembler;
import com.novaleap.api.module.note.dto.NoteCreateRequest;
import com.novaleap.api.module.note.dto.NoteUpdateRequest;
import com.novaleap.api.module.note.vo.NoteDetailVO;
import com.novaleap.api.module.note.vo.NoteListItemVO;
import com.novaleap.api.module.system.security.ActorIdentity;
import com.novaleap.api.module.system.security.CurrentUserService;
import com.novaleap.api.service.AiService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoteApplicationService {

    private static final int NOTE_STATUS_PENDING = 0;
    private static final int NOTE_STATUS_APPROVED = 1;
    private static final Set<String> HARD_BANNED_WORDS = Set.of(
            "吸毒", "毒品", "贩毒", "涉毒",
            "赌博", "博彩", "赌钱", "网赌",
            "诈骗", "洗钱", "刷单", "传销",
            "暴恐", "恐怖袭击", "爆炸物", "枪支", "制枪",
            "仇恨言论", "极端主义",
            "色情", "约炮", "成人视频", "卖淫",
            "草泥马", "你妈", "傻逼", "操你妈"
    );

    private final NoteMapper noteMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteCommentMapper noteCommentMapper;
    private final CurrentUserService currentUserService;
    private final AiService aiService;

    public NoteApplicationService(
            NoteMapper noteMapper,
            NoteLikeMapper noteLikeMapper,
            NoteCommentMapper noteCommentMapper,
            CurrentUserService currentUserService,
            AiService aiService
    ) {
        this.noteMapper = noteMapper;
        this.noteLikeMapper = noteLikeMapper;
        this.noteCommentMapper = noteCommentMapper;
        this.currentUserService = currentUserService;
        this.aiService = aiService;
    }

    public Page<NoteListItemVO> getNoteList(
            Integer page,
            Integer size,
            String keyword,
            String category,
            Authentication authentication
    ) {
        Page<Note> pageParam = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getStatus, NOTE_STATUS_APPROVED);

        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Note::getTitle, keyword.trim())
                    .or()
                    .like(Note::getAuthor, keyword.trim()));
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(Note::getCategory, category);
        }
        wrapper.orderByDesc(Note::getCreatedAt);

        Page<Note> result = noteMapper.selectPage(pageParam, wrapper);
        ActorIdentity actor = currentUserService.resolveActor(authentication);

        List<NoteListItemVO> records = result.getRecords().stream()
                .map(note -> {
                    enrichNoteMetadata(note, actor);
                    return NoteViewAssembler.toListItemVO(note);
                })
                .collect(Collectors.toList());

        Page<NoteListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    public Page<NoteListItemVO> getMine(Authentication authentication, Integer page, Integer size, Integer status) {
        User user = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u67e5\u770b\u6211\u7684\u624b\u8bb0");
        Page<Note> pageParam = new Page<>(page == null ? 1 : page, size == null ? 20 : size);

        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, user.getId());
        if (status != null) {
            wrapper.eq(Note::getStatus, status);
        }
        wrapper.orderByDesc(Note::getCreatedAt);

        Page<Note> result = noteMapper.selectPage(pageParam, wrapper);
        ActorIdentity actor = new ActorIdentity("user", user.getUsername());

        List<NoteListItemVO> records = result.getRecords().stream()
                .map(note -> {
                    enrichNoteMetadata(note, actor);
                    return NoteViewAssembler.toListItemVO(note);
                })
                .collect(Collectors.toList());

        Page<NoteListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(records);
        return voPage;
    }

    public NoteDetailVO getNoteDetail(Long id, Authentication authentication) {
        Note note = loadAccessibleNote(id, authentication);
        ActorIdentity actor = currentUserService.resolveActor(authentication);
        enrichNoteMetadata(note, actor);
        return NoteViewAssembler.toDetailVO(note);
    }

    @Transactional
    public NoteDetailVO increaseView(Long id, Authentication authentication) {
        Note note = loadAccessibleNote(id, authentication);
        note.setViewCount((note.getViewCount() == null ? 0 : note.getViewCount()) + 1);
        noteMapper.updateById(note);

        enrichNoteMetadata(note, currentUserService.resolveActor(authentication));
        return NoteViewAssembler.toDetailVO(note);
    }

    @Transactional
    public NoteDetailVO createNote(NoteCreateRequest request, Authentication authentication) {
        User user = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u6295\u7a3f\u624b\u8bb0");
        validateNoteSubmission(request.getTitle(), request.getContent());

        Note note = new Note();
        note.setUserId(user.getId());
        note.setAuthor(user.getNickname());
        note.setSummary(request.getSummary());
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setCategory(request.getCategory());
        note.setEmoji(request.getEmoji());
        note.setStatus(NOTE_STATUS_PENDING);
        note.setViewCount(0);
        note.setRejectReason(null);
        note.setAuditSource(null);
        note.setAuditedAt(null);
        LocalDateTime now = LocalDateTime.now();
        note.setCreatedAt(now);
        note.setUpdatedAt(now);

        noteMapper.insert(note);
        return NoteViewAssembler.toDetailVO(note);
    }

    @Transactional
    public NoteDetailVO updateNote(Long id, NoteUpdateRequest request, Authentication authentication) {
        User user = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u7f16\u8f91\u624b\u8bb0");
        Note note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(user.getId())) {
            throw new NotFoundException("\u624b\u8bb0\u4e0d\u5b58\u5728\uff0c\u6216\u4f60\u6ca1\u6709\u7f16\u8f91\u6743\u9650");
        }

        validateNoteSubmission(request.getTitle(), request.getContent());

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setSummary(request.getSummary());
        note.setCategory(request.getCategory());
        note.setEmoji(request.getEmoji());
        note.setStatus(NOTE_STATUS_PENDING);
        note.setRejectReason(null);
        note.setAuditSource(null);
        note.setAuditedAt(null);
        note.setUpdatedAt(LocalDateTime.now());

        noteMapper.updateById(note);
        return NoteViewAssembler.toDetailVO(note);
    }

    @Transactional
    public NoteLikeVO toggleLike(Long id, Authentication authentication) {
        ActorIdentity actor = currentUserService.resolveActor(authentication);
        if (actor.isEmpty()) {
            throw new UnauthorizedException("\u8bf7\u5148\u767b\u5f55\u540e\u518d\u70b9\u8d5e");
        }

        loadAccessibleNote(id, authentication);

        LambdaQueryWrapper<NoteLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteLike::getNoteId, id)
                .eq(NoteLike::getActorType, actor.type())
                .eq(NoteLike::getActorId, actor.id());
        NoteLike existing = noteLikeMapper.selectOne(wrapper);

        boolean liked;
        if (existing != null) {
            noteLikeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            NoteLike noteLike = new NoteLike();
            noteLike.setNoteId(id);
            noteLike.setActorType(actor.type());
            noteLike.setActorId(actor.id());
            noteLike.setCreatedAt(LocalDateTime.now());
            noteLikeMapper.insert(noteLike);
            liked = true;
        }

        Long count = countLikes(id);
        return new NoteLikeVO(id, liked, count);
    }

    public List<NoteCommentVO> getComments(Long id, Authentication authentication) {
        loadAccessibleNote(id, authentication);
        ActorIdentity actor = currentUserService.resolveActor(authentication);
        LambdaQueryWrapper<NoteComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteComment::getNoteId, id).orderByDesc(NoteComment::getCreatedAt);
        List<NoteComment> comments = noteCommentMapper.selectList(wrapper);
        return comments.stream()
                .map(comment -> toCommentVO(comment, actor))
                .collect(Collectors.toList());
    }

    @Transactional
    public NoteCommentVO addComment(Long id, NoteCommentRequest request, Authentication authentication) {
        User user = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u53d1\u8868\u8bc4\u8bba");
        loadAccessibleNote(id, authentication);

        NoteComment comment = new NoteComment();
        comment.setNoteId(id);
        comment.setUserId(user.getId());
        comment.setNickname(user.getNickname());
        comment.setUsername(user.getUsername());
        comment.setContent(request.getContent());
        comment.setCreatedAt(LocalDateTime.now());
        noteCommentMapper.insert(comment);

        return toCommentVO(comment, new ActorIdentity("user", user.getUsername()));
    }

    @Transactional
    public void deleteNote(Long id, Authentication authentication) {
        User user = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u5220\u9664\u624b\u8bb0");
        Note note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(user.getId())) {
            throw new NotFoundException("\u624b\u8bb0\u4e0d\u5b58\u5728\uff0c\u6216\u4f60\u6ca1\u6709\u5220\u9664\u6743\u9650");
        }
        noteMapper.deleteById(id);
    }

    private Note loadAccessibleNote(Long id, Authentication authentication) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new NotFoundException("\u624b\u8bb0\u4e0d\u5b58\u5728");
        }
        if (Integer.valueOf(NOTE_STATUS_APPROVED).equals(note.getStatus())) {
            return note;
        }
        if (currentUserService.isAdmin(authentication)) {
            return note;
        }
        User user = currentUserService.loadDatabaseUser(authentication);
        if (user != null && user.getId() != null && user.getId().equals(note.getUserId())) {
            return note;
        }
        throw new NotFoundException("\u624b\u8bb0\u4e0d\u5b58\u5728");
    }

    private void enrichNoteMetadata(Note note, ActorIdentity actor) {
        if (note == null) {
            return;
        }
        note.setLikeCount(countLikes(note.getId()));
        note.setCommentCount(countComments(note.getId()));
        if (actor != null && !actor.isEmpty()) {
            LambdaQueryWrapper<NoteLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NoteLike::getNoteId, note.getId())
                    .eq(NoteLike::getActorType, actor.type())
                    .eq(NoteLike::getActorId, actor.id());
            note.setLikedByMe(noteLikeMapper.selectCount(wrapper) > 0);
        } else {
            note.setLikedByMe(false);
        }
    }

    private Long countLikes(Long noteId) {
        LambdaQueryWrapper<NoteLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteLike::getNoteId, noteId);
        return noteLikeMapper.selectCount(wrapper);
    }

    private Long countComments(Long noteId) {
        LambdaQueryWrapper<NoteComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteComment::getNoteId, noteId);
        return noteCommentMapper.selectCount(wrapper);
    }

    private NoteCommentVO toCommentVO(NoteComment comment, ActorIdentity actor) {
        NoteCommentVO vo = new NoteCommentVO();
        vo.setId(comment.getId());
        vo.setNoteId(comment.getNoteId());
        vo.setContent(comment.getContent());
        vo.setNickname(comment.getNickname());
        vo.setUsername(comment.getUsername());
        vo.setUserId(comment.getUserId());
        vo.setCreatedAt(comment.getCreatedAt());
        vo.setMine(actor != null && !actor.isEmpty() && "user".equals(actor.type()) && actor.id().equals(comment.getUsername()));
        return vo;
    }

    private void validateNoteSubmission(String title, String content) {
        String safeTitle = title == null ? "" : title.trim();
        String safeContent = content == null ? "" : content.trim();
        String fullText = (safeTitle + "\n" + safeContent).trim();
        LinkedHashSet<String> hitWords = new LinkedHashSet<>();
        String normalized = fullText.toLowerCase();
        for (String bannedWord : HARD_BANNED_WORDS) {
            if (normalized.contains(bannedWord.toLowerCase())) {
                hitWords.add(bannedWord);
            }
        }
        if (!hitWords.isEmpty()) {
            throw new IllegalArgumentException("\u5185\u5bb9\u6821\u9a8c\u672a\u901a\u8fc7\uff0c\u547d\u4e2d\u8fdd\u7981\u8bcd\uff1a"
                    + String.join("\u3001", hitWords)
                    + "\u3002\u8bf7\u4fee\u6539\u540e\u518d\u63d0\u4ea4\u3002");
        }

        AiService.NoteModerationResult result = aiService.moderateNote(title, content);
        if (result == null || result.approved()) {
            return;
        }

        String reason = result.reason() == null ? "" : result.reason().trim();
        if (reason.startsWith("Blocked words:")) {
            reason = "\u547d\u4e2d\u8fdd\u7981\u8bcd\uff1a" + reason.substring("Blocked words:".length()).trim();
        }
        if (reason.isBlank()) {
            reason = "\u5185\u5bb9\u5305\u542b\u4e0d\u9002\u5b9c\u516c\u5f00\u5c55\u793a\u7684\u4fe1\u606f";
        }
        throw new IllegalArgumentException("\u5185\u5bb9\u6821\u9a8c\u672a\u901a\u8fc7\uff0c" + reason + "\u3002\u8bf7\u4fee\u6539\u540e\u518d\u63d0\u4ea4\u3002");
    }
}
