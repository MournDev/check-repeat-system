package com.abin.checkrepeatsystem.common.utils;

import java.util.Map;

public final class FileMimeTypeUtils {

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
        Map.entry("pdf", "application/pdf"),
        Map.entry("doc", "application/msword"),
        Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        Map.entry("xls", "application/vnd.ms-excel"),
        Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        Map.entry("ppt", "application/vnd.ms-powerpoint"),
        Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
        Map.entry("html", "text/html"),
        Map.entry("htm", "text/html"),
        Map.entry("txt", "text/plain"),
        Map.entry("csv", "text/csv"),
        Map.entry("xml", "application/xml"),
        Map.entry("json", "application/json"),
        Map.entry("zip", "application/zip"),
        Map.entry("rar", "application/x-rar-compressed"),
        Map.entry("jpg", "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("png", "image/png"),
        Map.entry("gif", "image/gif"),
        Map.entry("bmp", "image/bmp"),
        Map.entry("svg", "image/svg+xml"),
        Map.entry("webp", "image/webp")
    );

    private FileMimeTypeUtils() {}

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    public static String getContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String extension = getFileExtension(fileName).toLowerCase();
        return MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    }
}
