package com.zhongweixian.wechat.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.zhongweixian.wechat.domain.HttpMessage;
import com.zhongweixian.wechat.domain.request.RevokeRequst;
import com.zhongweixian.wechat.domain.response.SendMsgResponse;
import com.zhongweixian.wechat.service.LoginService;
import com.zhongweixian.wechat.service.WechatHttpService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

/**
 * Created by caoliang on 2019/1/11
 */
@RestController
@RequestMapping("index")
public class MessageController {
    private Logger logger = LoggerFactory.getLogger(MessageController.class);

    @Autowired
    private WechatHttpService wechatHttpService;

    Map<Long, List<RevokeRequst>> messageMap = new HashMap<>();


    private Set<String> toUsers = new HashSet<>();


    @PostMapping("sendMessage")
    public String send(@RequestBody HttpMessage httpMessage) {
        try {
            logger.info("send message : {}", httpMessage);
            for (String user : toUsers) {
                if ("create".equals(httpMessage.getOption())) {
                    SendMsgResponse response = wechatHttpService.sendText(user, httpMessage.getContent());
                    //保存消息
                    putMessage(httpMessage.getId(), new RevokeRequst(user, response.getMsgID()));
                } else {
                    RevokeRequst revokeRequst = getRevoke(httpMessage.getId());
                    if (revokeRequst != null) {
                        wechatHttpService.revoke(revokeRequst.getClientMsgId(), user);
                    }
                    wechatHttpService.sendText(user, httpMessage.getContent());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "send success";
    }

    @GetMapping("addToUser")
    public String addGroup(String groupId) {
        toUsers.add(groupId);
        return "is ok";
    }

    @DeleteMapping("deleteGroup")
    public String deleteGroup(String groupId) {
        toUsers.remove(groupId);
        return "is ok";
    }

    private void putMessage(Long httpMessageId, RevokeRequst revokeRequst) {
        List<RevokeRequst> revokeRequsts = messageMap.get(httpMessageId);
        if (CollectionUtils.isEmpty(revokeRequsts)) {
            revokeRequsts = new ArrayList<>();
        }
        revokeRequsts.add(revokeRequst);
    }

    private RevokeRequst getRevoke(Long httpMessageId) {
        List<RevokeRequst> revokeRequsts = messageMap.get(httpMessageId);
        if (CollectionUtils.isEmpty(revokeRequsts)) {
            return null;
        }
        RevokeRequst revokeRequst = revokeRequsts.get(0);
        revokeRequsts.remove(revokeRequst);
        return revokeRequst;
    }


}
