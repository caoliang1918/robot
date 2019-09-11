package com.zhongweixian.web;

import com.alibaba.fastjson.JSONObject;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.zhongweixian.cache.CacheService;
import com.zhongweixian.domain.weibo.WeiBoUser;
import com.zhongweixian.login.WbSyncMessage;
import com.zhongweixian.login.WxIMThread;
import com.zhongweixian.service.*;
import com.zhongweixian.web.entity.BotVideo;
import com.zhongweixian.web.entity.page.Page;
import com.zhongweixian.web.service.BotVideoService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
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
    private WbBlockUser wbBlockUser;

    @Autowired
    private WbSyncMessage wbSyncMessage;

    @Autowired
    private WeiBoHttpService weiBoHttpService;

    @Autowired
    private BotVideoService botVideoService;

    @Autowired
    private OssService ossService;


    @GetMapping("login")
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


    @GetMapping
    public HttpEntity addBlackUser(@RequestParam Long userId, @RequestParam(defaultValue = "1") Integer pageNum) {
        int defaultPage = pageNum;
        List<WeiBoUser> weiBoUserList = new ArrayList<>();
        List<WeiBoUser> pageList = null;
        wbBlockUser.addBlackUser(userId);
        while (true) {
            pageList = wbBlockUser.fans(userId, pageNum);
            if (CollectionUtils.isEmpty(pageList)) {
                break;
            }
            weiBoUserList.addAll(pageList);
            pageNum++;
        }
        pageNum = defaultPage;
        while (true) {
            pageList = wbBlockUser.follow(userId, pageNum);
            if (CollectionUtils.isEmpty(pageList)) {
                break;
            }
            weiBoUserList.addAll(pageList);
            pageNum++;
        }
        for (WeiBoUser weiBoUser : weiBoUserList) {
            wbBlockUser.addBlackUser(weiBoUser.getId());
        }
        return new HttpEntity<>(HttpStatus.OK);
    }


    @PostMapping("setCookie")
    public String setCookie(String cookie) {
        this.cookie = cookie;
        wbSyncMessage.setCookie(cookie);
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

}