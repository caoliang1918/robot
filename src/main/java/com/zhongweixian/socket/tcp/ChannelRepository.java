package com.zhongweixian.socket.tcp;

import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class ChannelRepository {
    private final static Map<String, NioSocketChannel> channelMap = new ConcurrentHashMap<String, NioSocketChannel>();

    public static void put(String station, NioSocketChannel nioSocketChannel) {
        channelMap.put(station, nioSocketChannel);
    }

    public static NioSocketChannel getChannel(String station) {
        return channelMap.get(station);
    }

    public static void remove(String station) {
        if (channelMap.containsKey(station)) {
            channelMap.remove(station);
        }
    }

    public static int size() {
        return channelMap.size();
    }

    public static void remove(NioSocketChannel nioSocketChannel) {
        if (nioSocketChannel == null) {
            return;
        }
        Set<Map.Entry<String, NioSocketChannel>> entryseSet = channelMap.entrySet();
        for (Map.Entry<String, NioSocketChannel> entry : entryseSet) {
            if (nioSocketChannel.equals(entry.getValue())) {
                entryseSet.remove(entry.getKey());
            }
        }
        nioSocketChannel.close().channel();
    }
}
