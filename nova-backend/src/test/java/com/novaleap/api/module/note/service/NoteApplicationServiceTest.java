package com.novaleap.api.module.note.service;

import com.novaleap.api.common.exception.NotFoundException;
import com.novaleap.api.entity.Note;
import com.novaleap.api.entity.User;
import com.novaleap.api.mapper.NoteCommentMapper;
import com.novaleap.api.mapper.NoteLikeMapper;
import com.novaleap.api.mapper.NoteMapper;
import com.novaleap.api.module.note.dto.NoteCreateRequest;
import com.novaleap.api.module.system.security.CurrentUserService;
import com.novaleap.api.service.AiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteApplicationServiceTest {

    @Mock
    private NoteMapper noteMapper;

    @Mock
    private NoteLikeMapper noteLikeMapper;

    @Mock
    private NoteCommentMapper noteCommentMapper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private AiService aiService;

    @Mock
    private Authentication authentication;

    @Test
    void shouldCreatePendingNoteWithInitializedViewCount() {
        NoteApplicationService service = new NoteApplicationService(
                noteMapper,
                noteLikeMapper,
                noteCommentMapper,
                currentUserService,
                aiService
        );
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setNickname("Alice");
        when(currentUserService.requireDatabaseUser(eq(authentication), anyString())).thenReturn(user);
        when(aiService.moderateNote("Title", "Content"))
                .thenReturn(new AiService.NoteModerationResult(true, "", "mock"));

        NoteCreateRequest request = new NoteCreateRequest();
        request.setTitle("Title");
        request.setContent("Content");
        request.setSummary("Summary");
        request.setCategory("java");
        request.setEmoji("🙂");

        service.createNote(request, authentication);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteMapper).insert(captor.capture());
        Note inserted = captor.getValue();
        assertEquals(0, inserted.getStatus());
        assertEquals(0, inserted.getViewCount());
        assertEquals(7L, inserted.getUserId());
        assertNotNull(inserted.getCreatedAt());
        assertNotNull(inserted.getUpdatedAt());
    }

    @Test
    void shouldHidePendingNoteFromUnrelatedUser() {
        NoteApplicationService service = new NoteApplicationService(
                noteMapper,
                noteLikeMapper,
                noteCommentMapper,
                currentUserService,
                aiService
        );
        Note note = new Note();
        note.setId(1L);
        note.setStatus(0);
        note.setUserId(42L);
        when(noteMapper.selectById(1L)).thenReturn(note);
        when(currentUserService.isAdmin(authentication)).thenReturn(false);
        when(currentUserService.loadDatabaseUser(authentication)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> service.getNoteDetail(1L, authentication));
    }
}
