package com.zhongweixian.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.zhongweixian.service.OssService;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Created by caoliang on 2019-06-29
 */

@Service
public class OssServiceImpl implements OssService {
    private Logger logger = LoggerFactory.getLogger(OssServiceImpl.class);

    @Autowired
    private AmazonS3 amazonS3;

    @Override
    public PutObjectResult uploadJdcloud(InputStream inputStream, int length, String contentType, String bucket, String fileName) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(contentType);
        objectMetadata.setContentLength(length);
        PutObjectResult result = amazonS3.putObject(bucket, fileName, inputStream, objectMetadata);
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("uploadJdcloud is success:{}" , result.getETag());
        return result;
    }
}
