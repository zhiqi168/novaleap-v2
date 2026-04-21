package com.novaleap.api.module.note.assembler;

import com.novaleap.api.entity.Note;
import com.novaleap.api.module.note.vo.NoteDetailVO;
import com.novaleap.api.module.note.vo.NoteListItemVO;

public final class NoteViewAssembler {

    private NoteViewAssembler() {
    }

    public static NoteListItemVO toListItemVO(Note note) {
        NoteListItemVO vo = new NoteListItemVO();
        fillCommon(note, vo);
        return vo;
    }

    public static NoteDetailVO toDetailVO(Note note) {
        NoteDetailVO vo = new NoteDetailVO();
        fillCommon(note, vo);
        vo.setContent(note == null ? null : note.getContent());
        return vo;
    }

    private static void fillCommon(Note note, NoteListItemVO vo) {
        if (note == null || vo == null) {
            return;
        }
        vo.setId(note.getId());
        vo.setTitle(note.getTitle());
        vo.setSummary(note.getSummary());
        vo.setCategory(note.getCategory());
        vo.setEmoji(note.getEmoji());
        vo.setAuthor(note.getAuthor());
        vo.setViewCount(note.getViewCount());
        vo.setStatus(note.getStatus());
        vo.setRejectReason(note.getRejectReason());
        vo.setCreatedAt(note.getCreatedAt());
        vo.setUpdatedAt(note.getUpdatedAt());
        vo.setLikeCount(note.getLikeCount());
        vo.setCommentCount(note.getCommentCount());
        vo.setLikedByMe(Boolean.TRUE.equals(note.getLikedByMe()));
        vo.setWordCount(wordCountOf(note.getContent()));
    }

    private static void fillCommon(Note note, NoteDetailVO vo) {
        if (note == null || vo == null) {
            return;
        }
        vo.setId(note.getId());
        vo.setTitle(note.getTitle());
        vo.setSummary(note.getSummary());
        vo.setCategory(note.getCategory());
        vo.setEmoji(note.getEmoji());
        vo.setAuthor(note.getAuthor());
        vo.setViewCount(note.getViewCount());
        vo.setStatus(note.getStatus());
        vo.setRejectReason(note.getRejectReason());
        vo.setCreatedAt(note.getCreatedAt());
        vo.setUpdatedAt(note.getUpdatedAt());
        vo.setLikeCount(note.getLikeCount());
        vo.setCommentCount(note.getCommentCount());
        vo.setLikedByMe(Boolean.TRUE.equals(note.getLikedByMe()));
        vo.setWordCount(wordCountOf(note.getContent()));
    }

    private static int wordCountOf(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return content.length();
    }
}
