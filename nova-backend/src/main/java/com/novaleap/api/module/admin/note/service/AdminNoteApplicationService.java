package com.novaleap.api.module.admin.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novaleap.api.common.exception.NotFoundException;
import com.novaleap.api.entity.Note;
import com.novaleap.api.mapper.NoteMapper;
import com.novaleap.api.mapper.NoteLikeMapper;
import com.novaleap.api.mapper.NoteCommentMapper;
import com.novaleap.api.module.admin.note.assembler.AdminNoteViewAssembler;
import com.novaleap.api.module.admin.note.dto.AdminNoteSaveRequest;
import com.novaleap.api.module.admin.note.dto.AdminNoteStatusRequest;
import com.novaleap.api.module.admin.note.vo.AdminNoteDetailVO;
import com.novaleap.api.module.admin.note.vo.AdminNoteListItemVO;
import com.novaleap.api.module.note.support.NoteReadCacheSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminNoteApplicationService {

    private final NoteMapper noteMapper;
    private final NoteLikeMapper noteLikeMapper;
    private final NoteCommentMapper noteCommentMapper;
    private final NoteReadCacheSupport noteReadCacheSupport;

    public AdminNoteApplicationService(
            NoteMapper noteMapper,
            NoteLikeMapper noteLikeMapper,
            NoteCommentMapper noteCommentMapper,
            NoteReadCacheSupport noteReadCacheSupport
    ) {
        this.noteMapper = noteMapper;
        this.noteLikeMapper = noteLikeMapper;
        this.noteCommentMapper = noteCommentMapper;
        this.noteReadCacheSupport = noteReadCacheSupport;
    }

    public Page<AdminNoteListItemVO> getNoteList(
            Integer page,
            Integer size,
            String keyword,
            String category,
            Integer status
    ) {
        Page<Note> pageParam = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Note::getTitle, keyword.trim())
                    .or()
                    .like(Note::getAuthor, keyword.trim()));
        }
        if (category != null && !category.isBlank()) {
            wrapper.eq(Note::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Note::getStatus, status);
        }
        wrapper.orderByDesc(Note::getCreatedAt);

        Page<Note> result = noteMapper.selectPage(pageParam, wrapper);
        Page<AdminNoteListItemVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(AdminNoteViewAssembler::toListItemVO)
                .toList());
        return voPage;
    }

    public AdminNoteDetailVO getNoteDetail(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new NotFoundException("手记不存在");
        }
        return AdminNoteViewAssembler.toDetailVO(note);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminNoteDetailVO createNote(AdminNoteSaveRequest request) {
        Note note = new Note();
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setSummary(request.getSummary());
        note.setCategory(request.getCategory());
        note.setEmoji(request.getEmoji());
        note.setAuthor(request.getAuthor());
        note.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        note.setViewCount(0);
        note.setCreatedAt(LocalDateTime.now());
        note.setUpdatedAt(LocalDateTime.now());
        
        noteMapper.insert(note);
        noteReadCacheSupport.evictPublicLists();
        noteReadCacheSupport.evictAllMineLists();
        noteReadCacheSupport.resetNoteViewCount(note.getId(), note.getViewCount());
        return AdminNoteViewAssembler.toDetailVO(note);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminNoteDetailVO updateNote(Long id, AdminNoteSaveRequest request) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new NotFoundException("手记不存在");
        }
        
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setSummary(request.getSummary());
        note.setCategory(request.getCategory());
        note.setEmoji(request.getEmoji());
        note.setAuthor(request.getAuthor());
        if (request.getStatus() != null) {
            note.setStatus(request.getStatus());
        }
        note.setUpdatedAt(LocalDateTime.now());
        
        noteMapper.updateById(note);
        noteReadCacheSupport.evictNoteReadCaches(id);
        noteReadCacheSupport.resetNoteViewCount(id, note.getViewCount());
        return AdminNoteViewAssembler.toDetailVO(note);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminNoteDetailVO updateNoteStatus(Long id, Integer status, String reason, String rejectReason, AdminNoteStatusRequest body) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new NotFoundException("手记不存在");
        }

        Integer newStatus = status;
        String newRejectReason = rejectReason;
        
        if (body != null) {
            if (body.getStatus() != null) newStatus = body.getStatus();
            if (body.getRejectReason() != null) newRejectReason = body.getRejectReason();
        }

        LambdaUpdateWrapper<Note> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Note::getId, id);
        if (newStatus != null) updateWrapper.set(Note::getStatus, newStatus);
        if (newRejectReason != null) updateWrapper.set(Note::getRejectReason, newRejectReason);
        updateWrapper.set(Note::getAuditedAt, LocalDateTime.now());
        updateWrapper.set(Note::getUpdatedAt, LocalDateTime.now());
        
        noteMapper.update(null, updateWrapper);
        noteReadCacheSupport.evictNoteReadCaches(id);
        
        Note refreshed = noteMapper.selectById(id);
        noteReadCacheSupport.resetNoteViewCount(id, refreshed == null ? 0 : refreshed.getViewCount());
        return AdminNoteViewAssembler.toDetailVO(refreshed);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new NotFoundException("手记不存在");
        }
        noteMapper.deleteById(id);
        noteReadCacheSupport.evictNoteReadCaches(id);
        noteReadCacheSupport.evictNoteViewCount(id);
    }
}
