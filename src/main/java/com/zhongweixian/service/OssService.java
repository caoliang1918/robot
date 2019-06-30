package com.zhongweixian.service;

import com.amazonaws.services.s3.model.PutObjectResult;

import java.io.InputStream;

/**
 * Created by caoliang on 2019-06-29
 */
public interface OssService {


    /**
     * 京东云文件上传
     *
     * @param inputStream
     * @param length
     * @param contentType
     * @param bucket
     * @param fileName
     * @return
     */
    PutObjectResult uploadJdcloud(InputStream inputStream, int length, String contentType, String bucket, String fileName);
}
