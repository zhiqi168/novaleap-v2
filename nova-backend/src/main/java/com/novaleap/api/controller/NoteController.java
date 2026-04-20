package com.novaleap.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.novaleap.api.common.Result;
import com.novaleap.api.dto.NoteCommentRequest;
import com.novaleap.api.dto.NoteCommentVO;
import com.novaleap.api.dto.NoteLikeVO;
import com.novaleap.api.module.note.dto.NoteCreateRequest;
import com.novaleap.api.module.note.dto.NoteUpdateRequest;
import com.novaleap.api.module.note.service.NoteApplicationService;
import com.novaleap.api.module.note.vo.NoteDetailVO;
import com.novaleap.api.module.note.vo.NoteListItemVO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteApplicationService noteApplicationService;

    public NoteController(NoteApplicationService noteApplicationService) {
        this.noteApplicationService = noteApplicationService;
    }

    @GetMapping
    public Result<Page<NoteListItemVO>> getNoteList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            Authentication authentication
    ) {
        return Result.success(noteApplicationService.getNoteList(page, size, keyword, category, authentication));
    }

    @GetMapping("/mine")
    public Result<Page<NoteListItemVO>> getMine(
            Authentication authentication,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer status
    ) {
        return Result.success(noteApplicationService.getMine(authentication, page, size, status));
    }

    @GetMapping("/{id}")
    public Result<NoteDetailVO> getNoteDetail(@PathVariable Long id, Authentication authentication) {
        return Result.success(noteApplicationService.getNoteDetail(id, authentication));
    }

    @PostMapping("/{id}/view")
    public Result<NoteDetailVO> increaseView(@PathVariable Long id, Authentication authentication) {
        return Result.success(noteApplicationService.increaseView(id, authentication));
    }

    @PostMapping
    public Result<NoteDetailVO> createNote(
            @RequestBody @Valid NoteCreateRequest request,
            Authentication authentication
    ) {
        return Result.success(noteApplicationService.createNote(request, authentication));
    }

    @PutMapping("/{id}")
    public Result<NoteDetailVO> updateNote(
            @PathVariable Long id,
            @RequestBody @Valid NoteUpdateRequest request,
            Authentication authentication
    ) {
        return Result.success(noteApplicationService.updateNote(id, request, authentication));
    }

    @PostMapping("/{id}/like")
    public Result<NoteLikeVO> toggleLike(@PathVariable Long id, Authentication authentication) {
        return Result.success(noteApplicationService.toggleLike(id, authentication));
    }

    @GetMapping("/{id}/comments")
    public Result<List<NoteCommentVO>> getComments(@PathVariable Long id, Authentication authentication) {
        return Result.success(noteApplicationService.getComments(id, authentication));
    }

    @PostMapping("/{id}/comments")
    public Result<NoteCommentVO> addComment(
            @PathVariable Long id,
            @RequestBody @Valid NoteCommentRequest request,
            Authentication authentication
    ) {
        return Result.success(noteApplicationService.addComment(id, request, authentication));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNote(@PathVariable Long id, Authentication authentication) {
        noteApplicationService.deleteNote(id, authentication);
        return Result.success();
    }

    @GetMapping("/hot")
    public Result<List<NoteListItemVO>> getHotNotes(
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            Authentication authentication
    ) {
        return Result.success(noteApplicationService.getHotNoteList(limit, authentication));
    }
}
