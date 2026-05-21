package com.novaleap.api.module.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.novaleap.api.module.note.support.NoteReadCacheSupport;
import com.novaleap.api.module.note.vo.NoteDetailVO;
import com.novaleap.api.module.note.vo.NoteListItemVO;
import com.novaleap.api.module.system.security.ActorIdentity;
import com.novaleap.api.module.system.security.CurrentUserService;
import com.novaleap.api.service.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NoteApplicationService {

    private static final int NOTE_STATUS_PENDING = 0;
    private static final int NOTE_STATUS_APPROVED = 1;
    private static final int DEFAULT_HOT_LIMIT = 10;
    private static final int MAX_HOT_LIMIT = 20;
    private static final int HOT_KEY_RETRY_TIMES = 3;
    private static final long HOT_KEY_RETRY_SLEEP_MS = 40L;
    private static final Logger log = LoggerFactory.getLogger(NoteApplicationService.class);
    private static final Set<String> HARD_BANNED_WORDS = Set.of(
            "\u5438\u6bd2", "\u6bd2\u54c1", "\u8d29\u6bd2", "\u6d89\u6bd2",
            "\u8d4c\u535a", "\u535a\u5f69", "\u8d4c\u94b1", "\u7f51\u8d4c",
            "\u8bc8\u9a97", "\u6d17\u94b1", "\u5237\u5355", "\u4f20\u9500",
            "\u66b4\u6050", "\u6050\u6016\u88ad\u51fb", "\u7206\u70b8\u7269", "\u67aa\u652f", "\u5236\u67aa",
            "\u4ec7\u6068\u8a00\u8bba", "\u6781\u7aef\u4e3b\u4e49",
            "\u8272\u60c5", "\u7ea6\u70ae", "\u6210\u4eba\u89c6\u9891", "\u5356\u6deb",
            "\u8349\u6ce5\u9a6c", "\u4f60\u5988", "\u50bb\u903c", "\u64cd\u4f60\u5988"
    );

    private final NoteMapper noteMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteCommentMapper noteCommentMapper;
    private final CurrentUserService currentUserService;
    private final AiService aiService;
    private final NoteReadCacheSupport noteReadCacheSupport;

    public NoteApplicationService(
            NoteMapper noteMapper,
            NoteLikeMapper noteLikeMapper,
            NoteCommentMapper noteCommentMapper,
            CurrentUserService currentUserService,
            AiService aiService,
            NoteReadCacheSupport noteReadCacheSupport
    ) {
        this.noteMapper = noteMapper;
        this.noteLikeMapper = noteLikeMapper;
        this.noteCommentMapper = noteCommentMapper;
        this.currentUserService = currentUserService;
        this.aiService = aiService;
        this.noteReadCacheSupport = noteReadCacheSupport;
    }

    public Page<NoteListItemVO> getNoteList(
            Integer page,
            Integer size,
            String keyword,
            String category,
            Authentication authentication
    ) {
        long startNs = System.nanoTime();
        ActorIdentity actor = currentUserService.resolveActor(authentication);
        String cacheKey = noteReadCacheSupport.publicListKey(page, size, keyword, category);
        Page<NoteListItemVO> cached = noteReadCacheSupport.readNoteListPage(cacheKey);
        if (cached != null) {
            applyResolvedNoteListViewCounts(cached.getRecords());
            hydrateLikedState(cached.getRecords(), actor);
            log.info("[perf][note-list] cache-hit scope=public page={} size={} keyword={} category={} records={} total={} tookMs={}",
                    normalizePage(page), normalizeSize(size), safe(keyword), safe(category),
                    cached.getRecords() == null ? 0 : cached.getRecords().size(), cached.getTotal(), elapsedMs(startNs));
            return cached;
        }

        Page<Note> pageParam = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                        Note::getId,
                        Note::getTitle,
                        Note::getSummary,
                        Note::getCategory,
                        Note::getEmoji,
                        Note::getAuthor,
                        Note::getViewCount,
                        Note::getStatus,
                        Note::getRejectReason,
                        Note::getCreatedAt,
                        Note::getUpdatedAt
                )
                .eq(Note::getStatus, NOTE_STATUS_APPROVED);

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
        enrichNoteMetadata(result.getRecords(), actor);

        Page<NoteListItemVO> voPage = toNoteListPage(result.getRecords(), result);
        noteReadCacheSupport.writePublicListPage(cacheKey, cloneWithoutLikeState(voPage));
        applyResolvedNoteListViewCounts(voPage.getRecords());
        log.info("[perf][note-list] cache-miss scope=public page={} size={} keyword={} category={} records={} total={} tookMs={}",
                normalizePage(page), normalizeSize(size), safe(keyword), safe(category),
                voPage.getRecords() == null ? 0 : voPage.getRecords().size(), voPage.getTotal(), elapsedMs(startNs));
        return voPage;
    }

    public Page<NoteListItemVO> getMine(Authentication authentication, Integer page, Integer size, Integer status) {
        long startNs = System.nanoTime();
        User user = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u67e5\u770b\u6211\u7684\u624b\u8bb0");
        String cacheKey = noteReadCacheSupport.mineListKey(user.getId(), page, size, status);
        Page<NoteListItemVO> cached = noteReadCacheSupport.readNoteListPage(cacheKey);
        if (cached != null) {
            applyResolvedNoteListViewCounts(cached.getRecords());
            log.info("[perf][note-list] cache-hit scope=mine userId={} page={} size={} status={} records={} total={} tookMs={}",
                    user.getId(), normalizePage(page), normalizeMineSize(size), status,
                    cached.getRecords() == null ? 0 : cached.getRecords().size(), cached.getTotal(), elapsedMs(startNs));
            return cached;
        }

        Page<Note> pageParam = new Page<>(page == null ? 1 : page, size == null ? 20 : size);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                        Note::getId,
                        Note::getTitle,
                        Note::getSummary,
                        Note::getCategory,
                        Note::getEmoji,
                        Note::getAuthor,
                        Note::getViewCount,
                        Note::getStatus,
                        Note::getRejectReason,
                        Note::getCreatedAt,
                        Note::getUpdatedAt,
                        Note::getUserId
                )
                .eq(Note::getUserId, user.getId());
        if (status != null) {
            wrapper.eq(Note::getStatus, status);
        }
        wrapper.orderByDesc(Note::getCreatedAt);

        Page<Note> result = noteMapper.selectPage(pageParam, wrapper);
        ActorIdentity actor = new ActorIdentity("user", user.getUsername());
        enrichNoteMetadata(result.getRecords(), actor);

        Page<NoteListItemVO> voPage = toNoteListPage(result.getRecords(), result);
        noteReadCacheSupport.writeMineListPage(cacheKey, voPage);
        applyResolvedNoteListViewCounts(voPage.getRecords());
        log.info("[perf][note-list] cache-miss scope=mine userId={} page={} size={} status={} records={} total={} tookMs={}",
                user.getId(), normalizePage(page), normalizeMineSize(size), status,
                voPage.getRecords() == null ? 0 : voPage.getRecords().size(), voPage.getTotal(), elapsedMs(startNs));
        return voPage;
    }

    public NoteDetailVO getNoteDetail(Long id, Authentication authentication) {
        long startNs = System.nanoTime();
        ActorIdentity actor = currentUserService.resolveActor(authentication);
        NoteDetailVO cached = noteReadCacheSupport.readNoteDetail(id);
        if (cached != null) {
            cached.setViewCount(noteReadCacheSupport.resolveNoteViewCount(id, cached.getViewCount()));
            hydrateLikedState(cached, actor);
            log.info("[perf][note-detail] cache-hit id={} tookMs={}", id, elapsedMs(startNs));
            return cached;
        }

        String lockToken = noteReadCacheSupport.tryLockNoteDetail(id);
        if (lockToken == null) {
            NoteDetailVO waitedCache = waitForNoteDetailCache(id);
            if (waitedCache != null) {
                waitedCache.setViewCount(noteReadCacheSupport.resolveNoteViewCount(id, waitedCache.getViewCount()));
                hydrateLikedState(waitedCache, actor);
                log.info("[perf][note-detail] cache-hit-after-wait id={} tookMs={}", id, elapsedMs(startNs));
                return waitedCache;
            }
        }

        try {
            if (lockToken != null) {
                NoteDetailVO doubleChecked = noteReadCacheSupport.readNoteDetail(id);
                if (doubleChecked != null) {
                    doubleChecked.setViewCount(noteReadCacheSupport.resolveNoteViewCount(id, doubleChecked.getViewCount()));
                    hydrateLikedState(doubleChecked, actor);
                    log.info("[perf][note-detail] cache-hit-after-lock id={} tookMs={}", id, elapsedMs(startNs));
                    return doubleChecked;
                }
            }

            Note note = loadAccessibleNote(id, authentication);
            enrichNoteMetadata(note, actor);
            NoteDetailVO detail = NoteViewAssembler.toDetailVO(note);
            detail.setViewCount(noteReadCacheSupport.resolveNoteViewCount(id, detail.getViewCount()));
            if (Integer.valueOf(NOTE_STATUS_APPROVED).equals(note.getStatus())) {
                noteReadCacheSupport.writeNoteDetail(id, copyWithoutLikeState(detail));
            }
            log.info("[perf][note-detail] cache-miss id={} status={} tookMs={}", id, note.getStatus(), elapsedMs(startNs));
            return detail;
        } finally {
            noteReadCacheSupport.unlockNoteDetail(id, lockToken);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public NoteDetailVO increaseView(Long id, Authentication authentication) {
        long startNs = System.nanoTime();
        ActorIdentity actor = currentUserService.resolveActor(authentication);
        NoteDetailVO cachedDetail = noteReadCacheSupport.readNoteDetail(id);
        if (cachedDetail != null) {
            int nextCount = noteReadCacheSupport.incrementAndGetNoteViewCount(id, cachedDetail.getViewCount() == null ? 0 : cachedDetail.getViewCount());
            NoteDetailVO response = copyWithoutLikeState(cachedDetail);
            response.setViewCount(nextCount);
            noteReadCacheSupport.writeNoteDetail(id, response);
            hydrateLikedState(response, actor);
            log.info("[perf][note-view] buffered id={} nextCount={} tookMs={}", id, nextCount, elapsedMs(startNs));
            return response;
        }

        Note note = loadAccessibleNote(id, authentication);
        note.setViewCount(noteReadCacheSupport.incrementAndGetNoteViewCount(id, note.getViewCount() == null ? 0 : note.getViewCount()));
        enrichNoteMetadata(note, actor);
        if (Integer.valueOf(NOTE_STATUS_APPROVED).equals(note.getStatus())) {
            noteReadCacheSupport.writeNoteDetail(id, copyWithoutLikeState(NoteViewAssembler.toDetailVO(note)));
        }
        log.info("[perf][note-view] fallback-buffered id={} nextCount={} tookMs={}", id, note.getViewCount(), elapsedMs(startNs));
        return NoteViewAssembler.toDetailVO(note);
    }

    public List<NoteListItemVO> getHotNoteList(Integer limit, Authentication authentication) {
        ActorIdentity actor = currentUserService.resolveActor(authentication);
        int finalLimit = normalizeHotLimit(limit);
        List<Long> hotIds = noteReadCacheSupport.readHotNoteIds(finalLimit);
        if (hotIds.isEmpty()) {
            LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(
                            Note::getId,
                            Note::getTitle,
                            Note::getSummary,
                            Note::getCategory,
                            Note::getEmoji,
                            Note::getAuthor,
                            Note::getViewCount,
                            Note::getStatus,
                            Note::getRejectReason,
                            Note::getCreatedAt,
                            Note::getUpdatedAt
                    )
                    .eq(Note::getStatus, NOTE_STATUS_APPROVED)
                    .orderByDesc(Note::getViewCount)
                    .orderByDesc(Note::getCreatedAt)
                    .last("LIMIT " + finalLimit);
            List<NoteListItemVO> fallback = questionlessNoteList(wrapper, actor);
            applyResolvedNoteListViewCounts(fallback);
            hydrateLikedState(fallback, actor);
            return fallback;
        }

        Map<Long, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < hotIds.size(); i++) {
            orderMap.put(hotIds.get(i), i);
        }
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                        Note::getId,
                        Note::getTitle,
                        Note::getSummary,
                        Note::getCategory,
                        Note::getEmoji,
                        Note::getAuthor,
                        Note::getViewCount,
                        Note::getStatus,
                        Note::getRejectReason,
                        Note::getCreatedAt,
                        Note::getUpdatedAt
                )
                .eq(Note::getStatus, NOTE_STATUS_APPROVED)
                .in(Note::getId, hotIds);
        List<NoteListItemVO> hotList = questionlessNoteList(wrapper, actor).stream()
                .sorted((left, right) -> Integer.compare(
                        orderMap.getOrDefault(left.getId(), Integer.MAX_VALUE),
                        orderMap.getOrDefault(right.getId(), Integer.MAX_VALUE)
                ))
                .limit(finalLimit)
                .toList();
        applyResolvedNoteListViewCounts(hotList);
        hydrateLikedState(hotList, actor);
        return hotList;
    }

    @Transactional(rollbackFor = Exception.class)
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
        noteReadCacheSupport.evictMineLists(user.getId());
        noteReadCacheSupport.resetNoteViewCount(note.getId(), 0);
        return NoteViewAssembler.toDetailVO(note);
    }

    @Transactional(rollbackFor = Exception.class)
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
        noteReadCacheSupport.evictNoteReadCaches(id);
        noteReadCacheSupport.evictMineLists(user.getId());
        return NoteViewAssembler.toDetailVO(note);
    }

    @Transactional(rollbackFor = Exception.class)
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
        noteReadCacheSupport.writeLikedState(actor, id, liked);
        noteReadCacheSupport.evictNoteReadCaches(id);
        noteReadCacheSupport.evictAllMineLists();
        return new NoteLikeVO(id, liked, count);
    }

    public List<NoteCommentVO> getComments(Long id, Authentication authentication) {
        long startNs = System.nanoTime();
        ActorIdentity actor = currentUserService.resolveActor(authentication);
        List<NoteCommentVO> cached = noteReadCacheSupport.readComments(id);
        if (cached != null) {
            List<NoteCommentVO> result = cloneCommentsWithMineState(cached, actor);
            log.info("[perf][note-comments] cache-hit id={} size={} tookMs={}", id, result.size(), elapsedMs(startNs));
            return result;
        }

        Note note = loadAccessibleNote(id, authentication);
        LambdaQueryWrapper<NoteComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteComment::getNoteId, id).orderByDesc(NoteComment::getCreatedAt);
        List<NoteComment> comments = noteCommentMapper.selectList(wrapper);
        List<NoteCommentVO> result = comments.stream()
                .map(comment -> toCommentVO(comment, actor))
                .collect(Collectors.toList());
        if (Integer.valueOf(NOTE_STATUS_APPROVED).equals(note.getStatus())) {
            noteReadCacheSupport.writeComments(id, cloneCommentsWithoutMineState(result));
        }
        log.info("[perf][note-comments] cache-miss id={} size={} tookMs={}", id, result.size(), elapsedMs(startNs));
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
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

        noteReadCacheSupport.evictNoteReadCaches(id);
        noteReadCacheSupport.evictAllMineLists();
        return toCommentVO(comment, new ActorIdentity("user", user.getUsername()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long id, Authentication authentication) {
        User user = currentUserService.requireDatabaseUser(authentication, "\u8bf7\u5148\u767b\u5f55\u540e\u518d\u5220\u9664\u624b\u8bb0");
        Note note = noteMapper.selectById(id);
        if (note == null || !note.getUserId().equals(user.getId())) {
            throw new NotFoundException("\u624b\u8bb0\u4e0d\u5b58\u5728\uff0c\u6216\u4f60\u6ca1\u6709\u5220\u9664\u6743\u9650");
        }
        noteMapper.deleteById(id);
        noteReadCacheSupport.evictNoteReadCaches(id);
        noteReadCacheSupport.evictMineLists(user.getId());
        noteReadCacheSupport.evictNoteViewCount(id);
    }

    private Note loadAccessibleNote(Long id, Authentication authentication) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                        Note::getId,
                        Note::getTitle,
                        Note::getContent,
                        Note::getSummary,
                        Note::getCategory,
                        Note::getEmoji,
                        Note::getAuthor,
                        Note::getUserId,
                        Note::getViewCount,
                        Note::getStatus,
                        Note::getRejectReason,
                        Note::getAuditSource,
                        Note::getAuditedAt,
                        Note::getCreatedAt,
                        Note::getUpdatedAt
                )
                .eq(Note::getId, id)
                .last("LIMIT 1");
        Note note = noteMapper.selectOne(wrapper);
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
        enrichNoteMetadata(List.of(note), actor);
    }

    private void enrichNoteMetadata(List<Note> notes, ActorIdentity actor) {
        if (notes == null || notes.isEmpty()) {
            return;
        }

        List<Long> noteIds = notes.stream()
                .map(Note::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (noteIds.isEmpty()) {
            return;
        }

        Map<Long, Long> likeCountMap = loadLikeCountMap(noteIds);
        Map<Long, Long> commentCountMap = loadCommentCountMap(noteIds);
        Set<Long> likedIds = loadLikedNoteIds(noteIds, actor);

        for (Note note : notes) {
            Long noteId = note.getId();
            note.setLikeCount(likeCountMap.getOrDefault(noteId, 0L));
            note.setCommentCount(commentCountMap.getOrDefault(noteId, 0L));
            note.setLikedByMe(likedIds.contains(noteId));
        }
    }

    private Long countLikes(Long noteId) {
        LambdaQueryWrapper<NoteLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteLike::getNoteId, noteId);
        Long count = noteLikeMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    private Long countComments(Long noteId) {
        LambdaQueryWrapper<NoteComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoteComment::getNoteId, noteId);
        Long count = noteCommentMapper.selectCount(wrapper);
        return count == null ? 0L : count;
    }

    private Map<Long, Long> loadLikeCountMap(List<Long> noteIds) {
        QueryWrapper<NoteLike> wrapper = new QueryWrapper<>();
        wrapper.select("note_id AS noteId", "COUNT(*) AS total")
                .in("note_id", noteIds)
                .groupBy("note_id");
        return toLongCountMap(noteLikeMapper.selectMaps(wrapper), "noteId");
    }

    private Map<Long, Long> loadCommentCountMap(List<Long> noteIds) {
        QueryWrapper<NoteComment> wrapper = new QueryWrapper<>();
        wrapper.select("note_id AS noteId", "COUNT(*) AS total")
                .in("note_id", noteIds)
                .groupBy("note_id");
        return toLongCountMap(noteCommentMapper.selectMaps(wrapper), "noteId");
    }

    private Set<Long> loadLikedNoteIds(List<Long> noteIds, ActorIdentity actor) {
        if (actor == null || actor.isEmpty() || noteIds == null || noteIds.isEmpty()) {
            return Set.of();
        }
        Map<Long, Boolean> cachedLikedState = noteReadCacheSupport.readLikedStateMap(actor, noteIds);
        if (cachedLikedState.size() == noteIds.size()) {
            return cachedLikedState.entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }

        LambdaQueryWrapper<NoteLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(NoteLike::getNoteId)
                .eq(NoteLike::getActorType, actor.type())
                .eq(NoteLike::getActorId, actor.id())
                .in(NoteLike::getNoteId, noteIds);
        Set<Long> likedIds = noteLikeMapper.selectList(wrapper).stream()
                .map(NoteLike::getNoteId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        noteReadCacheSupport.writeLikedStates(actor, noteIds, likedIds);
        return likedIds;
    }

    private Map<Long, Long> toLongCountMap(List<Map<String, Object>> rows, String idKey) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = asLong(row.get(idKey));
            if (id == null) {
                continue;
            }
            result.put(id, asLongValue(row.get("total")));
        }
        return result;
    }

    private List<NoteListItemVO> questionlessNoteList(LambdaQueryWrapper<Note> wrapper, ActorIdentity actor) {
        List<Note> notes = noteMapper.selectList(wrapper);
        enrichNoteMetadata(notes, actor);
        return notes.stream().map(NoteViewAssembler::toListItemVO).toList();
    }

    private void applyResolvedNoteListViewCounts(List<NoteListItemVO> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<Long, Integer> fallbackMap = new HashMap<>();
        for (NoteListItemVO item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }
            fallbackMap.put(item.getId(), item.getViewCount() == null ? 0 : item.getViewCount());
        }
        Map<Long, Integer> mergedMap = noteReadCacheSupport.resolveNoteViewCountMap(fallbackMap);
        for (NoteListItemVO item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }
            Integer merged = mergedMap.get(item.getId());
            if (merged != null) {
                item.setViewCount(merged);
            }
        }
    }

    private void hydrateLikedState(List<NoteListItemVO> items, ActorIdentity actor) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<Long> noteIds = items.stream()
                .map(NoteListItemVO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Set<Long> likedIds = loadLikedNoteIds(noteIds, actor);
        for (NoteListItemVO item : items) {
            item.setLikedByMe(likedIds.contains(item.getId()));
        }
    }

    private void hydrateLikedState(NoteDetailVO detail, ActorIdentity actor) {
        if (detail == null || detail.getId() == null) {
            return;
        }
        Set<Long> likedIds = loadLikedNoteIds(List.of(detail.getId()), actor);
        detail.setLikedByMe(likedIds.contains(detail.getId()));
    }

    private Page<NoteListItemVO> toNoteListPage(List<Note> notes, Page<Note> source) {
        Page<NoteListItemVO> voPage = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        voPage.setRecords(notes == null
                ? List.of()
                : notes.stream().map(NoteViewAssembler::toListItemVO).toList());
        return voPage;
    }

    private Page<NoteListItemVO> cloneWithoutLikeState(Page<NoteListItemVO> source) {
        if (source == null) {
            return null;
        }
        Page<NoteListItemVO> target = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        target.setRecords(source.getRecords() == null
                ? List.of()
                : source.getRecords().stream().map(this::copyWithoutLikeState).toList());
        return target;
    }

    private NoteListItemVO copyWithoutLikeState(NoteListItemVO source) {
        if (source == null) {
            return null;
        }
        NoteListItemVO target = new NoteListItemVO();
        target.setId(source.getId());
        target.setTitle(source.getTitle());
        target.setSummary(source.getSummary());
        target.setCategory(source.getCategory());
        target.setEmoji(source.getEmoji());
        target.setAuthor(source.getAuthor());
        target.setViewCount(source.getViewCount());
        target.setStatus(source.getStatus());
        target.setRejectReason(source.getRejectReason());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setLikeCount(source.getLikeCount());
        target.setCommentCount(source.getCommentCount());
        target.setLikedByMe(false);
        target.setWordCount(source.getWordCount());
        return target;
    }

    private NoteDetailVO copyWithoutLikeState(NoteDetailVO source) {
        if (source == null) {
            return null;
        }
        NoteDetailVO target = new NoteDetailVO();
        target.setId(source.getId());
        target.setTitle(source.getTitle());
        target.setSummary(source.getSummary());
        target.setCategory(source.getCategory());
        target.setEmoji(source.getEmoji());
        target.setAuthor(source.getAuthor());
        target.setViewCount(source.getViewCount());
        target.setStatus(source.getStatus());
        target.setRejectReason(source.getRejectReason());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        target.setLikeCount(source.getLikeCount());
        target.setCommentCount(source.getCommentCount());
        target.setLikedByMe(false);
        target.setWordCount(source.getWordCount());
        target.setContent(source.getContent());
        return target;
    }

    private List<NoteCommentVO> cloneCommentsWithoutMineState(List<NoteCommentVO> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(comment -> copyComment(comment, false))
                .toList();
    }

    private List<NoteCommentVO> cloneCommentsWithMineState(List<NoteCommentVO> source, ActorIdentity actor) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .map(comment -> copyComment(comment, isMyComment(comment, actor)))
                .toList();
    }

    private NoteCommentVO copyComment(NoteCommentVO source, boolean mine) {
        NoteCommentVO target = new NoteCommentVO();
        target.setId(source.getId());
        target.setNoteId(source.getNoteId());
        target.setContent(source.getContent());
        target.setNickname(source.getNickname());
        target.setUsername(source.getUsername());
        target.setUserId(source.getUserId());
        target.setCreatedAt(source.getCreatedAt());
        target.setMine(mine);
        return target;
    }

    private boolean isMyComment(NoteCommentVO comment, ActorIdentity actor) {
        return comment != null
                && actor != null
                && !actor.isEmpty()
                && "user".equals(actor.type())
                && actor.id().equals(comment.getUsername());
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignore) {
            return null;
        }
    }

    private long asLongValue(Object value) {
        Long parsed = asLong(value);
        return parsed == null ? 0L : parsed;
    }

    private int normalizePage(Integer value) {
        return value == null || value < 1 ? 1 : value;
    }

    private int normalizeSize(Integer value) {
        return value == null || value < 1 ? 10 : value;
    }

    private int normalizeMineSize(Integer value) {
        return value == null || value < 1 ? 20 : value;
    }

    private int normalizeHotLimit(Integer value) {
        if (value == null || value <= 0) {
            return DEFAULT_HOT_LIMIT;
        }
        return Math.min(value, MAX_HOT_LIMIT);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private long elapsedMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    private NoteDetailVO waitForNoteDetailCache(Long id) {
        for (int i = 0; i < HOT_KEY_RETRY_TIMES; i++) {
            sleepQuietly(HOT_KEY_RETRY_SLEEP_MS);
            NoteDetailVO cached = noteReadCacheSupport.readNoteDetail(id);
            if (cached != null) {
                return cached;
            }
        }
        return null;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
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
