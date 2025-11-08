package com.deqiying.file;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public final class UrlUtils {
    // 默认配置常量
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000; // 10s 连接超时
    private static final int DEFAULT_READ_TIMEOUT_MS = 120_000;   // 120s 读取超时
    private static final int DEFAULT_MAX_REDIRECTS = 5;           // 最大重定向次数
    private static final String DEFAULT_USER_AGENT =
            "file-fingerprint/0.0.2 (+https://github.com/deqiying/file-fingerprint)";

    // 工具类不允许实例化
    private UrlUtils() { }

    /**
     * 判断字符串是否是一个有效的 HTTP/HTTPS URL
     *
     * @param url 要判断的字符串
     * @return 如果是有效HTTP/HTTPS URL则返回true，否则返回false
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            URL u = new URL(url);
            String protocol = u.getProtocol();
            return "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol);
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * 打开URL并返回输入流。关闭该流将自动断开底层连接，避免调用方忘记disconnect导致资源泄露。
     */
    public static InputStream openUrl(String url) throws Exception {
        HttpURLConnection connection = openUrlConnection(url);
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            return new ConnectionInputStream(connection.getInputStream(), connection);
        } else {
            String message = connection.getResponseMessage();
            connection.disconnect();
            throw new IOException("无法打开URL, 响应码: " + responseCode + (message != null ? (" " + message) : ""));
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
        return downloadInternal(url, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, DEFAULT_MAX_REDIRECTS);
    }

    /**
     * 带自定义超时与最大重定向的下载方法
     */
    public static byte[] downloadUrl(String url, int connectTimeoutMs, int readTimeoutMs, int maxRedirects) throws Exception {
        return downloadInternal(url, connectTimeoutMs, readTimeoutMs, maxRedirects);
    }

    /** 获取内容长度（如果服务端返回），未知时返回 -1 */
    public static long getContentLength(String url) throws Exception {
        HttpURLConnection conn = openUrlConnection(url);
        try {
            return conn.getContentLengthLong();
        } finally {
            conn.disconnect();
        }
    }

    /** 获取内容类型（Content-Type），未知时返回 null */
    public static String getContentType(String url) throws Exception {
        HttpURLConnection conn = openUrlConnection(url);
        try {
            return conn.getContentType();
        } finally {
            conn.disconnect();
        }
    }

    /** 内部统一的下载逻辑 */
    private static byte[] downloadInternal(String url, int connectTimeoutMs, int readTimeoutMs, int maxRedirects) throws Exception {
        HttpURLConnection connection = openUrlConnection(url, connectTimeoutMs, readTimeoutMs, maxRedirects);
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String message = connection.getResponseMessage();
                throw new IOException("无法下载URL, 响应码: " + responseCode + (message != null ? (" " + message) : ""));
            }
            long contentLength = connection.getContentLengthLong();
            int initialSize = (contentLength > 0 && contentLength <= Integer.MAX_VALUE) ? (int) contentLength : 8192;
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream(initialSize)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * 尝试打开一个URL链接，返回HttpURLConnection对象（已处理重定向、超时与请求头）。
     *
     * @param url 要打开的URL
     * @return HttpURLConnection 连接对象
     * @throws Exception 如果URL无效或请求失败
     */
    public static HttpURLConnection openUrlConnection(String url) throws Exception {
        return openUrlConnection(url, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS, DEFAULT_MAX_REDIRECTS);
    }

    /**
     * 尝试打开一个URL链接，返回HttpURLConnection对象（已处理重定向、超时与请求头）。
     *
     * @param url               要打开的URL
     * @param connectTimeoutMs  连接超时（毫秒）
     * @param readTimeoutMs     读取超时（毫秒）
     * @param maxRedirects      最大重定向次数
     * @return HttpURLConnection 连接对象
     * @throws Exception 如果URL无效或请求失败
     */
    public static HttpURLConnection openUrlConnection(String url, int connectTimeoutMs, int readTimeoutMs, int maxRedirects) throws Exception {
        if (!isValidUrl(url)) {
            throw new MalformedURLException("无效的URL(仅支持HTTP/HTTPS): " + url);
        }
        URL current = new URL(url);
        int redirects = 0;
        while (true) {
            HttpURLConnection connection = (HttpURLConnection) current.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("Connection", "close");
            int code = connection.getResponseCode();
            switch (code) {
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case 307:
                case 308: {
                    String location = connection.getHeaderField("Location");
                    connection.disconnect();
                    if (location == null || location.isEmpty()) {
                        throw new IOException("重定向响应缺少Location头");
                    }
                    URL next = new URL(current, location);
                    if (++redirects > maxRedirects) {
                        throw new IOException("重定向次数过多: " + redirects);
                    }
                    current = next;
                    continue;
                }
                default:
                    return connection;
            }
        }
    }

    /**
     * 包装InputStream, 在close时自动断开HttpURLConnection。
     */
    private static final class ConnectionInputStream extends FilterInputStream {
        private final HttpURLConnection connection;
        ConnectionInputStream(InputStream in, HttpURLConnection connection) {
            super(in);
            this.connection = connection;
        }
        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                connection.disconnect();
            }
        }
    }
}