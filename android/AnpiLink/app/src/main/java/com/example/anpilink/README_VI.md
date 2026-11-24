# AnpiLink: lấy vị trí khi trả lời offline

## Cài vào bản đang phát triển

Sao lưu các file hiện tại trước khi thay thế. Trong thư mục:

`C:\work\android\AnpiLink\app\src\main\java\com\example\anpilink\`

- Tạo mới `StudentSettingsCache.kt`.
- Thay toàn bộ `StudentSettingsApi.kt`.
- Thay toàn bộ `StudentSettingsContent.kt`.
- Thay toàn bộ `SafetyReportApi.kt`.

Các file trong gói đều là file đầy đủ. Không tạo thêm class trùng tên.
Giữ `StudentLocationProvider.kt` có log vừa thêm, cùng PendingSafetyReportStore/Sender hiện tại.

Trong AndroidManifest.xml, trước `<application>`, cần có các quyền (không thêm trùng):

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

Build từ thư mục project:

```powershell
cd C:\work\android\AnpiLink
.\gradlew.bat assembleDebug
```

## Hành vi mới

- Khi online, đọc cài đặt server và lưu riêng theo server + userId.
- Khi offline hoặc lỗi kết nối/HTTP tạm thời, dùng cài đặt đã lưu. Không mặc định bật vị trí nếu chưa từng biết cài đặt.
- 401/403 không dùng cache để bỏ qua lỗi xác thực/quyền.
- Nếu đã bật và cấp quyền, thử lấy vị trí tại thời điểm trả lời ngay cả khi không có Internet.
- Vị trí lấy được được lưu cùng câu trả lời và locationAcquiredAt. Không lấy lại vị trí hoặc đổi requestId khi gửi lại.
- OFF được lưu ở máy trước khi gọi server. Nếu offline hoặc POST thất bại, máy vẫn giữ OFF; lần đọc cài đặt có mạng tiếp theo thử đồng bộ OFF lên server.
- Bật ON và đổi thông báo vẫn cần xác nhận từ server. Màn hình offline có ghi rõ đang dùng cài đặt đã lưu.
- Bỏ một bản submitPending bị dán thừa dưới dạng hàm cục bộ trong submit; giữ hàm submitPending ở cấp object.

## Kiểm tra tại máy

1. Chạy backend local, đăng nhập học sinh, mở その他 → 設定 khi có mạng. Đảm bảo màn hình đã tải cài đặt từ server và 位置情報の利用 = ON.
2. Cấp quyền vị trí cho Android. Để kiểm tra GPS khi offline, chọn vị trí chính xác (Precise).
3. Máy ảo: mở Extended Controls → Location và cấp một điểm mới bằng Set Location. Máy ảo dùng điểm giả lập, không phải GPS thật của máy tính.
4. Tắt Wi-Fi và dữ liệu di động của thiết bị, giữ vị trí Android/GPS bật. Với điện thoại thật, không cần dùng chế độ máy bay để thử nghiệm này.
5. Gửi một câu trả lời MỚI. Lọc Logcat: `package:com.example.anpilink tag:AnpiLinkLocation`.
6. Kỳ vọng log `source=device, locationEnabled=true`, sau đó log từ StudentLocationProvider. Nếu lấy được, có `回答時の位置情報を取得成功`.
7. Trở lại mạng: kiểm tra bản trả lời tự gửi và tọa độ/thời điểm lấy vẫn là dữ liệu đã lưu lúc trả lời. Backend phải có xử lý requestId và đủ quyền DB.
8. Kiểm tra OFF: khi offline, mở cài đặt từ cache và tắt vị trí. Lần trả lời mới tiếp theo không được lấy vị trí. Đọc lại cài đặt khi online để đồng bộ OFF.
9. Kiểm tra tài khoản/server khác: khi chưa có cache tương ứng, không tự mặc định locationEnabled=true.

Nếu vẫn có câu trả lời cũ chưa xử lý, giữ nguyên bản đó. Gói này không bổ sung vị trí vào bản đã lưu và không xóa bản lưu. Hoàn tất xử lý bản cũ trước khi thử câu trả lời mới.

## Giới hạn

- Vẫn chờ tối đa 4 giây mỗi provider và chỉ chấp nhận vị trí tối đa 60 giây tuổi khi lấy. Không đảm bảo GPS luôn lấy được vị trí, nhất là trong nhà hoặc khi chỉ cấp vị trí gần đúng.
- Đây là thay đổi phía Android; không sửa lỗi SQLState=42501 của backend. Lỗi đó vẫn cần xử lý riêng nếu còn xuất hiện.
- Không thay BASE_URL, không deploy EC2, không thay APK đã gửi tester.
- Gói được soát xét theo các source bạn cung cấp; chưa được build/chạy trên project Android đầy đủ trong môi trường này.
