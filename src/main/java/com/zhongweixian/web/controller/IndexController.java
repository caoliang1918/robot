package com.zhongweixian.web.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.zhongweixian.cache.CacheService;
import com.zhongweixian.web.CommonResponse;
import com.zhongweixian.wechat.login.WxIMThread;
import com.zhongweixian.wechat.service.WxHttpService;
import com.zhongweixian.wechat.service.WxMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

/**
 * Created by caoliang on 2019/1/15
 */

@RestController
@RequestMapping("index")
public class IndexController {
    private Logger logger = LoggerFactory.getLogger(IndexController.class);

    @Autowired
    private ExecutorService wxExecutor;
    @Autowired
    private CacheService cacheService;
    @Autowired
    private WxHttpService wxHttpService;
    @Autowired
    private WxMessageHandler wxMessageHandler;

    /**
     * 登录页面
     *
     * @return
     */
    @GetMapping
    public ModelAndView loginPage() {
        return new ModelAndView("/index");
    }


    @PostMapping("login")
    public CommonResponse login(@RequestParam String username, @RequestParam String password) {
        logger.info("username:{} login success!", username);
        return new CommonResponse();
    }

    @PostMapping("logout")
    public CommonResponse logout(@RequestParam String username, @RequestParam String password) {
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


}