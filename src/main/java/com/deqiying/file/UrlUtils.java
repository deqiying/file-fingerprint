package com.deqiying.file;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtils {
    /**
     * 判断字符串是否是一个有效的URL
     *
     * @param url 要判断的字符串
     * @return 如果是有效URL则返回true，否则返回false
     */
    public static boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * 尝试打开一个URL链接，如果响应类型是文件或流则返回InputStream，否则返回null
     *
     * @param url 要打开的URL
     * @return InputStream 如果是文件或流；否则返回null
     * @throws Exception 如果URL无效或请求失败
     */
    public static InputStream openUrl(String url) throws Exception {
        // 打开连接
        HttpURLConnection connection = openUrlConnection(url);

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return connection.getInputStream();
        } else {
            throw new Exception("无法打开URL,响应码: " + responseCode);
        }
    }

    /**
     * 尝试下载一个URL链接
     *
     * @param url 要打开的URL
     * @return byte[] 下载好的字节流
     * @throws Exception 如果URL无效或请求失败
     */
    public static byte[] downloadUrl(String url) throws Exception {
        try (InputStream inputStream = openUrl(url);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // 缓冲区
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }

    /**
     * 尝试打开一个URL链接，返回HttpURLConnection对象
     *
     * @param url 要打开的URL
     * @return HttpURLConnection 如果是文件或流；否则返回null
     * @throws Exception 如果URL无效或请求失败
     */
    public static HttpURLConnection openUrlConnection(String url) throws Exception {
        if (!isValidUrl(url)) {
            throw new MalformedURLException("无效的URL: " + url);
        }
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10 * 60 * 1000); // 设置连接超时时间
        // 设置读取超时时间--不限时间
        connection.setReadTimeout(0);
        return connection;
    }

}