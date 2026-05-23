package ntu.viet773092.ungDungCdbct_65134318;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;
import android.widget.Button;
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

    // Điều khiển giao diện cho giai đoạn 1
    private Button btnAction;
    private boolean isAnalyzing = true; // true: đang phân tích, false: tạm dừng quét  

    // Luồng xử lý riêng cho tác vụ AI, tránh làm nghẽn giao diện  
    private ExecutorService cameraExecutor;

    // Bộ phân loại TFLite và trạng thái sẵn sàng  
    private TFLiteClassifier tfliteClassifier;
    private boolean isClassifierReady = false;

    // Quản lý cấp quyền Camera theo cơ chế hiện đại  
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

        // Khởi tạo Firebase  
        FirebaseApp.initializeApp(this);

        // Ánh xạ thành phần giao diện  
        viewFinder = findViewById(R.id.viewFinder);
        resultTextView = findViewById(R.id.resultTextView);
        btnAction = findViewById(R.id.btnAction); // Nút điều khiển tạm dừng / tiếp tục  

        // Xử lý sự kiện nút: bấm để tạm dừng hoặc tiếp tục quét  
        btnAction.setOnClickListener(v -> {
            if (isAnalyzing) {
                // Đang quét -> chuyển sang tạm dừng, đóng băng kết quả  
                isAnalyzing = false;
                btnAction.setText("Tiếp tục quét");
                btnAction.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));
            } else {
                // Đang tạm dừng -> kích hoạt quét lại  
                isAnalyzing = true;
                btnAction.setText("Tạm dừng quét");
                btnAction.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
                resultTextView.setText("Đang phân tích...");
            }
        });

        // Khởi tạo luồng nền cho Camera và AI  
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Tải hoặc kiểm tra mô hình AI từ Firebase  
        downloadModelFromFirebase();

        // Kiểm tra và xin quyền Camera nếu chưa được cấp  
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

                // 1. Cấu hình hiển thị preview camera  
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // 2. Cấu hình phân tích hình ảnh (ImageAnalysis)  
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(224, 224)) // Độ phân giải phù hợp cho model phân loại  
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // Chỉ xử lý khung hình mới nhất  
                        .build();

                // Đăng ký analyzer xử lý ảnh theo thời gian thực  
                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        // Chỉ phân tích khi model đã sẵn sàng và trạng thái isAnalyzing = true  
                        if (isClassifierReady && tfliteClassifier != null && isAnalyzing) {

                            // Chuyển sang UI thread để lấy Bitmap từ PreviewView  
                            runOnUiThread(() -> {
                                Bitmap bitmap = viewFinder.getBitmap();

                                if (bitmap != null) {
                                    // Xử lý phân tích ảnh nặng trên luồng nền  
                                    cameraExecutor.execute(() -> {
                                        try {
                                            final String resultText = tfliteClassifier.classifyImage(bitmap);

                                            // Hiển thị kết quả lên giao diện chính  
                                            runOnUiThread(() -> resultTextView.setText(resultText));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                                }
                            });
                        }

                        // Giải phóng frame hiện tại để nhận frame tiếp theo  
                        image.close();
                    }
                });

                // Sử dụng camera sau (máy ảnh chính)  
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Liên kết các use case với vòng đời Activity  
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Lỗi khởi tạo Camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void downloadModelFromFirebase() {
        runOnUiThread(() -> resultTextView.setText("Đang kiểm tra mô hình AI từ đám mây..."));

        // Chỉ tải mô hình khi kết nối WiFi  
        CustomModelDownloadConditions conditions = new CustomModelDownloadConditions.Builder()
                .requireWifi()
                .build();

        // Tải mô hình từ Firebase với tên đã khai báo trong console  
        FirebaseModelDownloader.getInstance()
                .getModel("crop_doctor_model", DownloadType.LATEST_MODEL, conditions)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        CustomModel model = task.getResult();
                        File modelFile = model.getFile();

                        if (modelFile != null) {
                            try {
                                // Khởi tạo bộ phân loại từ file model vừa tải  
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
                        // Dự phòng nếu không tải được từ Firebase  
                        loadLocalModelFallback();
                    }
                });
    }

    // Dự phòng: sử dụng mô hình có sẵn trong thư mục assets  
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
        // Giải phóng Executor để tránh rò rỉ bộ nhớ khi thoát  
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}