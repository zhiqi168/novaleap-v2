package com.novaleap.api.task;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.novaleap.api.entity.Note;
import com.novaleap.api.entity.Question;
import com.novaleap.api.mapper.NoteMapper;
import com.novaleap.api.mapper.QuestionMapper;
import com.novaleap.api.module.note.support.NoteReadCacheSupport;
import com.novaleap.api.module.question.support.QuestionReadCacheSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ContentViewFlushTask {

    private static final Logger log = LoggerFactory.getLogger(ContentViewFlushTask.class);
    private static final int MAX_BATCH_SIZE = 200;

    private final QuestionMapper questionMapper;
    private final NoteMapper noteMapper;
    private final QuestionReadCacheSupport questionReadCacheSupport;
    private final NoteReadCacheSupport noteReadCacheSupport;

    public ContentViewFlushTask(
            QuestionMapper questionMapper,
            NoteMapper noteMapper,
            QuestionReadCacheSupport questionReadCacheSupport,
            NoteReadCacheSupport noteReadCacheSupport
    ) {
        this.questionMapper = questionMapper;
        this.noteMapper = noteMapper;
        this.questionReadCacheSupport = questionReadCacheSupport;
        this.noteReadCacheSupport = noteReadCacheSupport;
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 10000)
    public void flushBufferedViewCounts() {
        flushQuestionViews();
        flushNoteViews();
    }

    private void flushQuestionViews() {
        Map<Long, Long> pending = questionReadCacheSupport.loadPendingViewCounts(MAX_BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }

        int success = 0;
        long totalDelta = 0L;
        for (Map.Entry<Long, Long> entry : pending.entrySet()) {
            Long questionId = entry.getKey();
            long delta = entry.getValue() == null ? 0L : entry.getValue();
            if (questionId == null || delta <= 0L) {
                continue;
            }
            try {
                UpdateWrapper<Question> wrapper = new UpdateWrapper<>();
                wrapper.eq("id", questionId)
                        .eq("status", 1)
                        .setSql("view_count = IFNULL(view_count, 0) + " + delta);
                int updated = questionMapper.update(null, wrapper);
                if (updated > 0) {
                    questionReadCacheSupport.acknowledgePendingViewCount(questionId, delta);
                    success += 1;
                    totalDelta += delta;
                }
            } catch (Exception ex) {
                log.warn("[perf][question-view-flush] failed id={} delta={} msg={}", questionId, delta, ex.getMessage());
            }
        }

        if (success > 0) {
            questionReadCacheSupport.evictQuestionLists();
            log.info("[perf][question-view-flush] flushedKeys={} flushedDelta={}", success, totalDelta);
        }
    }

    private void flushNoteViews() {
        Map<Long, Long> pending = noteReadCacheSupport.loadPendingViewCounts(MAX_BATCH_SIZE);
        if (pending.isEmpty()) {
            return;
        }

        int success = 0;
        long totalDelta = 0L;
        for (Map.Entry<Long, Long> entry : pending.entrySet()) {
            Long noteId = entry.getKey();
            long delta = entry.getValue() == null ? 0L : entry.getValue();
            if (noteId == null || delta <= 0L) {
                continue;
            }
            try {
                UpdateWrapper<Note> wrapper = new UpdateWrapper<>();
                wrapper.eq("id", noteId)
                        .eq("deleted", 0)
                        .setSql("view_count = IFNULL(view_count, 0) + " + delta);
                int updated = noteMapper.update(null, wrapper);
                if (updated > 0) {
                    noteReadCacheSupport.acknowledgePendingViewCount(noteId, delta);
                    success += 1;
                    totalDelta += delta;
                }
            } catch (Exception ex) {
                log.warn("[perf][note-view-flush] failed id={} delta={} msg={}", noteId, delta, ex.getMessage());
            }
        }

        if (success > 0) {
            noteReadCacheSupport.evictPublicLists();
            noteReadCacheSupport.evictAllMineLists();
            log.info("[perf][note-view-flush] flushedKeys={} flushedDelta={}", success, totalDelta);
        }
    }
}
