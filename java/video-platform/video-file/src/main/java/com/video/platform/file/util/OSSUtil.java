package com.video.platform.file.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Component
public class OSSUtil {

    @Value("${aliyun.oss.endpoint:}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name:}")
    private String bucketName;

    @Value("${aliyun.oss.url-prefix:}")
    private String urlPrefix;

    @Value("${aliyun.oss.enabled:false}")
    private boolean enabled;

    private com.aliyun.oss.OSS ossClient;

    private void initOSSClient() {
        if (ossClient == null && enabled) {
            ossClient = new com.aliyun.oss.OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        }
    }

    public String uploadFile(MultipartFile file, String dir) {
        if (!enabled) {
            return null;
        }

        initOSSClient();

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = dir + "/" + UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            ossClient.putObject(bucketName, fileName, inputStream);
            return urlPrefix + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String uploadVideo(MultipartFile file) {
        return uploadFile(file, "videos");
    }

    public String uploadCover(MultipartFile file) {
        return uploadFile(file, "covers");
    }

    public boolean deleteFile(String fileName) {
        if (!enabled) {
            return false;
        }

        initOSSClient();

        try {
            String objectName = fileName.replace(urlPrefix, "");
            ossClient.deleteObject(bucketName, objectName);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
            ossClient = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }
}
