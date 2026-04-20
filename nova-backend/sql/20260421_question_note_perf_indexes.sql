ALTER TABLE questions
  ADD INDEX idx_questions_status_source_category_diff_created (status, source_type, category, difficulty, created_at DESC),
  ADD INDEX idx_questions_status_source_bank_created (status, source_type, custom_bank_id, created_at DESC),
  ADD INDEX idx_questions_status_source_view_created (status, source_type, view_count DESC, created_at DESC);

ALTER TABLE notes
  ADD INDEX idx_notes_status_deleted_created (status, deleted, created_at DESC),
  ADD INDEX idx_notes_status_deleted_view_created (status, deleted, view_count DESC, created_at DESC),
  ADD INDEX idx_notes_user_status_created (user_id, status, created_at DESC);

ALTER TABLE note_likes
  ADD UNIQUE INDEX uk_note_likes_actor (note_id, actor_type, actor_id);

ALTER TABLE note_comments
  ADD INDEX idx_note_comments_note_created (note_id, created_at DESC);
