package com.zhongweixian.service.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.zhongweixian.service.OssService;
import com.zhongweixian.web.entity.BotVideo;
import com.zhongweixian.web.entity.page.Page;
import com.zhongweixian.web.service.BotVideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by caoliang on 2019-06-29
 */

@Service
public class OssServiceImpl implements OssService {
    private Logger logger = LoggerFactory.getLogger(OssServiceImpl.class);


    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private BotVideoService botVideoService;


    @Override
    public PutObjectResult uploadJdcloud(InputStream inputStream, int length, String contentType, String bucket, String fileName) {
        Long start = System.currentTimeMillis();
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(contentType);
        objectMetadata.setContentLength(length);
        PutObjectResult result = amazonS3.putObject(bucket, fileName, inputStream, objectMetadata);
        try {
            inputStream.close();
        } catch (IOException e) {
            logger.error("{}" , e);
        }
        Long end = System.currentTimeMillis();
        logger.info("uploadJdcloud is success:{} , time:{}", result.getETag(), end - start);
        return result;
    }

    @Override
    public void getMediaFile(String bucket, String fileName) {
        try {
            S3Object o = amazonS3.getObject(bucket, fileName);
            S3ObjectInputStream s3is = o.getObjectContent();
            FileOutputStream fos = new FileOutputStream(new File(fileName));
            byte[] read_buf = new byte[1024];
            int read_len = 0;
            while ((read_len = s3is.read(read_buf)) > 0) {
                fos.write(read_buf, 0, read_len);
            }
            s3is.close();
            fos.close();
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

    /**
     * 获取所有的bucket
     *
     * @return
     */

    private void getAllMedia() {
        Integer start = 0;
        Map<String , Object> params = new HashMap<>();
        params.put("pageNum" , start);
        params.put("limit" , 1000);
        params.put("status" , 2);

        Page<BotVideo> page = page = botVideoService.findByPageParams(params);
        downList(page.getList());

        while (page!= null && !CollectionUtils.isEmpty(page.getList())){
            start++;
            params.put("pageNum" , start);
            page = botVideoService.findByPageParams(params);
            downList(page.getList());
        }
    }


    private void downList(List<BotVideo> list){
        if (CollectionUtils.isEmpty(list)){
            return;
        }
        String bucket = null;
        String key = null;
        for (BotVideo botVideo : list){
            bucket = botVideo.getVideoUrl().substring(47 , 77);
            key =  botVideo.getVideoUrl().substring(78 , botVideo.getVideoUrl().length());
            logger.info("bucket:{} key:{} , botVideo:{}" ,bucket , key ,  botVideo.getVideoUrl());
            getMediaFile(bucket , key);
        }
    }

}
