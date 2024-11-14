package com.deqiying.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * 文件指纹工具类
 *
 * @author qiying
 * @since 2024-11-13
 */
@SuppressWarnings("unused")
public class FileFingerprintUtils {
    private static final String HASH_ALGORITHM = "SHA-256";


    /**
     * 生成文件指纹。
     *
     * @param filePath 文件路径。
     * @return 文件指纹。
     */
    public static String generateFingerprint(Path filePath) {
        // 使用 try-with-resources 自动关闭文件输入流
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            return generateFingerprint(fis);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 生成文件指纹。
     *
     * @param file 文件。
     * @return 文件指纹。
     */
    public static String generateFingerprint(File file) {
        // 使用 try-with-resources 自动关闭文件输入流
        try (FileInputStream fis = new FileInputStream(file)) {
            return generateFingerprint(fis);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 生成文件指纹。
     *
     * @param inputStream 文件输入流。
     * @return 文件指纹。
     */
    public static String generateFingerprint(InputStream inputStream) {
        try {
            // 初始化 SHA-256 消息摘要
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            // 读取文件内容
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(messageDigest.digest());
        } catch (Throwable ignored) {

        }
        return null;
    }


    /**
     * 生成文件指纹。
     *
     * @param fileBytes 文件字节数组。
     * @return 文件指纹。
     */
    public static String generateFingerprint(byte[] fileBytes) {
        try {
            // 初始化 SHA-256 消息摘要
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            // 读取文件内容
            messageDigest.update(fileBytes);
            return bytesToHex(messageDigest.digest());
        } catch (Throwable ignored) {

        }
        return null;
    }

    /**
     * 将字节数组转换为十六进制字符串。
     *
     * @param bytes 字节数组。
     * @return 十六进制字符串。
     */
    public static String bytesToHex(byte[] bytes) {
        // 字节数组转换成的十六进制字符串
        if (bytes == null) {
            return null;
        }
        // 为了避免扩容，直接指定足够的初始容量。SHA-256 哈希值通常有 64 个字符
        // 每个字节转为两个字符
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

}
