package com.zhongweixian.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.zhongweixian.domain.weibo.WeiBoUser;
import com.zhongweixian.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by caoliang on 2019/1/15
 */

@RestController
@RequestMapping("index")
public class LoginController {

    @Autowired
    private ExecutorService executorService;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private WechatHttpService wechatHttpService;
    @Autowired
    private WechatMessageService wechatMessageService;

    @Autowired
    private WeiBoService weiBoService;


    @GetMapping("login")
    public void qrcode(HttpServletResponse response) throws IOException, WriterException {
        LoginThread loginThread = new LoginThread(cacheService, wechatHttpService, wechatMessageService);
        executorService.execute(loginThread);
        String qrUrl = loginThread.showQrcode();
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
        while (true) {
            pageList = weiBoService.fans(userId, pageNum);
            if (CollectionUtils.isEmpty(pageList)) {
                break;
            }
            weiBoUserList.addAll(pageList);
            pageNum++;
        }
        pageNum = defaultPage;
        while (true) {
            pageList = weiBoService.follow(userId, pageNum);
            if (CollectionUtils.isEmpty(pageList)) {
                break;
            }
            weiBoUserList.addAll(pageList);
            pageNum++;
        }
        for (WeiBoUser weiBoUser : weiBoUserList) {
            weiBoService.addBlackUser(weiBoUser.getId());
        }
        return new HttpEntity<>(HttpStatus.OK);
    }


}
