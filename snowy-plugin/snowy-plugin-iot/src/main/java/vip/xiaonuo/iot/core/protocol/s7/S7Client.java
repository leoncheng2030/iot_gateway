package vip.xiaonuo.iot.core.protocol.s7;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * S7协议客户端 (简化版)
 * 用于与西门子S7系列PLC通信
 * 基于ISO-on-TCP协议实现
 *
 * @author xiaonuo
 * @date 2026/01/10
 */
@Slf4j
@Component
public class S7Client {

    /**
     * S7连接池 - Key: deviceId, Value: Socket实例
     */
    private final Map<String, Socket> connectionPool = new ConcurrentHashMap<>();

    /**
     * 连接到S7 PLC
     *
     * @param deviceId 设备ID
     * @param config   连接配置
     * @return 是否连接成功
     */
    public boolean connect(String deviceId, JSONObject config) {
        try {
            // 解析配置
            String host = config.getStr("host");
            int port = config.getInt("port", 102);
            int rack = config.getInt("rack", 0);
            int slot = config.getInt("slot", 2);
            boolean skipHandshake = config.getBool("skipHandshake", false); // 是否跳过握手

            if (ObjectUtil.isEmpty(host)) {
                log.error("S7连接失败 - 缺少host配置: DeviceId: {}", deviceId);
                return false;
            }

            // 检查是否已连接
            Socket socket = connectionPool.get(deviceId);
            if (socket != null && socket.isConnected() && !socket.isClosed()) {
                log.debug("S7设备已连接 - DeviceId: {}", deviceId);
                return true;
            }

            // 创建新连接
            socket = new Socket(host, port);
            socket.setSoTimeout(5000); // 5秒超时
            socket.setTcpNoDelay(true);

            // 执行S7连接握手（可选）
            if (skipHandshake) {
                // 跳过握手，直接认为连接成功
                connectionPool.put(deviceId, socket);
                log.warn("S7连接成功(跳过握手) - DeviceId: {}, Host: {}, Port: {}", 
                        deviceId, host, port);
                return true;
            } else if (performHandshake(socket, rack, slot)) {
                connectionPool.put(deviceId, socket);
                log.info("S7连接成功 - DeviceId: {}, Host: {}, Port: {}, Rack: {}, Slot: {}",
                        deviceId, host, port, rack, slot);
                return true;
            } else {
                socket.close();
                log.error("S7连接握手失败 - DeviceId: {}", deviceId);
                return false;
            }
        } catch (Exception e) {
            log.error("S7连接异常 - DeviceId: {}", deviceId, e);
            return false;
        }
    }

    /**
     * 断开连接
     *
     * @param deviceId 设备ID
     */
    public void disconnect(String deviceId) {
        try {
            Socket socket = connectionPool.remove(deviceId);
            if (socket != null && !socket.isClosed()) {
                socket.close();
                log.info("S7断开连接 - DeviceId: {}", deviceId);
            }
        } catch (Exception e) {
            log.error("S7断开连接异常 - DeviceId: {}", deviceId, e);
        }
    }

    /**
     * 读取DB块数据
     *
     * @param deviceId   设备ID
     * @param dbNumber   DB块号
     * @param start      起始地址
     * @param size       读取字节数
     * @return 读取的字节数组
     */
    public byte[] readDB(String deviceId, int dbNumber, int start, int size) {
        return readArea(deviceId, 0x84, dbNumber, start, size);
    }

    /**
     * 读取M区数据
     *
     * @param deviceId 设备ID
     * @param start    起始地址
     * @param size     读取字节数
     * @return 读取的字节数组
     */
    public byte[] readMerker(String deviceId, int start, int size) {
        return readArea(deviceId, 0x83, 0, start, size);
    }

    /**
     * 写入DB块数据
     *
     * @param deviceId 设备ID
     * @param dbNumber DB块号
     * @param start    起始地址
     * @param data     写入的数据
     * @return 是否写入成功
     */
    public boolean writeDB(String deviceId, int dbNumber, int start, byte[] data) {
        return writeArea(deviceId, 0x84, dbNumber, start, data);
    }

    /**
     * 检查设备是否已连接
     *
     * @param deviceId 设备ID
     * @return 是否已连接
     */
    public boolean isConnected(String deviceId) {
        Socket socket = connectionPool.get(deviceId);
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * 关闭所有连接
     */
    public void closeAll() {
        connectionPool.forEach((deviceId, socket) -> {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                log.error("关闭S7连接异常 - DeviceId: {}", deviceId, e);
            }
        });
        connectionPool.clear();
        log.info("S7所有连接已关闭");
    }

    /**
     * 执行S7连接握手
     */
    private boolean performHandshake(Socket socket, int rack, int slot) {
        try {
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // 1. 发送ISO-on-TCP连接请求
            byte[] isoConnectRequest = buildIsoConnectRequest(rack, slot);
            out.write(isoConnectRequest);
            out.flush();

            // 2. 接收响应
            byte[] response = new byte[22];
            int bytesRead = in.read(response);
            if (bytesRead < 22) {
                return false;
            }

            // 3. 发送S7连接请求
            byte[] s7ConnectRequest = buildS7ConnectRequest();
            out.write(s7ConnectRequest);
            out.flush();

            // 4. 接收响应
            response = new byte[27];
            bytesRead = in.read(response);
            return bytesRead >= 27;

        } catch (Exception e) {
            log.error("S7握手异常", e);
            return false;
        }
    }

    /**
     * 构建ISO连接请求
     */
    private byte[] buildIsoConnectRequest(int rack, int slot) {
        byte[] request = {
                0x03, 0x00, 0x00, 0x16, // TPKT Header
                0x11, (byte) 0xE0, 0x00, 0x00, // COTP Header
                0x00, 0x01, 0x00,
                (byte) 0xC1, 0x02, 0x01, 0x00, // Source TSAP
                (byte) 0xC2, 0x02, 0x01, (byte) ((rack * 32) + slot) // Destination TSAP
        };
        return request;
    }

    /**
     * 构建S7连接请求
     */
    private byte[] buildS7ConnectRequest() {
        byte[] request = {
                0x03, 0x00, 0x00, 0x19, // TPKT Header
                0x02, (byte) 0xF0, (byte) 0x80, // COTP Header
                0x32, 0x01, 0x00, 0x00, // S7 Header
                0x04, 0x00, 0x00, 0x08, 0x00, 0x00,
                (byte) 0xF0, 0x00, 0x00, 0x01, 0x00, 0x01, 0x01, (byte) 0xE0
        };
        return request;
    }

    /**
     * 读取区域数据
     */
    private byte[] readArea(String deviceId, int area, int dbNumber, int start, int size) {
        try {
            Socket socket = connectionPool.get(deviceId);
            if (socket == null || socket.isClosed()) {
                log.warn("S7设备未连接 - DeviceId: {}", deviceId);
                return null;
            }
    
            // 构建 S7读取请求
            byte[] request = buildReadRequest(area, dbNumber, start, size);
            log.info("S7发送读取请求 - DeviceId: {}, Area: 0x{}, DB: {}, Start: {}, Size: {}, 请求字节: {}", 
                deviceId, Integer.toHexString(area), dbNumber, start, size, bytesToHexString(request));
    
            // 发送请求
            OutputStream out = socket.getOutputStream();
            out.write(request);
            out.flush();
    
            // 接收响应
            InputStream in = socket.getInputStream();
            byte[] header = new byte[7];
            int headerRead = in.read(header);
            log.info("S7接收响应头 - DeviceId: {}, 读取字节: {}, Header: {}", 
                deviceId, headerRead, bytesToHexString(header));
    
            // 读取PDU数据
            int pduLength = ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
            log.info("S7 PDU长度 - DeviceId: {}, Length: {}", deviceId, pduLength);
                
            byte[] pdu = new byte[pduLength - 7];
            int pduRead = in.read(pdu);
            log.info("S7接收PDU数据 - DeviceId: {}, 读取字节: {}, PDU: {}", 
                deviceId, pduRead, bytesToHexString(pdu));
    
            // 提取数据
            // 查找成功标志 0xFF 的位置（跳过握手后格式可能不同）
            int successFlagIndex = -1;
            for (int i = 0; i < Math.min(pdu.length - 4, 20); i++) {
                if (pdu[i] == (byte) 0xFF && pdu[i + 1] == (byte) 0x04) {
                    successFlagIndex = i;
                    break;
                }
            }
            
            if (successFlagIndex >= 0 && pdu.length >= successFlagIndex + 4 + size) {
                log.info("S7找到成功标志 - DeviceId: {}, 位置: {}", deviceId, successFlagIndex);
                // 数据从成功标志后4字节开始
                byte[] data = new byte[size];
                System.arraycopy(pdu, successFlagIndex + 4, data, 0, size);
                log.info("S7读取成功 - DeviceId: {}, 数据: {}", deviceId, bytesToHexString(data));
                return data;
            } else {
                log.error("S7未找到成功标志或数据不足 - DeviceId: {}, successFlagIndex: {}, pduLength: {}", 
                    deviceId, successFlagIndex, pdu.length);
            }
    
            return null;
        } catch (Exception e) {
            log.error("S7读取区域异常 - DeviceId: {}", deviceId, e);
            return null;
        }
    }
        
    private String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    /**
     * 写入区域数据
     */
    private boolean writeArea(String deviceId, int area, int dbNumber, int start, byte[] data) {
        try {
            Socket socket = connectionPool.get(deviceId);
            if (socket == null || socket.isClosed()) {
                log.warn("S7设备未连接 - DeviceId: {}", deviceId);
                return false;
            }

            // 构建S7写入请求
            byte[] request = buildWriteRequest(area, dbNumber, start, data);

            // 发送请求
            OutputStream out = socket.getOutputStream();
            out.write(request);
            out.flush();

            // 接收响应
            InputStream in = socket.getInputStream();
            byte[] response = new byte[22];
            int bytesRead = in.read(response);

            return bytesRead >= 22 && response[21] == (byte) 0xFF;
        } catch (Exception e) {
            log.error("S7写入区域异常 - DeviceId: {}", deviceId, e);
            return false;
        }
    }

    /**
     * 构建S7读取请求
     */
    private byte[] buildReadRequest(int area, int dbNumber, int start, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(31);

        // TPKT Header
        buffer.put((byte) 0x03);
        buffer.put((byte) 0x00);
        buffer.putShort((short) 31);

        // COTP Header
        buffer.put((byte) 0x02);
        buffer.put((byte) 0xF0);
        buffer.put((byte) 0x80);

        // S7 Header
        buffer.put((byte) 0x32); // Protocol ID
        buffer.put((byte) 0x01); // ROSCTR (Job)
        buffer.putShort((short) 0x0000); // Redundancy ID
        buffer.putShort((short) 0x0001); // PDU Reference
        buffer.putShort((short) 0x000E); // Parameters Length
        buffer.putShort((short) 0x0000); // Data Length

        // Read Parameter
        buffer.put((byte) 0x04); // Function: Read Var
        buffer.put((byte) 0x01); // Item Count

        // Item Specification
        buffer.put((byte) 0x12); // Variable Specification
        buffer.put((byte) 0x0A); // Length of following address
        buffer.put((byte) 0x10); // Syntax ID: S7ANY
        buffer.put((byte) 0x02); // Transport size: BYTE
        buffer.putShort((short) (size * 8)); // Length in bits
        buffer.putShort((short) dbNumber); // DB Number
        buffer.put((byte) area); // Area Code
        buffer.put((byte) (start >> 16)); // Address (byte 2)
        buffer.putShort((short) (start & 0xFFFF)); // Address (byte 0-1)

        return buffer.array();
    }

    /**
     * 构建S7写入请求
     */
    private byte[] buildWriteRequest(int area, int dbNumber, int start, byte[] data) {
        int dataLength = data.length;
        ByteBuffer buffer = ByteBuffer.allocate(35 + dataLength);

        // TPKT Header
        buffer.put((byte) 0x03);
        buffer.put((byte) 0x00);
        buffer.putShort((short) (35 + dataLength));

        // COTP Header
        buffer.put((byte) 0x02);
        buffer.put((byte) 0xF0);
        buffer.put((byte) 0x80);

        // S7 Header
        buffer.put((byte) 0x32);
        buffer.put((byte) 0x01);
        buffer.putShort((short) 0x0000);
        buffer.putShort((short) 0x0001);
        buffer.putShort((short) 0x000E);
        buffer.putShort((short) (dataLength + 4));

        // Write Parameter
        buffer.put((byte) 0x05); // Function: Write Var
        buffer.put((byte) 0x01);

        // Item Specification
        buffer.put((byte) 0x12);
        buffer.put((byte) 0x0A);
        buffer.put((byte) 0x10);
        buffer.put((byte) 0x02);
        buffer.putShort((short) (dataLength * 8));
        buffer.putShort((short) dbNumber);
        buffer.put((byte) area);
        buffer.put((byte) (start >> 16));
        buffer.putShort((short) (start & 0xFFFF));

        // Data
        buffer.put((byte) 0x00); // Reserved
        buffer.put((byte) 0x04); // Transport size: BYTE
        buffer.putShort((short) (dataLength * 8)); // Length in bits
        buffer.put(data);

        return buffer.array();
    }
}
