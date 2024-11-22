package com.deqiying.file;

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
        if (!isValidUrl(url)) {
            throw new MalformedURLException("无效的URL: " + url);
        }

        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000); // 设置连接超时时间
        connection.setReadTimeout(5000);    // 设置读取超时时间

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            // 获取响应的 Content-Type
            String contentType = connection.getContentType();
            if (isDownloadable(contentType)) {
                return connection.getInputStream();
            } else {
                System.out.println("URL 响应类型不是文件或流: " + contentType);
                return null;
            }
        } else {
            throw new Exception("无法打开URL，响应码: " + responseCode);
        }
    }

    /**
     * 判断URL是否返回可下载的内容
     *
     * @param url 要判断的URL
     * @return 如果支持下载返回true，否则返回false
     * @throws Exception 如果请求失败
     */
    public static boolean isDownloadable(String url) throws Exception {
        URL urlObj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("HEAD"); // 使用HEAD请求避免下载整个内容
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            return false; // 如果响应不是200 OK，则不支持下载
        }

        // 检查 Content-Disposition 是否包含附件
        String contentDisposition = connection.getHeaderField("Content-Disposition");
        if (contentDisposition != null && contentDisposition.toLowerCase().contains("attachment")) {
            return true;
        }

        // 检查 Content-Type 是否为支持下载的类型
        String contentType = connection.getContentType();
        return isDownloadableContentType(contentType);
    }

    /**
     * 判断Content-Type是否支持下载
     *
     * @param contentType 响应的Content-Type
     * @return 如果支持下载返回true，否则返回false
     */
    private static boolean isDownloadableContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        // 常见文件和流类型
        return contentType.startsWith("application/") || // 二进制文件（如PDF、ZIP、EXE）
                contentType.startsWith("image/") ||       // 图片文件
                contentType.startsWith("video/") ||       // 视频文件
                contentType.startsWith("audio/") ||       // 音频文件
                contentType.startsWith("text/csv") ||     // CSV 文件
                contentType.equals("application/octet-stream"); // 通用二进制流
    }


}