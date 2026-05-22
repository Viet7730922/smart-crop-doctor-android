package ntu.viet773092.ungDungCdbct_65134318;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private TextView resultTextView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // Luồng xử lý riêng cho các tác vụ phân tích hình ảnh của AI (tránh đơ giao diện)
    private ExecutorService cameraExecutor;

    // Bộ phân loại TFLite và trạng thái sẵn sàng[cite: 6]
    private TFLiteClassifier tfliteClassifier;
    private boolean isClassifierReady = false;

    // Bộ quản lý xin quyền Camera hiện đại[cite: 6]
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

        // Khởi tạo FirebaseApp[cite: 6]
        FirebaseApp.initializeApp(this);

        // Ánh xạ thành phần giao diện[cite: 6]
        viewFinder = findViewById(R.id.viewFinder);
        resultTextView = findViewById(R.id.resultTextView);

        // Khởi tạo Executor chạy ngầm cho Camera/AI[cite: 6]
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Kích hoạt tiến trình tải/kiểm tra model AI từ Firebase đám mây[cite: 6]
        downloadModelFromFirebase();

        // Kiểm tra và xin quyền Camera[cite: 6]
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

                // 1. Cấu hình luồng ngắm Preview[cite: 6]
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 2. Cấu hình luồng Phân tích hình ảnh (Image Analysis)[cite: 6]
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(224, 224)) // Ép về 224x224 tối ưu cho mô hình phân loại[cite: 6]
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Chỉ giữ lại khung hình mới nhất[cite: 6]
                        .build();

                // Đăng ký bộ phân tích thời gian thực[cite: 6]
                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        // Chỉ chạy phân tích khi bộ phân loại AI đã được khởi tạo xong[cite: 6]
                        if (isClassifierReady && tfliteClassifier != null) {

                            // Ép tác vụ lấy Bitmap từ View giao diện về luồng chính (Main Thread)
                            runOnUiThread(() -> {
                                Bitmap bitmap = viewFinder.getBitmap();

                                if (bitmap != null) {
                                    // Đẩy tác vụ phân tích mô hình nặng ngược lại luồng ngầm để chạy độc lập
                                    cameraExecutor.execute(() -> {
                                        try {
                                            final String resultText = tfliteClassifier.classifyImage(bitmap);

                                            // Trả kết quả hiển thị văn bản về lại giao diện chính
                                            runOnUiThread(() -> resultTextView.setText(resultText));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                }
                            });
                        }

                        // BẮT BUỘC: Phải đóng luồng ảnh cũ để nhường chỗ cho khung hình tiếp theo[cite: 6]
                        image.close();
                    }
                });

                // Chọn Camera sau mặc định[cite: 6]
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Liên kết đồng thời luồng xem trước và phân tích vào Vòng đời (Lifecycle)[cite: 6]
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Lỗi khởi tạo Camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void downloadModelFromFirebase() {
        runOnUiThread(() -> resultTextView.setText("Đang kiểm tra mô hình AI từ đám mây..."));

        // Chỉ tải khi máy kết nối mạng Wifi để tránh tốn dung lượng của người dùng[cite: 6]
        CustomModelDownloadConditions conditions = new CustomModelDownloadConditions.Builder()
                .requireWifi()
                .build();

        // Tiến hành tải model với tên khớp 100% với tên trên Firebase Console[cite: 6]
        FirebaseModelDownloader.getInstance()
                .getModel("crop_doctor_model", DownloadType.LATEST_MODEL, conditions)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        CustomModel model = task.getResult();
                        File modelFile = model.getFile();

                        if (modelFile != null) {
                            try {
                                // Khởi tạo bộ biên dịch TFLite từ file tải về thành công[cite: 6]
                                tfliteClassifier = new TFLiteClassifier(modelFile, MainActivity.this);
                                isClassifierReady = true;
                                runOnUiThread(() -> resultTextView.setText("Đã nạp mô hình AI từ Firebase!"));
                            } catch (IOException e) {
                                e.printStackTrace();
                                loadLocalModelFallback();
                            }
                        } else {
                            loadLocalModelFallback();
                        }
                    } else {
                        // Nhánh rẽ dự phòng nếu không tải được từ Firebase[cite: 6]
                        loadLocalModelFallback();
                    }
                });
    }

    // Hàm khởi tạo mô hình dự phòng nằm trong thư mục assets để chạy offline[cite: 6]
    private void loadLocalModelFallback() {
        try {
            tfliteClassifier = new TFLiteClassifier(MainActivity.this, "model.tflite");
            isClassifierReady = true;
            runOnUiThread(() -> resultTextView.setText("Sử dụng mô hình AI mặc định (Offline)"));
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> resultTextView.setText("Lỗi nạp mô hình AI cục bộ!"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Giải phóng luồng Executor khi thoát ứng dụng để tránh rò rỉ bộ nhớ[cite: 6]
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}