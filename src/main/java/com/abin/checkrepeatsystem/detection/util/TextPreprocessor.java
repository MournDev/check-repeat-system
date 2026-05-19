package com.abin.checkrepeatsystem.detection.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class TextPreprocessor {

    // 目录模式匹配
    private static final Pattern TABLE_OF_CONTENTS_PATTERN = Pattern.compile(
            "(?:目录|contents|table of contents|目\\s*录)\\s*[：:]?\\s*\\n(?:[\\s\\S]*?)(?=\\n\\d+\\s*[第]?[章节]|\\n[一二三四五六七八九十]+[章节]|\\n[1-9]\\.|\\Z)",
            Pattern.CASE_INSENSITIVE
    );

    // 章节标题模式（匹配"第X章"、"1."、"1.1"、"第一章"等）
    private static final Pattern CHAPTER_TITLE_PATTERN = Pattern.compile(
            "(?:^|\\n)\\s*(?:第[一二三四五六七八九十\\d]+[章节]|[一二三四五六七八九十]+[章节]|[\\d]+(?:\\.[\\d]+)*)\\s+[^\\n]{1,50}(?=\\n|$)",
            Pattern.MULTILINE
    );

    // 页码模式
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile(
            "\\n\\s*[\\d]{1,4}\\s*\\n",
            Pattern.MULTILINE
    );

    // 致谢部分模式（匹配从"致谢"开始到下一个章节或文档结束）
    private static final Pattern ACKNOWLEDGEMENT_PATTERN = Pattern.compile(
            "(?:^|\\n)\\s*(致谢|acknowledgement|acknowledgments?)\\s*[：:]?\\s*\\n(?:[\\s\\S]*?)(?=\\n\\d+\\s*[第]?[章节]|\\n[一二三四五六七八九十]+[章节]|\\n[1-9]\\.|\\n参考文献|\\Z)",
            Pattern.CASE_INSENSITIVE
    );

    // 声明部分模式（原创性声明、版权声明等）
    private static final Pattern DECLARATION_PATTERN = Pattern.compile(
            "(?:^|\\n)\\s*(原创性声明|原创声明|独创性声明|版权声明|授权声明|使用授权书|承诺书|承诺声明|诚信声明|导师声明|指导教师声明|答辩委员会决议)\\s*[：:]?\\s*\\n(?:[\\s\\S]*?)(?=\\n\\d+\\s*[第]?[章节]|\\n[一二三四五六七八九十]+[章节]|\\n[1-9]\\.|\\Z)",
            Pattern.CASE_INSENSITIVE
    );

    // 封面部分模式
    private static final Pattern COVER_PATTERN = Pattern.compile(
            "(?:^|\\n)\\s*(封面|title\\s*page|学位论文)\\s*[：:]?\\s*\\n(?:[\\s\\S]*?)(?=\\n\\d+\\s*[第]?[章节]|\\n[一二三四五六七八九十]+[章节]|\\n[1-9]\\.|\\n摘要|\\Z)",
            Pattern.CASE_INSENSITIVE
    );

    // 常见章节标题前缀
    private static final String[] CHAPTER_PREFIXES = {
            "摘要", "abstract", "引言", "introduction",
            "第一章", "第二章", "第三章", "第四章", "第五章", "第六章", "第七章", "第八章", "第九章", "第十章",
            "第1章", "第2章", "第3章", "第4章", "第5章", "第6章", "第7章", "第8章", "第9章", "第10章",
            "1.", "2.", "3.", "4.", "5.", "6.", "7.", "8.", "9.", "10.",
            "1.1", "1.2", "1.3", "2.1", "2.2", "2.3", "3.1", "3.2", "3.3",
            "一、", "二、", "三、", "四、", "五、", "六、", "七、", "八、", "九、", "十、",
            "参考文献", "references", "reference", "附录", "appendix",
            "结论", "conclusion", "总结", "summary",
            // 新增：致谢、声明、封面等
            "致谢", "acknowledgement", "acknowledgments",
            "原创性声明", "原创声明", "独创性声明", "学位论文原创性声明",
            "版权声明", "授权声明", "使用授权书",
            "封面", "title page", "title", "学位论文",
            "承诺书", "承诺声明", "诚信声明",
            "导师声明", "指导教师声明", "答辩委员会决议"
    };

    /**
     * 完整文本预处理流程
     * @param content 原始文本
     * @param options 预处理选项
     * @return 处理后的文本
     */
    public String preprocess(String content, PreprocessOptions options) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        String result = content;

        // 1. 去除封面
        if (options.isRemoveCover()) {
            result = removeCover(result);
        }

        // 2. 去除声明（原创性声明、版权声明等）
        if (options.isRemoveDeclarations()) {
            result = removeDeclarations(result);
        }

        // 3. 去除目录
        if (options.isRemoveTableOfContents()) {
            result = removeTableOfContents(result);
        }

        // 4. 去除章节标题
        if (options.isRemoveChapterTitles()) {
            result = removeChapterTitles(result);
        }

        // 5. 去除致谢
        if (options.isRemoveAcknowledgements()) {
            result = removeAcknowledgements(result);
        }

        // 6. 去除页码
        if (options.isRemovePageNumbers()) {
            result = removePageNumbers(result);
        }

        // 7. 去除多余空白
        result = normalizeWhitespace(result);

        log.debug("文本预处理完成: 原长度={}, 处理后长度={}", content.length(), result.length());
        return result;
    }

    /**
     * 去除目录部分
     */
    public String removeTableOfContents(String content) {
        Matcher matcher = TABLE_OF_CONTENTS_PATTERN.matcher(content);
        return matcher.replaceAll("");
    }

    /**
     * 去除章节标题
     */
    public String removeChapterTitles(String content) {
        // 使用正则匹配章节标题
        Matcher matcher = CHAPTER_TITLE_PATTERN.matcher(content);
        String result = matcher.replaceAll("");

        // 额外处理常见章节标题前缀
        for (String prefix : CHAPTER_PREFIXES) {
            Pattern prefixPattern = Pattern.compile("(?:^|\\n)\\s*" + Pattern.quote(prefix) + "\\s+[^\\n]{0,30}(?=\\n|$)", Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
            result = prefixPattern.matcher(result).replaceAll("");
        }

        return result;
    }

    /**
     * 去除页码
     */
    public String removePageNumbers(String content) {
        Matcher matcher = PAGE_NUMBER_PATTERN.matcher(content);
        return matcher.replaceAll("\n");
    }

    /**
     * 去除封面部分
     */
    public String removeCover(String content) {
        Matcher matcher = COVER_PATTERN.matcher(content);
        return matcher.replaceAll("");
    }

    /**
     * 去除声明部分（原创性声明、版权声明等）
     */
    public String removeDeclarations(String content) {
        Matcher matcher = DECLARATION_PATTERN.matcher(content);
        return matcher.replaceAll("");
    }

    /**
     * 去除致谢部分
     */
    public String removeAcknowledgements(String content) {
        Matcher matcher = ACKNOWLEDGEMENT_PATTERN.matcher(content);
        return matcher.replaceAll("");
    }

    /**
     * 规范化空白字符
     */
    public String normalizeWhitespace(String content) {
        // 去除多余的换行和空格
        return content.replaceAll("\\n+", "\n")
                      .replaceAll("\\s+", " ")
                      .trim();
    }

    /**
     * 获取文本中的章节列表
     */
    public List<String> extractChapters(String content) {
        List<String> chapters = new ArrayList<>();
        Matcher matcher = CHAPTER_TITLE_PATTERN.matcher(content);
        while (matcher.find()) {
            chapters.add(matcher.group().trim());
        }
        return chapters;
    }

    /**
     * 预处理选项
     */
    public static class PreprocessOptions {
        private boolean removeCover = true;
        private boolean removeDeclarations = true;
        private boolean removeTableOfContents = true;
        private boolean removeChapterTitles = true;
        private boolean removeAcknowledgements = true;
        private boolean removePageNumbers = true;

        public PreprocessOptions() {}

        public PreprocessOptions removeCover(boolean value) {
            this.removeCover = value;
            return this;
        }

        public PreprocessOptions removeDeclarations(boolean value) {
            this.removeDeclarations = value;
            return this;
        }

        public PreprocessOptions removeTableOfContents(boolean value) {
            this.removeTableOfContents = value;
            return this;
        }

        public PreprocessOptions removeChapterTitles(boolean value) {
            this.removeChapterTitles = value;
            return this;
        }

        public PreprocessOptions removeAcknowledgements(boolean value) {
            this.removeAcknowledgements = value;
            return this;
        }

        public PreprocessOptions removePageNumbers(boolean value) {
            this.removePageNumbers = value;
            return this;
        }

        public boolean isRemoveCover() { return removeCover; }
        public boolean isRemoveDeclarations() { return removeDeclarations; }
        public boolean isRemoveTableOfContents() { return removeTableOfContents; }
        public boolean isRemoveChapterTitles() { return removeChapterTitles; }
        public boolean isRemoveAcknowledgements() { return removeAcknowledgements; }
        public boolean isRemovePageNumbers() { return removePageNumbers; }
    }
}