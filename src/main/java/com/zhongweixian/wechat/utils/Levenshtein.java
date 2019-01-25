package com.zhongweixian.wechat.utils;

/**
 * Created by caoliang on 2019/1/25
 */
public class Levenshtein {

    private int compare(String str, String target) {
        /**
         * 矩阵
         */
        int d[][];
        int n = str.length();
        int m = target.length();
        int i;
        int j;
        char ch1;
        char ch2;
        /**
         *  记录相同字符,在某个矩阵位置值的增量,不是0就是1
         */
        int temp;

        if (n == 0) {
            return m;
        }

        if (m == 0) {
            return n;
        }

        d = new int[n + 1][m + 1];

        // 初始化第一列
        for (i = 0; i <= n; i++) {
            d[i][0] = i;
        }

        // 初始化第一行
        for (j = 0; j <= m; j++) {
            d[0][j] = j;
        }

        // 遍历str
        for (i = 1; i <= n; i++) {
            ch1 = str.charAt(i - 1);
            // 去匹配target
            for (j = 1; j <= m; j++) {
                ch2 = target.charAt(j - 1);
                if (ch1 == ch2) {
                    temp = 0;
                } else {
                    temp = 1;
                }

                // 左边+1,上边+1, 左上角+temp取最小
                d[i][j] = min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + temp);
            }
        }

        return d[n][m];
    }

    private int min(int one, int two, int three) {
        return (one = one < two ? one : two) < three ? one : three;
    }

    /**
     * 获取两字符串的相似度
     *
     * @param str
     * @param target
     * @return
     */

    public float getSimilarityRatio(String str, String target) {
        return 1 - (float) compare(str, target) / Math.max(str.length(), target.length());

    }

    public static void main(String[] args) {
        Levenshtein lt = new Levenshtein();
        String str = "中国央行：截至2018年末，全国共有小额贷款公司8133家。贷款余额9550亿元，全年减少190亿元。";
        String target = "中国央行：2018年末个人住房贷款余额25.75万亿元，同比增长17.8%。";
        System.out.println("similarityRatio=" + lt.getSimilarityRatio(str, target));
    }

}
