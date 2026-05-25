# Ứng Dụng Phân Tích Hình Ảnh Chuẩn Đoán Bệnh Cây Trồng Bằng AI (SmartCropDoctor-Android)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg?style=for-the-badge&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Java%20-blue.svg?style=for-the-badge" alt="Language">
  <img src="https://img.shields.io/badge/AI%2FML-TensorFlow%20Lite-orange.svg?style=for-the-badge&logo=tensorflow" alt="AI/ML">
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Backend-Firebase-yellow.svg?style=for-the-badge&logo=firebase" alt="Backend">
  <img src="https://img.shields.io/badge/License-MIT-purple.svg?style=for-the-badge" alt="License">
</p>

**Smart Crop Doctor** là ứng dụng di động chạy trên nền tảng Android, sử dụng Trí tuệ nhân tạo (AI) để phân tích hình ảnh và chẩn đoán các loại sâu bệnh trên lá cây (đốm lá, rỉ sắt, vàng lá...) theo thời gian thực. 

Dự án được thiết kế với tư duy kiến trúc mở, đóng vai trò là một mảnh ghép phân tích hình ảnh trong hệ sinh thái Nông nghiệp thông minh, sẵn sàng mở rộng kết hợp với các hệ thống phần cứng IoT (như vi điều khiển ESP32, cảm biến độ ẩm đất và hệ thống trực quan hóa ThingsBoard).

---

## 🚀 Tính Năng Cốt Lõi & Đáp Ứng Thang Điểm

### 1. Tối Ưu Hóa Mô Hình Trên Thiết Bị Đầu Cuối 
Ứng dụng tích hợp hai phiên bản mô hình để phục vụ bài toán so sánh hiệu năng trong báo cáo kỹ thuật:
* **Bản gốc (Float32):** Cho độ chính xác cao nhất, dùng làm mốc đối chứng (Baseline).
* **Bản lượng tử hóa (Quantized INT8):** Áp dụng kỹ thuật *Post-Training Quantization* giúp giảm dung lượng file mô hình xuống ~4 lần, tối ưu tốc độ suy luận (Inference Time) và tiết kiệm RAM/Pin cho thiết bị di động.
* *Tính năng phụ trợ:* Nút chuyển đổi (Toggle) trực tiếp trên giao diện để đo lường và so sánh FPS, thời gian xử lý giữa 2 phiên bản mô hình.

### 2. Triển Khai Linh Hoạt Theo Tư Duy MLOps
Dịch bệnh cây trồng thay đổi liên tục theo mùa vụ. Thay vì phải biên dịch lại mã nguồn, ứng dụng áp dụng quy trình MLOps thông qua **Firebase ML Custom Model**:
* **Cơ chế Fallback:** Nhúng sẵn mô hình mặc định trong thư mục `assets` để chạy ngoại tuyến (Offline) ngay khi cài app.
* **Cập nhật Over-The-Air (OTA):** Tự động kiểm tra và tải ngầm (Background download) phiên bản mô hình mới nhất từ Firebase Console khi có kết nối Internet, giúp cập nhật danh mục nhận diện bệnh mới một cách xuyên suốt.

### 3. Kỹ Thuật Xử Lý Ảnh Thời Gian Thực & Lưu Trữ 
* **CameraX Real-time Scanning:** Sử dụng Use Case `ImageAnalysis` của Jetpack CameraX để trích xuất luồng hình ảnh trực tiếp. Áp dụng kỹ thuật điều tiết khung hình (Inference Throttling - chạy 3-5 frame/giây) để chống quá nhiệt và giật lag.
* **Cơ sở dữ liệu kép (Local & Cloud):**
    * **Room Database:** Lưu trữ dữ liệu cấu hình cục bộ, hiển thị phác đồ và cách chữa trị bệnh ngay lập tức kể cả khi không có mạng.
    * **Firebase Firestore:** Đồng bộ các bài thuốc, danh mục thuốc bảo vệ thực vật mới nhất từ đám mây.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

* **IDE:** Android Studio
* **Ngôn ngữ:** Java 
* **Camera Framework:** Jetpack CameraX (`Core`, `Camera2`, `Lifecycle`, `View`)
* **Machine Learning Runtime:** TensorFlow Lite (TFLite) & TFLite Support Library
* **Cloud & MLOps:** Firebase (Firebase ML Model Downloader, Firestore)
* **Local Storage:** Room Persistence Library

---

## 📁 Cấu Trúc Thư Mục Dự Án (Cốt Lõi)
Dưới đây là sơ đồ tổ chức thư mục chính:
```text
📁 app/
└── 📁 src/
    └── 📁 main/
        ├── 📁 assets/
        │   ├── 📄 disease_solutions.json        # Danh mục giải pháp, phác đồ điều trị bệnh (JSON)
        │   ├── 📄 labels.txt                    # Danh sách nhãn tên bệnh cây trồng cục bộ
        │   └── 📄 model.tflite                  # Tệp mô hình AI mặc định (Offline Fallback)
        ├── 📁 java/ntu/viet773092/ungDungCdbct_65134318/
        │   ├── 📁 adapter/
        │   │   └── 📄 HistoryAdapter.java       # Bộ nạp dữ liệu và xử lý sự kiện cho danh sách Nhật ký
        │   ├── 📁 classifier/
        │   │   └── 📄 TFLiteClassifier.java     # Trình quản lý nạp mô hình AI (LiteRT/TFLite) và suy luận ảnh
        │   ├── 📁 database/
        │   │   └── 📄 HistoryDatabaseHelper.java # Cấu hình và quản lý cơ sở dữ liệu nhật ký chẩn đoán (SQLite)
        │   ├── 📁 ui/
        │   │   ├── 📄 DetailActivity.java       # Hiển thị chi tiết phác đồ, ảnh lớn bệnh và đọc TextToSpeech (TTS)
        │   │   ├── 📄 HistoryActivity.java      # Giao diện hiển thị danh sách nhật ký chẩn đoán đã lưu (RecyclerView)
        │   │   └── 📄 MainActivity.java         # Màn hình chính: Điều phối CameraX, phân tích luồng AI Đám mây/Offline
        │   └── 📄 MainApplication.java          # Lớp ứng dụng toàn cục: Đăng ký Lifecycle quản lý Context màn hình
        └── 📁 res/
            └── 📁 layout/
                ├── 📄 activity_main.xml         # UI Màn hình quét CameraX, khung kết quả chẩn đoán và các nút lệnh
                ├── 📄 activity_detail.xml       # UI Trang chi tiết giải pháp: Ảnh lớn, mô tả bệnh và nút phát âm thanh
                ├── 📄 activity_history.xml      # UI Trang danh sách nhật ký chẩn đoán
                └── 📄 item_history.xml          # UI Thiết kế từng dòng thẻ (Card) hiển thị trong Nhật ký                     # Cấu hình thư viện Dependencies
