package com.abin.checkrepeatsystem.common.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 文件类型枚举
 */
public enum FileType {
    IMAGE("image", Arrays.asList("jpg", "jpeg", "png", "gif", "svg", "webp", "bmp")),
    PDF("pdf", Arrays.asList("pdf")),
    TEXT("text", Arrays.asList("txt", "json", "xml", "csv", "md")),
    CODE("code", Arrays.asList("html", "css", "js", "ts", "java", "py")),
    AUDIO("audio", Arrays.asList("mp3", "ogg", "wav")),
    VIDEO("video", Arrays.asList("mp4", "webm", "ogg")),
    OFFICE("office", Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx")),
    OTHER("other", List.of());

    private final String type;
    private final List<String> extensions;

    FileType(String type, List<String> extensions) {
        this.type = type;
        this.extensions = extensions;
    }

    public static FileType fromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return OTHER;
        }
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        for (FileType fileType : values()) {
            if (fileType.extensions.contains(ext)) {
                return fileType;
            }
        }
        return OTHER;
    }

    public boolean isNativeSupported() {
        return this != OFFICE && this != OTHER;
    }

    public String getType() {
        return type;
    }
}