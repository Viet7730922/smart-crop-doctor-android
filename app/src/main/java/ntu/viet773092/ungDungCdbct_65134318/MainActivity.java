package ntu.viet773092.ungDungCdbct_65134318;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.modeldownloader.CustomModel;
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions;
import com.google.firebase.ml.modeldownloader.DownloadType;
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private PreviewView viewFinder;
    private TextView resultTextView;
    private ImageView ivSelectedImage;

    private MaterialButton btnAction;
    private MaterialButton btnPickImage;
    private LinearLayout layoutResultClick;
    private TextView tvClickGuide;

    private boolean isAnalyzing = true;
    private ExecutorService cameraExecutor;

    private TFLiteClassifier tfliteClassifier;
    private boolean isClassifierReady = false;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) startCamera();
                else Toast.makeText(this, "Cần quyền Camera để sử dụng!", Toast.LENGTH_LONG).show();
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                processPickedImage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            FirebaseApp.initializeApp(this);

            viewFinder = findViewById(R.id.viewFinder);
            resultTextView = findViewById(R.id.resultTextView);
            ivSelectedImage = findViewById(R.id.ivSelectedImage);
            btnAction = findViewById(R.id.btnAction);
            btnPickImage = findViewById(R.id.btnPickImage);
            layoutResultClick = findViewById(R.id.layoutResultClick);
            tvClickGuide = findViewById(R.id.tvClickGuide);

            setupButtons();

            cameraExecutor = Executors.newSingleThreadExecutor();

            downloadModelFromFirebase();

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khởi tạo ứng dụng!", Toast.LENGTH_LONG).show();
        }
    }

    private void setupButtons() {
        btnAction.setOnClickListener(v -> {
            isAnalyzing = !isAnalyzing;

            if (isAnalyzing) {
                btnAction.setText("Tạm dừng");
                btnAction.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_dark));
                btnAction.setIconResource(R.drawable.ic_pause);
                ivSelectedImage.setVisibility(View.GONE);
                tvClickGuide.setVisibility(View.GONE);
            } else {
                btnAction.setText("Tiếp tục");
                btnAction.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));
                btnAction.setIconResource(R.drawable.ic_play);
            }
        });

        btnPickImage.setOnClickListener(v -> {
            if (isClassifierReady) {
                pickImageLauncher.launch("image/*");
            } else {
                Toast.makeText(this, "Mô hình AI chưa sẵn sàng!", Toast.LENGTH_SHORT).show();
            }
        });

        layoutResultClick.setOnClickListener(v -> {
            String text = resultTextView.getText().toString();
            if (text.contains("(") && text.contains(")")) {
                try {
                    String diseaseKey = text.split("\\(")[0].trim();
                    android.content.Intent intent = new android.content.Intent(this, DetailActivity.class);
                    intent.putExtra("DISEASE_KEY", diseaseKey);
                    startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void processPickedImage(android.net.Uri uri) {
        isAnalyzing = false;
        btnAction.setText("Tiếp tục quét");
        btnAction.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_blue_dark));
        btnAction.setIconResource(R.drawable.ic_play);
        resultTextView.setText("Đang phân tích ảnh...");
        tvClickGuide.setVisibility(View.GONE);

        InputStream inputStream = null;
        try {
            android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            inputStream = getContentResolver().openInputStream(uri);
            android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            int srcWidth = options.outWidth;
            int srcHeight = options.outHeight;
            int inSampleSize = 1;

            if (srcWidth > 400 || srcHeight > 400) {
                final int halfHeight = srcHeight / 2;
                final int halfWidth = srcWidth / 2;
                while ((halfHeight / inSampleSize) >= 200 && (halfWidth / inSampleSize) >= 200) {
                    inSampleSize *= 2;
                }
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            inputStream = getContentResolver().openInputStream(uri);
            Bitmap sampledBitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            if (sampledBitmap != null) {
                ivSelectedImage.setImageBitmap(sampledBitmap);
                ivSelectedImage.setVisibility(View.VISIBLE);

                Bitmap finalBitmap = Bitmap.createScaledBitmap(sampledBitmap, 200, 200, true);

                cameraExecutor.execute(() -> {
                    try {
                        if (tfliteClassifier != null) {
                            String result = tfliteClassifier.classifyImage(finalBitmap);
                            runOnUiThread(() -> {
                                resultTextView.setText(result);
                                tvClickGuide.setVisibility(View.VISIBLE);
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (finalBitmap != null && !finalBitmap.isRecycled()) {
                            finalBitmap.recycle();
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi xử lý hình ảnh tĩnh!", Toast.LENGTH_SHORT).show();
        } finally {
            if (inputStream != null) {
                try { inputStream.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(200, 200))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        if (isClassifierReady && tfliteClassifier != null && isAnalyzing) {

                            runOnUiThread(() -> {
                                Bitmap bitmap = viewFinder.getBitmap();

                                if (bitmap == null) {
                                    return;
                                }

                                Bitmap scaledCameraBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);
                                Bitmap finalCameraBitmap = scaledCameraBitmap.copy(Bitmap.Config.ARGB_8888, false);

                                if (scaledCameraBitmap != finalCameraBitmap) scaledCameraBitmap.recycle();

                                cameraExecutor.execute(() -> {
                                    try {
                                        final String resultText = tfliteClassifier.classifyImage(finalCameraBitmap);
                                        runOnUiThread(() -> {
                                            resultTextView.setText(resultText);
                                            tvClickGuide.setVisibility(View.VISIBLE);
                                        });
                                        finalCameraBitmap.recycle();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                            });
                        }
                        image.close();
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Lỗi khởi tạo Camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void downloadModelFromFirebase() {
        resultTextView.setText("Đang tải mô hình AI...");

        CustomModelDownloadConditions conditions = new CustomModelDownloadConditions.Builder()
                .requireWifi()
                .build();

        FirebaseModelDownloader.getInstance()
                .getModel("crop_doctor_model", DownloadType.LATEST_MODEL, conditions)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        try {
                            File modelFile = task.getResult().getFile();
                            tfliteClassifier = new TFLiteClassifier(modelFile, this);
                            isClassifierReady = true;
                            runOnUiThread(() -> resultTextView.setText("✅ Mô hình Firebase đã sẵn sàng!"));
                        } catch (Exception e) {
                            e.printStackTrace();
                            loadLocalModelFallback();
                        }
                    } else {
                        loadLocalModelFallback();
                    }
                });
    }

    private void loadLocalModelFallback() {
        try {
            tfliteClassifier = new TFLiteClassifier(this, "model.tflite");
            isClassifierReady = true;
            runOnUiThread(() -> resultTextView.setText("Đang dùng mô hình Offline"));
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> resultTextView.setText("Lỗi mô hình! Kiểm tra file assets"));
        }
    }

    // Hàm đón nhận sự kiện onClick trực tiếp từ tệp tin XML để kích hoạt nhảy trang HistoryActivity
    public void openHistoryActivityFromXml(android.view.View view) {
        startActivity(new android.content.Intent(this, HistoryActivity.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}