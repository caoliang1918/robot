package com.zhongweixian.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@ControllerAdvice
public class ExceptionHandlerUtil {
    private Logger logger = LoggerFactory.getLogger(ExceptionHandlerUtil.class);

    @ExceptionHandler(RobotException.class)
    public void handleSystem(RobotException ex) {
        logger.error("RobotException:{}", ex);
    }

    @ExceptionHandler(WechatQRExpiredException.class)
    public void qrExpiredException(MissingServletRequestParameterException ex) {
        logger.error("WechatQRExpiredException:{}", ex);
    }

}
