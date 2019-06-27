package com.zhongweixian.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
    public static String getChatRoomTextMessageContent(String content) {
        if (content == null) {
            throw new IllegalArgumentException("content");
        }
        return content.replaceAll("^(@([0-9]|[a-z])+):", "")
                .replaceAll("<br/>", "\r\n");
    }

    public static String getSenderOfChatRoomTextMessage(String content) {
        if (content == null) {
            throw new IllegalArgumentException("content");
        }
        Pattern pattern = Pattern.compile("^(@([0-9]|[a-z])+):");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static boolean checkLan(String content) {
        if (content.startsWith("用户")) {
            return true;
        }
        boolean chinese = false;
        boolean english = false;
        for (char c : content.toCharArray()) {
            if (chinese == false && (c >= 0x4E00 && c <= 0x9FA5)) {
                chinese = true;
            }
            if (english == false && (c + "").matches("[a-zA-Z0-9]+")) {
                english = true;
            }
        }
        return english && chinese;
    }

    public static void main(String[] args) {
        String content = "你好啊1";
        System.out.println(checkLan(content));
    }
}
