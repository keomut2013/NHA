# IPTV Smart Player (Android & Android TV)

Dự án mẫu IPTV Player được thiết kế theo chuẩn hiện đại của Google, hỗ trợ phát luồng m3u8/TS/FLV và cơ chế tự động cập nhật 2 tầng (Hot-reload danh sách kênh + OTA In-App Update).

---

## Cấu trúc thư mục

* `app/src/main/java/com/iptv/player/`:
  * `MainActivity.kt`: Điều khiển phát video, hiển thị danh sách kênh và lắng nghe nút bấm Remote TV (D-pad).
  * `data/M3uParser.kt`: Bộ bóc tách danh sách kênh M3U tốc độ cao (hỗ trợ group-title, tvg-logo, http-user-agent).
  * `network/ChannelRepository.kt`: Tải danh sách kênh từ xa, tự động lưu bộ nhớ tạm (Cache TTL 2h) để xem mượt khi mạng chập chờn.
  * `updater/OtaUpdater.kt`: Trình tự động cập nhật ứng dụng in-app qua file `version.json`, kiểm tra mã SHA-256 chống lỗi file và gọi `FileProvider` để cài đè.
* `config_example/`:
  * `version.json`: File mẫu cấu hình phiên bản bạn đưa lên GitHub.
  * `playlist.m3u`: File mẫu danh sách kênh bạn đưa lên GitHub.

---

## Cách đưa vào vận hành

1. **Đưa cấu hình lên GitHub:**
   * Tạo 1 repo trên GitHub (ví dụ: `my-iptv-config`).
   * Tải 2 file trong thư mục `config_example` lên repo đó.
   * Lấy link dạng "Raw" (ví dụ: `https://raw.githubusercontent.com/<username>/<repo>/main/...`).
   * Thay 2 đường link này vào dòng 36 và 37 trong file `MainActivity.kt`.

2. **Mở dự án bằng Android Studio:**
   * Mở Android Studio -> Chọn **Open** -> Trỏ tới thư mục:
     `C:\Users\PhongPC\.gemini\antigravity\scratch\iptv-player-android`
   * Chờ Gradle đồng bộ (Sync Project with Gradle Files).
   * Bấm nút **Run** (chạy thử trên máy ảo hoặc thiết bị thật) hoặc chọn **Build > Build APK(s)** để xuất file `.apk`.

