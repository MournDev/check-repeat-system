package com.abin.checkrepeatsystem.common.component;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@ConfigurationProperties(prefix = "minio")
@Component
public class MinioProp {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private Bucket bucket = new Bucket();

    @Data
    public static class Bucket {
        private String main = "check-repeat-system";
        private String avatar = "avatar";
        private String report = "report-bucket";
        private String paperContent = "paper-content";
        private String file = "file";
    }
}
