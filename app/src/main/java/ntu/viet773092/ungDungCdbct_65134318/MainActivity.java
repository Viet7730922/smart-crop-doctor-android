package ntu.viet773092.ungDungCdbct_65134318;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Size;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.modeldownloader.CustomModel;
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;

import java.io.File;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private TextView resultTextView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // Luồng xử lý riêng cho các tác vụ phân tích hình ảnh của AI (tránh đơ giao diện)
    private ExecutorService cameraExecutor;

    // Bộ quản lý xin quyền Camera hiện đại
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Ứng dụng cần quyền Camera để chẩn đoán bệnh!", Toast.LENGTH_LONG).show();
                    resultTextView.setText("Chưa được cấp quyền Camera");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo FirebaseApp
        FirebaseApp.initializeApp(this);

        // Ánh xạ thành phần giao diện
        viewFinder = findViewById(R.id.viewFinder);
        resultTextView = findViewById(R.id.resultTextView);

        // Khởi tạo Executor chạy ngầm cho Camera/AI
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Kích hoạt tiến trình tải/kiểm tra model AI từ Firebase đám mây
        downloadModelFromFirebase();

        // Kiểm tra và xin quyền Camera
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. Cấu hình luồng ngắm Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 2. Cấu hình luồng Phân tích hình ảnh (Image Analysis)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(640, 480)) // Độ phân giải chuẩn tối ưu cho mô hình di động
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Chỉ giữ lại khung hình mới nhất để xử lý
                        .build();

                // Đăng ký bộ phân tích thời gian thực
                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        // Cứ mỗi khung hình camera quét qua, hệ thống sẽ nhảy vào đây

                        // TODO: Tại đây chúng ta sẽ lấy dữ liệu từ 'image' xử lý bitmap
                        // và đẩy vào hàm nhận diện dự đoán của TensorFlow Lite.

                        // BẮT BUỘC: Phải đóng luồng ảnh cũ để nhường chỗ cho khung hình tiếp theo
                        image.close();
                    }
                });

                // Chọn Camera sau mặc định
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Liên kết đồng thời luồng xem trước và phân tích vào Vòng đời (Lifecycle)
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

                runOnUiThread(() -> resultTextView.setText("Camera và Luồng phân tích đã sẵn sàng"));

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Lỗi khởi tạo Camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void downloadModelFromFirebase() {
        runOnUiThread(() -> resultTextView.setText("Đang kiểm tra mô hình AI từ đám mây..."));

        // Chỉ tải khi máy kết nối mạng Wifi để tránh tốn dung lượng của người dùng
        CustomModelDownloadConditions conditions = new CustomModelDownloadConditions.Builder()
                .requireWifi()
                .build();

        // Tiến hành tải model với tên khớp 100% với tên trên Firebase Console của bạn
        FirebaseModelDownloader.getInstance()
                .getModel("crop_doctor_model", DownloadType.LATEST_MODEL, conditions)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        CustomModel model = task.getResult();
                        File modelFile = model.getFile();

                        if (modelFile != null) {
                            runOnUiThread(() -> resultTextView.setText("Đã nạp mô hình AI từ Firebase!"));
                            // Khởi tạo bộ biên dịch TFLite Interpreter từ 'modelFile' tại đây ở bước kế tiếp
                        }
                    } else {
                        runOnUiThread(() -> resultTextView.setText("Sử dụng mô hình AI mặc định (Offline)"));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Giải phóng luồng Executor khi thoát ứng dụng để tránh rò rỉ bộ nhớ
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}