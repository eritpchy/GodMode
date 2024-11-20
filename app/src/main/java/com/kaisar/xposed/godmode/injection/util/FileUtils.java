package com.kaisar.xposed.godmode.injection.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by jrsen on 17-10-21.
 */

public final class FileUtils {

    public static boolean copy(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[4096];
            for (int len; ((len = in.read(buffer)) != -1); ) {
                out.write(buffer, 0, len);
            }
            out.flush();
            return true;
        } catch (IOException ignore) {
            return false;
        }
    }

    public static boolean delete(String filePath) {
        return delete(new File(filePath));
    }

    public static boolean delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File childFile : files) {
                    if (!delete(childFile)) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    public static boolean rmdir(String dirPath) {
        return delete(new File(dirPath));
    }

    /**
     * 创建一个新文件
     *
     * @param pFile    要创建的文件
     * @param pReplace 是否替换已经存在的文件
     * @throws IOException 创建文件时错误
     */
    public static boolean createNewFile(File pFile, boolean pReplace) throws IOException {
        if (pFile == null) return false;
        if (pFile.isFile()) {
            if (!pReplace) {
                return true;
            } else {
                delete(pFile);
            }
        }

        pFile = pFile.getAbsoluteFile();
        File tParent = pFile.getParentFile();
        if (tParent != null && !tParent.isDirectory()) {
            if (!tParent.mkdirs()) {
                throw new IOException("File '" + pFile + "' could not be created");
            }
        }
        return pFile.createNewFile();
    }

    /**
     * 使用给定的文件打开输出流
     * <p>
     * 如果文件不存在,将会自动创建
     * </p>
     *
     * @param pFile 指定的文件
     * @return 创建的输出流
     * @throws IOException 打开文件时报错
     */
    public static FileOutputStream openOutputStream(File pFile) throws IOException {
        return openOutputStream(pFile, true);
    }

    /**
     * 从给定的文件打开输出流
     * <p>
     * 如果文件不存在,将会自动创建
     * </p>
     *
     * @param pFile   指定的文件
     * @param pAppend 是否以追加模式打开
     * @return 创建的输出流
     * @throws IOException 可能发生的异常
     */
    public static FileOutputStream openOutputStream(File pFile, boolean pAppend) throws IOException {
        if (!pFile.isFile()) {
            createNewFile(pFile, !pAppend);
        }
        return new FileOutputStream(pFile, pAppend);
    }

    /**
     * 使用指定文件创建一个输入流
     *
     * @param pFile 要打开的文件
     * @return 创建的输入流
     * @throws IOException 创建输入流时发生错误
     */
    public static FileInputStream openInputStream(File pFile) throws IOException {
        if (!pFile.isFile()) {
            createNewFile(pFile, false);
        }
        return new FileInputStream(pFile);
    }

    /**
     * 将文件内容全部读取出来,并使用UTF-8编码转换为String
     *
     * @param pFile 要读取的文件
     * @return 文件内容
     * @throws IllegalStateException 报错
     */
    public static String readContent(File pFile) {
        InputStream tIPStream = null;
        try {
            tIPStream = openInputStream(pFile);
            return readContent(tIPStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            closeStream(tIPStream);
        }
    }


    /**
     * 将文件内容全部读取出来
     *
     * @param pFile 要读取的文件
     * @return 文件内容
     * @throws IllegalStateException 打开文件或读取数据时发生错误
     */
    public static byte[] readData(File pFile) {
        InputStream tIPStream = null;
        try {
            tIPStream = openInputStream(pFile);
            return readData(tIPStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            closeStream(tIPStream);
        }
    }

    public static void writeData(File pFile, String pData) {
        writeData(pFile, pData.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将文件内容全部读取出来
     *
     * @param pFile 要读取的文件
     * @param pData 写入的数据
     * @throws IllegalStateException 打开文件或读取数据时发生错误
     */
    public static void writeData(String pFile, String pData) {
        writeData(new File(pFile), pData.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将文件内容全部读取出来
     *
     * @param pFile 要读取的文件
     * @param pData 写入的数据
     * @throws IllegalStateException 打开文件或读取数据时发生错误
     */
    public static void writeData(File pFile, byte[] pData) {
        OutputStream tOStream = null;
        try {
            tOStream = openOutputStream(pFile, false);
            tOStream.write(pData, 0, pData.length);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            closeStream(tOStream);
        }
    }

    /**
     * 复制文件到新位置
     * <p>
     * 如果目标文件或文件夹不存在,将会自动创建<br>
     * 如果目标文件存在,将会覆盖
     * </p>
     *
     * @param pSourceFile   要被复制的文件
     * @param pDestFile     复制到的新文件
     * @param pCopyFileInfo 是否设置复制后的文件的修改日期与源文件相同
     * @throws IllegalStateException 源文件不存在,创建新文件时发生错误,读取数据时发生错误
     */
    public static boolean copyFile(File pSourceFile, File pDestFile, boolean pCopyFileInfo) {
        try {
            if (pSourceFile.getCanonicalPath().equals(pDestFile.getCanonicalPath()))
                return true;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        FileInputStream tIPStream = null;
        FileOutputStream tOPStream = null;
        try {
            tIPStream = new FileInputStream(pSourceFile);
            tOPStream = openOutputStream(pDestFile, false);
            if (!copy(tIPStream, tOPStream)) throw new IllegalStateException("file copy error");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            closeStream(tIPStream, tOPStream);
        }
        if (pCopyFileInfo) {
            pDestFile.setLastModified(Math.max(0, pSourceFile.lastModified()));
        }
        return true;
    }

    /**
     * 关闭一个流
     *
     * @param pSteams 流
     * @return 是否无报错的关闭了
     */
    public static boolean closeStream(Closeable... pSteams) {
        boolean pHasError = false;
        for (Closeable sCloseable : pSteams) {
            if (sCloseable != null) {
                try {
                    sCloseable.close();
                } catch (Exception exp) {
                    pHasError = true;
                }
            }
        }

        return !pHasError;
    }

    /**
     * 将流中的内容全部读取出来,并使用指定编码转换为String
     *
     * @param pIPStream 输入流
     * @return 读取到的内容
     * @throws IOException 读取数据时发生错误
     */
    public static String readContent(InputStream pIPStream) throws IOException {
        return readContent(new InputStreamReader(pIPStream, StandardCharsets.UTF_8));
    }

    /**
     * 将流中的内容全部读取出来
     *
     * @param pIPSReader 输入流
     * @return 读取到的内容
     * @throws IOException 读取数据时发生错误
     */
    public static String readContent(InputStreamReader pIPSReader) throws IOException {
        int readCount = 0;
        char[] tBuff = new char[4096];
        StringBuilder tSB = new StringBuilder();
        while ((readCount = pIPSReader.read(tBuff)) != -1) {
            tSB.append(tBuff, 0, readCount);
        }
        return tSB.toString();
    }

    /**
     * 将流中的内容全部读取出来
     *
     * @param pIStream 输入流
     * @return 读取到的内容
     * @throws IOException 读取数据时发生错误
     */
    public static byte[] readData(InputStream pIStream) throws IOException {
        ByteArrayOutputStream tBAOStream = new ByteArrayOutputStream();
        copy(pIStream, tBAOStream);
        return tBAOStream.toByteArray();
    }
}
