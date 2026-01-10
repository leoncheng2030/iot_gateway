package vip.xiaonuo.iot.core.protocol.s7;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * S7数据转换工具类
 * 用于S7协议数据与Java数据类型之间的转换
 *
 * @author xiaonuo
 * @date 2026/01/10
 */
public class S7DataUtil {

    /**
     * 从字节数组中读取Boolean值（位）
     *
     * @param buffer 字节数组
     * @param byteIndex 字节索引
     * @param bitIndex 位索引 (0-7)
     * @return boolean值
     */
    public static boolean getBoolean(byte[] buffer, int byteIndex, int bitIndex) {
        if (buffer == null || byteIndex >= buffer.length) {
            return false;
        }
        return ((buffer[byteIndex] >> bitIndex) & 1) == 1;
    }

    /**
     * 从字节数组中读取Byte值
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @return byte值
     */
    public static byte getByte(byte[] buffer, int index) {
        if (buffer == null || index >= buffer.length) {
            return 0;
        }
        return buffer[index];
    }

    /**
     * 从字节数组中读取Word值（无符号16位整数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @return int值
     */
    public static int getWord(byte[] buffer, int index) {
        if (buffer == null || index + 1 >= buffer.length) {
            return 0;
        }
        return ((buffer[index] & 0xFF) << 8) | (buffer[index + 1] & 0xFF);
    }

    /**
     * 从字节数组中读取Int值（有符号16位整数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @return short值
     */
    public static short getInt(byte[] buffer, int index) {
        if (buffer == null || index + 1 >= buffer.length) {
            return 0;
        }
        return (short) (((buffer[index] & 0xFF) << 8) | (buffer[index + 1] & 0xFF));
    }

    /**
     * 从字节数组中读取DInt值（有符号32位整数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @return int值
     */
    public static int getDInt(byte[] buffer, int index) {
        if (buffer == null || index + 3 >= buffer.length) {
            return 0;
        }
        return ByteBuffer.wrap(buffer, index, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .getInt();
    }

    /**
     * 从字节数组中读取Real值（32位浮点数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @return float值
     */
    public static float getReal(byte[] buffer, int index) {
        if (buffer == null || index + 3 >= buffer.length) {
            return 0.0f;
        }
        return ByteBuffer.wrap(buffer, index, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .getFloat();
    }

    /**
     * 从字节数组中读取LReal值（64位浮点数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @return double值
     */
    public static double getLReal(byte[] buffer, int index) {
        if (buffer == null || index + 7 >= buffer.length) {
            return 0.0;
        }
        return ByteBuffer.wrap(buffer, index, 8)
                .order(ByteOrder.BIG_ENDIAN)
                .getDouble();
    }

    /**
     * 将Boolean值写入字节数组（位）
     *
     * @param buffer 字节数组
     * @param byteIndex 字节索引
     * @param bitIndex 位索引 (0-7)
     * @param value boolean值
     */
    public static void setBoolean(byte[] buffer, int byteIndex, int bitIndex, boolean value) {
        if (buffer == null || byteIndex >= buffer.length) {
            return;
        }
        if (value) {
            buffer[byteIndex] |= (byte) (1 << bitIndex);
        } else {
            buffer[byteIndex] &= (byte) ~(1 << bitIndex);
        }
    }

    /**
     * 将Byte值写入字节数组
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @param value byte值
     */
    public static void setByte(byte[] buffer, int index, byte value) {
        if (buffer == null || index >= buffer.length) {
            return;
        }
        buffer[index] = value;
    }

    /**
     * 将Word值写入字节数组（无符号16位整数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @param value int值
     */
    public static void setWord(byte[] buffer, int index, int value) {
        if (buffer == null || index + 1 >= buffer.length) {
            return;
        }
        buffer[index] = (byte) ((value >> 8) & 0xFF);
        buffer[index + 1] = (byte) (value & 0xFF);
    }

    /**
     * 将Int值写入字节数组（有符号16位整数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @param value short值
     */
    public static void setInt(byte[] buffer, int index, short value) {
        if (buffer == null || index + 1 >= buffer.length) {
            return;
        }
        buffer[index] = (byte) ((value >> 8) & 0xFF);
        buffer[index + 1] = (byte) (value & 0xFF);
    }

    /**
     * 将DInt值写入字节数组（有符号32位整数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @param value int值
     */
    public static void setDInt(byte[] buffer, int index, int value) {
        if (buffer == null || index + 3 >= buffer.length) {
            return;
        }
        ByteBuffer.wrap(buffer, index, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .putInt(value);
    }

    /**
     * 将Real值写入字节数组（32位浮点数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @param value float值
     */
    public static void setReal(byte[] buffer, int index, float value) {
        if (buffer == null || index + 3 >= buffer.length) {
            return;
        }
        ByteBuffer.wrap(buffer, index, 4)
                .order(ByteOrder.BIG_ENDIAN)
                .putFloat(value);
    }

    /**
     * 将LReal值写入字节数组（64位浮点数）
     *
     * @param buffer 字节数组
     * @param index 字节索引
     * @param value double值
     */
    public static void setLReal(byte[] buffer, int index, double value) {
        if (buffer == null || index + 7 >= buffer.length) {
            return;
        }
        ByteBuffer.wrap(buffer, index, 8)
                .order(ByteOrder.BIG_ENDIAN)
                .putDouble(value);
    }
}
