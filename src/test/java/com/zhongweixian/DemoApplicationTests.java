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

    String cookie = "JSESSIONID=2389A3740D82E4F72190EF2E1C9F1AAC; _s_tentry=login.sina.com.cn; Apache=351887198397.7934.1561655226197; SINAGLOBAL=351887198397.7934.1561655226197; ULV=1561655226225:1:1:1:351887198397.7934.1561655226197:; login_sid_t=dacbff5a360e932f01cd451ea2971302; cross_origin_proto=SSL; UOR=,,login.sina.com.cn; SSOLoginState=1561738330; wvr=6; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9Whu6d1rS1RzpY.o0E_MyWMw5JpX5KMhUgL.FoMp1KBRShq0e0.2dJLoIEXLxK-L12qL12BLxKML1hnLB-eLxKqL1-eLB.2LxK-L1K.LBKnLxKBLB.2LB.2t; ALF=1593430955; SCF=AtM3j7K_N0SUdpWaCxyv2VOsDtKqhtFgga8b8MLk0dfXop2hsydnwudsYVy0QbZ57SD9PiGxxLX4O_dkO_WkkQI.; SUB=_2A25wHNB-DeRhGeFP4lYZ9CjPyDWIHXVTaEa2rDV8PUNbmtAKLWGmkW9NQO_ryl5YixtLBa17V18ZCqu9OfFdD_wF; SUHB=02M_lx1mrNenoD; webim_unReadCount=%7B%22time%22%3A1561895811788%2C%22dm_pub_total%22%3A0%2C%22chat_group_pc%22%3A0%2C%22allcountNum%22%3A0%2C%22msgbox%22%3A0%7D";
    @Test
    public void uploadVideo() {
        Map<String , Object> param = new HashMap<>();
        param.put("status" ,1);
        param.put("pageNum" ,0);
        param.put("limit" ,100);
        Page<BotVideo> page = botVideoService.findByPageParams(param);

        for(BotVideo botVideo : page.getList()){
            logger.info("botVideo{}" , JSONObject.toJSONString(botVideo));
            ResponseEntity<byte[]> responseEntity = weiBoHttpService.download(botVideo.getFromUrl() , cookie);

            if(responseEntity.getBody() == null){
             continue;
            }
            byte[] bytes = responseEntity.getBody();
            /**
             * 上传云服务器
             */
            InputStream inputStream = new ByteArrayInputStream(bytes);
            String bucket = "wb-video/" + DateUtils.formatDate(new Date(), "yyyy-MM");
            String hashCode = DigestUtils.md5Hex(bytes);
            Integer size = bytes.length;
            String videoId = String.valueOf(System.currentTimeMillis());
            try {
                PutObjectResult result = ossService.uploadJdcloud(inputStream, bytes.length,"application/octet-stream", bucket,videoId);

                botVideo.setVideoCloud("jdcloud");
                botVideo.setVideoUrl("https://wb-video.s3.cn-south-1.jdcloud-oss.com/" + bucket + "/" + videoId);
                botVideo.setHashCode(hashCode);
                botVideo.setVideoSize(size);
                botVideo.setStatus(2);
                botVideoService.editById(botVideo);
            }catch (Exception e){
                logger.error("{}" , e);
            }


        }

    }

}
