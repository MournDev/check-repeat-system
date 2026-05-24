package com.abin.checkrepeatsystem.common.utils;

import com.abin.checkrepeatsystem.common.exception.BusinessException;
import com.abin.checkrepeatsystem.common.enums.ResultCode;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.JiebaSegmenter.SegMode;
import com.huaban.analysis.jieba.SegToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class IKAnalyzerUtils {

    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    private static final Set<String> STOP_WORDS = Set.of(
            "的", "在", "和", "是", "我",
            "你", "他", "这", "那", "与",
            "及", "等", "了", "着", "过"
    );

    public static List<String> segment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        try {
            List<SegToken> tokens = SEGMENTER.process(text.trim(), SegMode.SEARCH);
            for (SegToken token : tokens) {
                String word = token.word;
                if (!STOP_WORDS.contains(word) && word.length() > 1) {
                    result.add(word);
                }
            }
        } catch (Exception e) {
            log.error("中文分词失败: ", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "文本预处理失败,分词异常");
        }
        return result;
    }

    public static String segmentToString(String text) {
        List<String> words = segment(text);
        return String.join(" ", words);
    }
}
