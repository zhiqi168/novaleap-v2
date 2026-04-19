package com.novaleap.api.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class QuestionBankImportService {

    private static final String CONTENT_TYPE_TEXT = "text/plain";
    private static final String CONTENT_TYPE_OCTET = "application/octet-stream";
    private static final Set<String> SUPPORTED_TYPES = Set.of("txt");

    public record QuestionDraft(String title, String content, String standardAnswer) {
    }

    public record ImportPayload(String fileType, String rawText, List<QuestionDraft> questions) {
    }

    public ImportPayload analyzeFile(String originalFilename, byte[] fileBytes) {
        String fileType = detectFileType(originalFilename);
        String rawText = extractText(fileType, fileBytes);
        List<QuestionDraft> questions = parseQuestions(rawText);
        return new ImportPayload(fileType, rawText, questions);
    }

    public void validateImportFile(MultipartFile file, long maxFileSize) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("璇烽€夋嫨棰樺簱鏂囦欢");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("鏂囦欢澶у皬涓嶈兘瓒呰繃 5MB");
        }
        detectFileType(file.getOriginalFilename());
        String contentType = safe(file.getContentType()).toLowerCase(Locale.ROOT);
        if (StringUtils.hasText(contentType)
                && !CONTENT_TYPE_TEXT.equals(contentType)
                && !CONTENT_TYPE_OCTET.equals(contentType)) {
            throw new IllegalArgumentException("浠呮敮鎸?txt 鏍煎紡鏂囦欢");
        }
    }

    /**
     * 规范格式：
     * 1. 一个题目块 = 上面题目 + 下面答案，至少两行
     * 2. 题目块之间空一行
     * 3. 支持前缀：题目：/ 答案：
     */
    public List<QuestionDraft> parseQuestions(String rawText) {
        String normalized = normalizeText(rawText);
        if (normalized.isBlank()) {
            return Collections.emptyList();
        }

        List<QuestionDraft> drafts = new ArrayList<>();
        String[] blocks = normalized.split("\\n\\s*\\n+");
        for (String block : blocks) {
            QuestionDraft draft = parseBlock(block);
            if (draft != null) {
                drafts.add(draft);
            }
        }
        return drafts;
    }

    private QuestionDraft parseBlock(String block) {
        if (!StringUtils.hasText(block)) {
            return null;
        }
        List<String> lines = block.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        if (lines.size() < 2) {
            return null;
        }

        String questionLine = stripQuestionPrefix(lines.get(0));
        if (!StringUtils.hasText(questionLine)) {
            return null;
        }

        String answerText = stripAnswerPrefix(String.join("\n", lines.subList(1, lines.size())).trim());
        if (!StringUtils.hasText(answerText)) {
            return null;
        }

        String title = questionLine.length() <= 80
                ? questionLine
                : questionLine.substring(0, 80).trim() + "...";
        return new QuestionDraft(title, questionLine, answerText);
    }

    private String detectFileType(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("请上传 txt 文件");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!SUPPORTED_TYPES.contains(ext)) {
            throw new IllegalArgumentException("仅支持 txt 格式，题目在上、答案在下，题目之间空一行");
        }
        return ext;
    }

    private String extractText(String fileType, byte[] fileBytes) {
        if (!"txt".equals(fileType)) {
            throw new IllegalArgumentException("仅支持 txt 格式");
        }
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        return decodeTxt(fileBytes);
    }

    private String decodeTxt(byte[] fileBytes) {
        String utf8Text = normalizeText(new String(fileBytes, StandardCharsets.UTF_8));
        long replacementCount = utf8Text.chars().filter(ch -> ch == '\uFFFD').count();
        if (replacementCount == 0) {
            return utf8Text;
        }
        return normalizeText(new String(fileBytes, Charset.forName("GB18030")));
    }

    private String stripQuestionPrefix(String value) {
        return value
                .replaceFirst("(?i)^\\s*(题目|问题|question)\\s*[:：]?\\s*", "")
                .trim();
    }

    private String stripAnswerPrefix(String value) {
        return value
                .replaceFirst("(?i)^\\s*(答案|参考答案|标准答案|answer)\\s*[:：]?\\s*", "")
                .trim();
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
