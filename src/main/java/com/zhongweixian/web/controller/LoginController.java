package com.zhongweixian.web.controller;

import com.alibaba.fastjson.JSONObject;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.zhongweixian.cache.CacheService;
import com.zhongweixian.login.WbSyncMessage;
import com.zhongweixian.login.WxIMThread;
import com.zhongweixian.service.*;
import com.zhongweixian.web.CommonResponse;
import com.zhongweixian.web.entity.BotVideo;
import com.zhongweixian.web.entity.page.Page;
import com.zhongweixian.web.service.BotVideoService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;

/**
 * Created by caoliang on 2019/1/15
 */

@RestController
@RequestMapping("index")
public class LoginController {
    private Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private ExecutorService wxExecutor;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private WxHttpService wxHttpService;
    @Autowired
    private WxMessageHandler wxMessageHandler;

    @Autowired
    private WbService wbService;

    @Autowired
    private WeiBoHttpService weiBoHttpService;

    @Autowired
    private BotVideoService botVideoService;

    @Autowired
    private OssService ossService;

    /**
     * 登录页面
     *
     * @return
     */
    @GetMapping
    public ModelAndView loginPage() {
        return new ModelAndView("/welcome");
    }


    @PostMapping("login")
    public CommonResponse login(@RequestParam String username, @RequestParam String password) {
        logger.info("username:{} login success!", username);
        return new CommonResponse();
    }

    @GetMapping("qrcode")
    public void qrcode(HttpServletResponse response) throws IOException, WriterException {
        WxIMThread wxIMThread = new WxIMThread(cacheService, wxHttpService, wxMessageHandler);
        wxExecutor.execute(wxIMThread);
        String qrUrl = wxIMThread.showQrcode();
        if (qrUrl == null) {
            return;
        }
        String dataHandle = new String(qrUrl.getBytes("UTF-8"), "UTF-8");
        BitMatrix bitMatrix = new MultiFormatWriter().encode(dataHandle, BarcodeFormat.QR_CODE, 270, 270);
        OutputStream os = response.getOutputStream();
        //写入文件刷新
        MatrixToImageWriter.writeToStream(bitMatrix, "png", os);

        //关闭流
        os.flush();
        os.close();
    }


    @PostMapping("setCookie")
    public String setCookie(String cookie) {
        this.cookie = cookie;
        wbService.setCookie(cookie);
        return "is ok";
    }

    private String cookie;

    @PostMapping
    public String uploadVideo() {
        if (cookie == null) {
            return "cookie is null";
        }
        Map<String, Object> param = new HashMap<>();
        param.put("status", 1);
        param.put("pageNum", 0);
        param.put("limit", 200);
        Page<BotVideo> page = botVideoService.findByPageParams(param);
        for (BotVideo botVideo : page.getList()) {
            if (botVideo.getStatus() == 2) {
                continue;
            }
            download(botVideo);
        }
        return "is ok";
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
            logger.error("{}", e.getMessage());
            if (!"403 Forbidden".equals(e.getMessage())) {
                botVideoService.deleteById(botVideo.getId());
            }
        }
        if (responseEntity == null || responseEntity.getBody() == null) {
            return;
        }
        byte[] bytes = responseEntity.getBody();


        /**
         * 上传云服务器
         */
        InputStream inputStream = new ByteArrayInputStream(bytes);
        String bucket = "weibo-video/" + DateUtils.formatDate(new Date(), "yyyy-MM") + "/" + DateUtils.formatDate(new Date(), "yyyy-MM-dd");
        String hashCode = DigestUtils.md5Hex(bytes);
        Integer size = bytes.length;
        String videoId = System.currentTimeMillis() + ".mp4";

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

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://weibo.com/aj/mblog/add?ajwvr=6&__rnd=1582875725705";
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Type", "application/x-www-form-urlencoded");
        httpHeaders.add("Referer", "https://weibo.com/u/7400703364/home");
        httpHeaders.add("Cookie", "lang=zh-tw; Ugrow-G0=589da022062e21d675f389ce54f2eae7; login_sid_t=3185877c09f171149df56060772bf0c1; cross_origin_proto=SSL; TC-V5-G0=4de7df00d4dc12eb0897c97413797808; _ga=GA1.2.1744706232.1582765860; Hm_lvt_50f34491c9065a59f87d0d334df29fa4=1582765861; Hm_lpvt_50f34491c9065a59f87d0d334df29fa4=1582765861; _s_tentry=passport.weibo.com; Apache=9804979138133.703.1582765863164; SINAGLOBAL=9804979138133.703.1582765863164; ULV=1582765863169:1:1:1:9804979138133.703.1582765863164:; appkey=; WB_register_version=307744aa77dd5677; WBtopGlobal_register_version=307744aa77dd5677; wb_view_log_7400703364=1680*10502; YF-V5-G0=f0aacce81fff76e1515ae68ac76a20c3; UOR=,,login.sina.com.cn; wb_view_log=1680*10502; un=caoliang1918@aliyun.com; wvr=6; wb_timefeed_7400703364=1; WBStorage=42212210b087ca50|undefined; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9WWb_MqU--sF-w5l_zJLwA4A5JpX5K2hUgL.FoMXeh5Nehe0SoB2dJLoI0qLxKMLB.-L12-LxKnL1hzLBK2LxK-LB--L1--LxKqL1KMLBoqLxKnLBK2L12eLxKqL1heL1h-t; SUHB=0TPFPTekEioUf5; ALF=1614412742; SSOLoginState=1582876742; SCF=AmhWwo7yqg3L3XFnTLb2WHZXcffmS3SgdQaEIcW4KJMUZawqrK6bdS67Z-OBf5wbZDBzl_alFCSWyFiGYLVjTlM.; SUB=_2A25zXLgXDeRhGeFK61IW8C3PzTiIHXVQK67frDV8PUNbmtAKLRfbkW9NQ7xz1YWJeaMYhtSjO36qsrbDCe__sLJn; TC-Page-G0=51e9db4bd1cd84f5fb5f9b32772c2750|1582876746|1582876746; webim_unReadCount=%7B%22time%22%3A1582876749353%2C%22dm_pub_total%22%3A0%2C%22chat_group_client%22%3A0%2C%22allcountNum%22%3A0%2C%22msgbox%22%3A0%7D");

        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>("location=v6_content_home&text="+ RandomStringUtils.randomNumeric(32) +"&appkey=&style_type=1&pic_id=&tid=&pdetail=&mid=&isReEdit=false&rank=0&rankid=&module=stissue&pub_source=main_&pub_type=dialog&isPri=0&_t=0", httpHeaders), String.class);

        System.out.println(responseEntity.getBody());
    }


}