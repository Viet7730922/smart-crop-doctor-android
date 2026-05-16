# Smart Crop Doctor (SmartCropDoctor-Android)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg?style=flat-shaped&logo=android" alt="Platform" height="40">
  <img src="https://img.shields.io/badge/Language-Java%20%2F%20Kotlin-blue.svg" alt="Language" height="28">
  <img src="https://img.shields.io/badge/AI%2FML-TensorFlow%20Lite-orange.svg?logo=tensorflow" alt="AI/ML" height="28">
  <img src="https://img.shields.io/badge/Backend-Firebase-yellow.svg?logo=firebase" alt="Backend" height="28">
  <img src="https://img.shields.io/badge/License-MIT-purple.svg" alt="License" height="28">
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
## 👨‍🎓 Thông tin sinh viên
| Đặc điểm | Chi tiết |
| :--- | :--- |
| **Họ và Tên** | Võ Quốc Việt |
| **MSSV** | 65134318 |
| **Lớp** | 65.CNTT-1 |
| **Email** | [viet.vq.65cntt@ntu.edu.vn](mailto:viet.vq.65cntt@ntu.edu.vn) |
| **Số điện thoại** | 0846 103 268 |

---

## 🛠 Môi trường phát triển & Kỹ thuật
Để chạy project này, hãy đảm bảo môi trường của bạn đáp ứng các thông số sau:

* **IDE:** Android Studio (phiên bản Ladybug hoặc mới hơn khuyến nghị).
* **Minimum SDK:** API 24 (Android 7.0 Nougat).
* **Target SDK:** API 34/35.
* **Java Development Kit (JDK):** jdk-21.0.5.
* **Build System:** Gradle 9.2.1.
* **Ngôn ngữ:** Java.


---

## 📁 Cấu Trúc Thư Mục Dự Án (Cốt Lõi)
Dưới đây là sơ đồ tổ chức thư mục chính:
```text
app/
├── src/
│   ├── main/
│   │   ├── assets/
│   │   │   ├── crop_model_int8.tflite        # Mô hình mặc định (Offline fallback)
│   │   │   └── labels.txt                    # Danh sách nhãn tên bệnh cây trồng
│   │   ├── java/ntu/viet773092/ungDungCdbct_65134318/
│   │   │   ├── data/                         # Cấu hình Room Database
│   │   │   │   ├── Disease.java              # Entity định nghĩa thông tin bệnh
│   │   │   │   └── DiseaseDao.java           # Data Access Object
│   │   │   ├── ml/                           # Xử lý AI và Firebase MLOps
│   │   │   │   ├── TFLiteClassifier.java     # Quản lý luồng Interpreter suy luận
│   │   │   │   └── FirebaseModelManager.java # Tải mô hình OTA từ Firebase
│   │   │   └── ui/                           # Giao diện người dùng
│   │   │       └── MainActivity.java         # Xử lý CameraX và hiển thị BottomSheet
│   │   └── res/layout/
│   │       └── activity_main.xml             # Khung ngắm PreviewView và UI điều khiển
└── build.gradle.kts                          # Cấu hình thư viện Dependencies
