package com.my.procty;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.Socket;

public class MinitouchController {
    private static final String TAG = "ProcTy_Minitouch";
    private static Socket socket = null;
    private static OutputStream outputStream = null;
    private static Process process = null;

    public static void init(final Context context) {
        new Thread(() -> {
            try {
                // 1. TRỎ THẲNG TỚI FILE TRONG THƯ MỤC DOWNLOAD
                File minitouchFile = new File("/storage/emulated/0/Download/minitouch");
                if (!minitouchFile.exists()) {
                    Log.e(TAG, "Không tìm thấy file minitouch trong thư mục Download!");
                    return;
                }

                // 2. Dùng Reflection gọi Shizuku.newProcess
                Class<?> shizukuClass = Class.forName("rikka.shizuku.Shizuku");
                Method newProcessMethod = shizukuClass.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                newProcessMethod.setAccessible(true);

                // 3. Chạy lệnh chmod +x
                String chmodCmd = "chmod 755 " + minitouchFile.getAbsolutePath();
                Process chmodProcess = (Process) newProcessMethod.invoke(null, new String[]{"sh", "-c", chmodCmd}, null, null);
                if (chmodProcess != null) {
                    int chmodExit = chmodProcess.waitFor();
                    Log.d(TAG, "chmod exit code: " + chmodExit);
                }

                // 4. Khởi chạy Minitouch ngầm
                String startCmd = minitouchFile.getAbsolutePath() + " -i 127.0.0.1";
                process = (Process) newProcessMethod.invoke(null, new String[]{"sh", "-c", startCmd}, null, null);
                
                // 5. THỬ KẾT NỐI SOCKET VỚI CƠ CHẾ RETRY
                boolean connected = false;
                for (int i = 0; i < 3; i++) {
                    try {
                        Thread.sleep(1000);
                        socket = new Socket("127.0.0.1", 1111);
                        outputStream = socket.getOutputStream();
                        connected = true;
                        break;
                    } catch (Exception e) {
                        Log.w(TAG, "Thử kết nối lần " + (i + 1) + " thất bại: " + e.getMessage());
                    }
                }

                if (connected) {
                    Log.i(TAG, "Minitouch đã sẵn sàng!");
                } else {
                    Log.e(TAG, "Lỗi: Không thể kết nối tới Minitouch sau 3 lần thử!");
                    close();
                }

            } catch (Exception e) {
                Log.e(TAG, "Lỗi khởi tạo Minitouch: " + e.getMessage());
                e.printStackTrace();
                close();
            }
        }).start();
    }

    public static void swipe(int startX, int startY, int endX, int endY, int durationMs) {
        if (outputStream == null) return;
        try {
            String cmd = "d 0 " + startX + " " + startY + " 50\n" +
                         "m 0 " + endX + " " + endY + " 50\n" +
                         "u 0\n" +
                         "c\n";
            outputStream.write(cmd.getBytes());
            outputStream.flush();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi gửi lệnh Minitouch: " + e.getMessage());
        }
    }
    
    public static void close() {
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        try { if (process != null) process.destroy(); } catch (Exception ignored) {}
        socket = null;
        outputStream = null;
        process = null;
    }
                                     }
