package com.zhongweixian;

import com.alibaba.fastjson.JSONObject;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.zhongweixian.service.OssService;
import com.zhongweixian.service.WeiBoHttpService;
import com.zhongweixian.web.entity.BotVideo;
import com.zhongweixian.web.entity.page.Page;
import com.zhongweixian.web.service.BotVideoService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.utils.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = WechatBootApplication.class)
@EnableAutoConfiguration
public class DemoApplicationTests {
    Logger logger = LoggerFactory.getLogger(DemoApplicationTests.class);

    @Autowired
    private WeiBoHttpService weiBoHttpService;

    @Autowired
    private BotVideoService botVideoService;

    @Autowired
    private OssService ossService;

    private ExecutorService task = Executors.newCachedThreadPool();


    String cookie = "UOR=login.sina.com.cn,weibo.com,login.sina.com.cn; SINAGLOBAL=9456802003620.64.1561629285945; wvr=6; SSOLoginState=1563845470; _s_tentry=login.sina.com.cn; Apache=5521194163294.984.1563845497220; ULV=1563845497227:15:13:2:5521194163294.984.1563845497220:1563758131879; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whu6d1rS1RzpY.o0E_MyWMw5JpX5KMhUgL.FoMp1KBRShq0e0.2dJLoIEXLxK-L12qL12BLxKML1hnLB-eLxKqL1-eLB.2LxK-L1K.LBKnLxKBLB.2LB.2t; ALF=1595473064; SCF=Aqqh9eM0QX_5TJHvIT1Bp_DVlqjNkTXae_JopKR-sMiuaEUEaRuVy-VeaxJM2Rp4s18HuPYN5JlVYR6NBQAMEug.; SUB=_2A25wM7l6DeRhGeFP4lYZ9CjPyDWIHXVTSK2yrDV8PUNbmtBeLWPikW9NQO_ryhDqV76oq88QYKZwHjmITytKxf5F; SUHB=0tK6CBPEpuIV6l; webim_unReadCount=%7B%22time%22%3A1563937089856%2C%22dm_pub_total%22%3A1%2C%22chat_group_client%22%3A806%2C%22allcountNum%22%3A808%2C%22msgbox%22%3A0%7D";

    @Test
    public void uploadVideo() {
        Map<String, Object> param = new HashMap<>();
        param.put("status", 1);
        param.put("pageNum", 0);
        param.put("limit", 1000);
        Page<BotVideo> page = botVideoService.findByPageParams(param);

        for (BotVideo botVideo : page.getList()) {
            download(botVideo);
        }


    }

    private void download(BotVideo botVideo) {
        logger.info("botVideo{}", JSONObject.toJSONString(botVideo));
        ResponseEntity<byte[]> responseEntity = null;
        try {
            Long start = System.currentTimeMillis();
            responseEntity = weiBoHttpService.download(botVideo.getFromUrl(), cookie);
            Long end = System.currentTimeMillis();
            logger.info("end-start={}", end - start);
        } catch (Exception e) {
            botVideoService.deleteById(botVideo.getId());
            logger.error("{}", e.getMessage());
        }
        if (responseEntity == null || responseEntity.getBody() == null) {
            return;
        }

        byte[] bytes = responseEntity.getBody();


        /**
         * 上传云服务器
         */
        InputStream inputStream = new ByteArrayInputStream(bytes);
        String bucket = "weibo-video/" + DateUtils.formatDate(new Date(), "yyyy-MM")+"/"+DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        String hashCode = DigestUtils.md5Hex(bytes);
        Integer size = bytes.length;
        String videoId = String.valueOf(System.currentTimeMillis());

        BotVideo exist = botVideoService.findByHashCode(size, hashCode);
        if (exist != null) {
            logger.warn("file is exist:{}", JSONObject.toJSONString(exist));
            botVideoService.deleteById(exist.getId());
            return;
        }

        try {
            PutObjectResult result = ossService.uploadJdcloud(inputStream, bytes.length, "application/octet-stream", bucket, videoId);
            botVideo.setVideoCloud("jdcloud");
            botVideo.setVideoUrl("https://wb-video.s3.cn-south-1.jdcloud-oss.com/" + bucket + "/" + videoId);
            botVideo.setHashCode(hashCode);
            botVideo.setVideoSize(size);
            botVideo.setStatus(2);
            botVideoService.editById(botVideo);
        } catch (Exception e) {
            logger.error("{}", e);
        }
    }

}
