package vip.xiaonuo;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * 测试Snap7 Server连接
 */
public class TestSnap7 {
    
    @Test
    public void testSnap7Connection() {
        try {
            System.out.println("=== 开始测试Snap7 Server连接 ===");
            
            // 1. 建立TCP连接
            System.out.println("1. 连接到 127.0.0.1:102...");
            Socket socket = new Socket("127.0.0.1", 102);
            socket.setSoTimeout(5000);
            System.out.println("   ✅ TCP连接成功！");
            
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();
            
            // 2. 发送ISO-on-TCP连接请求
            byte[] isoRequest = {
                0x03, 0x00, 0x00, 0x16, // TPKT Header
                0x11, (byte) 0xE0, 0x00, 0x00, // COTP Header
                0x00, 0x01, 0x00,
                (byte) 0xC1, 0x02, 0x01, 0x00, // Source TSAP
                (byte) 0xC2, 0x02, 0x01, 0x00  // Destination TSAP (rack=0, slot=0)
            };
            
            System.out.println("2. 发送ISO握手请求...");
            System.out.print("   请求数据: ");
            for (byte b : isoRequest) {
                System.out.printf("%02X ", b);
            }
            System.out.println();
            
            out.write(isoRequest);
            out.flush();
            
            // 3. 等待响应
            System.out.println("3. 等待Snap7响应（5秒超时）...");
            byte[] response = new byte[22];
            int bytesRead = in.read(response);
            
            if (bytesRead > 0) {
                System.out.println("   ✅ 接收到响应！字节数: " + bytesRead);
                System.out.print("   响应数据: ");
                for (int i = 0; i < bytesRead; i++) {
                    System.out.printf("%02X ", response[i]);
                }
                System.out.println();
                System.out.println("   ✅ ISO握手成功！");
            } else {
                System.out.println("   ❌ 未接收到任何响应");
            }
            
            socket.close();
            System.out.println("=== 测试完成 ===");
            
        } catch (java.net.SocketTimeoutException e) {
            System.out.println("   ❌ 超时！Snap7 Server没有响应");
            System.out.println("   可能原因：");
            System.out.println("   1. Snap7 Server未正确启动");
            System.out.println("   2. Snap7 Server不支持此握手协议");
            System.out.println("   3. 端口102被其他程序占用");
        } catch (Exception e) {
            System.out.println("   ❌ 连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
