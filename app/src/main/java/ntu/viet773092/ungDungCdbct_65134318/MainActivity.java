package ntu.viet773092.ungDungCdbct_65134318;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private TextView resultTextView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    // Bộ quản lý xin quyền Camera hiện đại
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Ứng dụng cần quyền Camera để chẩn đoán bệnh!", Toast.LENGTH_LONG).show();
                    resultTextView.setText("Chưa được cấp quyền Camera ❌");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Trỏ đúng vào file layout bạn vừa gửi
        setContentView(R.layout.activity_main);

        // Ánh xạ View từ giao diện
        viewFinder = findViewById(R.id.viewFinder);
        resultTextView = findViewById(R.id.resultTextView);

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

                // Cấu hình khung ngắm Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // Chọn Camera sau mặc định
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Liên kết CameraX với Vòng đời (Lifecycle)
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

                resultTextView.setText("Camera đã sẵn sàng ✅");

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Lỗi khởi tạo Camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
}