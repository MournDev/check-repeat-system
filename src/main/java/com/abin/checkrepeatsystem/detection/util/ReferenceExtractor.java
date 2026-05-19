package com.abin.checkrepeatsystem.detection.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ReferenceExtractor {

    private static final Pattern CHINESE_REFERENCE_PATTERN = Pattern.compile(
            "\\[(\\d+)\\]\\s*([\\u4e00-\\u9fa5a-zA-Z0-9\\s]+?)\\s*\\.\\s*([^\\[\\]]+?)\\s*\\[([A-Za-z])\\]\\.?\\s*([^，,]+?)?\\s*[，,]?\\s*(\\d{4})\\s*[，,]?\\s*(\\d+(?:\\(\\d+\\))?(?::\\d+-\\d+)?)?\\s*[。]?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ENGLISH_REFERENCE_PATTERN = Pattern.compile(
            "\\[(\\d+)\\]\\s*([A-Z][a-zA-Z]+(?:\\s+[A-Z]\\.?)?(?:\\s+[a-zA-Z]+)*)\\s*\\.\\s*([^\\.]+?)\\s*\\[([A-Za-z])\\]\\.?\\s*([^，,]+?)?\\s*[，,]?\\s*(\\d{4})\\s*[，,]?\\s*(\\d+(?:\\(\\d+\\))?(?::\\d+-\\d+)?)?\\s*[。]?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern REFERENCE_BLOCK_PATTERN = Pattern.compile(
            "参考文献\\s*[：:]?\\s*\\n?",
            Pattern.CASE_INSENSITIVE
    );

    public ReferenceResult extractReferences(String content) {
        List<Reference> references = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return new ReferenceResult(content, references);
        }

        StringBuilder contentWithoutRefs = new StringBuilder(content);

        List<ExtractedReference> extractedRefs = new ArrayList<>();

        Matcher chineseMatcher = CHINESE_REFERENCE_PATTERN.matcher(content);
        while (chineseMatcher.find()) {
            ExtractedReference ref = parseChineseReference(chineseMatcher);
            if (ref != null) {
                extractedRefs.add(ref);
            }
        }

        Matcher englishMatcher = ENGLISH_REFERENCE_PATTERN.matcher(content);
        while (englishMatcher.find()) {
            ExtractedReference ref = parseEnglishReference(englishMatcher);
            if (ref != null) {
                extractedRefs.add(ref);
            }
        }

        extractedRefs.sort((a, b) -> Integer.compare(b.getStartPos(), a.getStartPos()));

        for (ExtractedReference extractedRef : extractedRefs) {
            int start = extractedRef.getStartPos();
            int end = extractedRef.getEndPos();
            if (start >= 0 && end <= contentWithoutRefs.length() && start < end) {
                contentWithoutRefs.replace(start, end, " ");
            }
        }

        ReferenceResult result = new ReferenceResult();
        result.setOriginalContent(content);
        result.setContentWithoutReferences(contentWithoutRefs.toString().replaceAll("\\s+", " ").trim());
        result.setReferences(convertToReferences(extractedRefs));

        log.debug("提取到 {} 条引用文献", references.size());
        return result;
    }

    private ExtractedReference parseChineseReference(Matcher matcher) {
        try {
            ExtractedReference ref = new ExtractedReference();
            ref.setIndex(Integer.parseInt(matcher.group(1)));
            ref.setAuthor(matcher.group(2).trim());
            ref.setTitle(matcher.group(3).trim());
            ref.setDocType(matcher.group(4).toUpperCase());
            ref.setSource(matcher.group(5) != null ? matcher.group(5).trim() : "");
            ref.setYear(matcher.group(6));
            ref.setVolumeIssue(matcher.group(7) != null ? matcher.group(7).trim() : "");
            ref.setStartPos(matcher.start());
            ref.setEndPos(matcher.end());
            ref.setFullText(matcher.group(0));
            ref.setLanguage("zh");
            return ref;
        } catch (Exception e) {
            log.warn("解析中文引用格式失败: {}", matcher.group(0), e);
            return null;
        }
    }

    private ExtractedReference parseEnglishReference(Matcher matcher) {
        try {
            ExtractedReference ref = new ExtractedReference();
            ref.setIndex(Integer.parseInt(matcher.group(1)));
            ref.setAuthor(matcher.group(2).trim());
            ref.setTitle(matcher.group(3).trim());
            ref.setDocType(matcher.group(4).toUpperCase());
            ref.setSource(matcher.group(5) != null ? matcher.group(5).trim() : "");
            ref.setYear(matcher.group(6));
            ref.setVolumeIssue(matcher.group(7) != null ? matcher.group(7).trim() : "");
            ref.setStartPos(matcher.start());
            ref.setEndPos(matcher.end());
            ref.setFullText(matcher.group(0));
            ref.setLanguage("en");
            return ref;
        } catch (Exception e) {
            log.warn("解析英文引用格式失败: {}", matcher.group(0), e);
            return null;
        }
    }

    private List<Reference> convertToReferences(List<ExtractedReference> extractedRefs) {
        List<Reference> refs = new ArrayList<>();
        for (ExtractedReference extRef : extractedRefs) {
            Reference ref = new Reference();
            ref.setIndex(extRef.getIndex());
            ref.setAuthor(extRef.getAuthor());
            ref.setTitle(extRef.getTitle());
            ref.setDocumentType(getDocumentTypeName(extRef.getDocType()));
            ref.setSource(extRef.getSource());
            ref.setYear(extRef.getYear());
            ref.setVolumeIssue(extRef.getVolumeIssue());
            ref.setPosition(extRef.getStartPos());
            ref.setFullCitation(extRef.getFullText());
            ref.setLanguage(extRef.getLanguage());
            refs.add(ref);
        }
        return refs;
    }

    private String getDocumentTypeName(String docType) {
        if (docType == null) return "其他";
        switch (docType.toUpperCase()) {
            case "J": return "期刊文章";
            case "M": return "图书";
            case "D": return "学位论文";
            case "C": return "会议论文";
            case "N": return "报纸文章";
            case "R": return "报告";
            case "S": return "标准";
            case "P": return "专利";
            default: return "其他";
        }
    }

    @Data
    public static class ReferenceResult {
        private String originalContent;
        private String contentWithoutReferences;
        private List<Reference> references;

        public ReferenceResult() {
        }

        public ReferenceResult(String content, List<Reference> references) {
            this.originalContent = content;
            this.contentWithoutReferences = content;
            this.references = references;
        }
    }

    @Data
    private static class ExtractedReference {
        private int index;
        private String author;
        private String title;
        private String docType;
        private String source;
        private String year;
        private String volumeIssue;
        private int startPos;
        private int endPos;
        private String fullText;
        private String language;
    }

    @Data
    public static class Reference {
        private int index;
        private String author;
        private String title;
        private String documentType;
        private String source;
        private String year;
        private String volumeIssue;
        private int position;
        private String fullCitation;
        private String language;
    }
}