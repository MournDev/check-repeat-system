package com.abin.checkrepeatsystem.common.service;

public interface PreviewTokenService {

    String generatePreviewToken(Long fileId);

    Long validatePreviewToken(String token);
}
