package com.novaleap.api.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestionBankImportServiceValidationTest {

    private final QuestionBankImportService service = new QuestionBankImportService();

    @Test
    void shouldAcceptPlainTextUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.txt",
                "text/plain",
                "Question: JVM?\nAnswer: Java Virtual Machine".getBytes()
        );

        assertDoesNotThrow(() -> service.validateImportFile(file, 5L * 1024L * 1024L));
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.txt",
                "application/json",
                "{\"q\":\"JVM\"}".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> service.validateImportFile(file, 5L * 1024L * 1024L));
    }

    @Test
    void shouldRejectUnsupportedExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.pdf",
                "application/octet-stream",
                "fake".getBytes()
        );

        assertThrows(IllegalArgumentException.class, () -> service.validateImportFile(file, 5L * 1024L * 1024L));
    }
}
