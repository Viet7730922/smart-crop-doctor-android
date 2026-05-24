package ntu.viet773092.ungDungCdbct_65134318.ui;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import ntu.viet773092.ungDungCdbct_65134318.MainApplication;
import ntu.viet773092.ungDungCdbct_65134318.R;
import ntu.viet773092.ungDungCdbct_65134318.database.HistoryDatabaseHelper;

public class DetailActivity extends AppCompatActivity {

    private TextView tvDiseaseName, tvCause, tvSymptoms, tvTreatment;
    private Button btnBack;
    private ImageView ivDiseaseDetail;
    private androidx.cardview.widget.CardView cardDiseaseImage;
    private Bitmap diseaseBitmapMemory = null;

    private MaterialButton btnSpeak;
    private TextToSpeech textToSpeech;
    private boolean isSpeaking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        tvDiseaseName = findViewById(R.id.tvDiseaseName);
        tvCause = findViewById(R.id.tvCause);
        tvSymptoms = findViewById(R.id.tvSymptoms);
        tvTreatment = findViewById(R.id.tvTreatment);
        btnBack = findViewById(R.id.btnBack);
        ivDiseaseDetail = findViewById(R.id.ivDiseaseDetail);
        cardDiseaseImage = findViewById(R.id.cardDiseaseImage);
        btnSpeak = findViewById(R.id.btnSpeak);

        btnBack.setOnClickListener(v -> finish());

        String diseaseKey = getIntent().getStringExtra("DISEASE_KEY");

        if (diseaseKey != null) {
            loadDiseaseSolution(diseaseKey);
        }

        captureCapturedImageFromMain();
        initTextToSpeech();
    }

    // Khởi tạo và thiết lập định dạng tiếng Việt cho bộ đọc TextToSpeech
    private void initTextToSpeech() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(new Locale("vi", "VN"));
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Thiết bị chưa cài gói dữ liệu tiếng Việt chuẩn!", Toast.LENGTH_SHORT).show();
                } else {
                    btnSpeak.setOnClickListener(v -> {
                        if (isSpeaking) {
                            stopReading();
                        } else {
                            startReadingPhacDo();
                        }
                    });
                }
            } else {
                Toast.makeText(this, "Không thể khởi chạy bộ đọc TextToSpeech!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Lọc sạch văn bản giao diện và ra lệnh phát âm thanh phác đồ điều trị
    private void startReadingPhacDo() {
        if (textToSpeech == null) return;

        String name = tvDiseaseName.getText().toString();
        String cause = tvCause.getText().toString();
        String symptoms = tvSymptoms.getText().toString();
        String treatment = tvTreatment.getText().toString();

        String fullTextToRead = "Chẩn đoán bệnh: " + name
                + ". Nguyên nhân do: " + cause
                + ". Triệu chứng nhận biết là: " + symptoms
                + ". Biện pháp điều trị đặc trị: " + treatment;

        textToSpeech.speak(fullTextToRead, TextToSpeech.QUEUE_FLUSH, null, "CropDoctorSpeakID");

        isSpeaking = true;
        btnSpeak.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.RED));
        btnSpeak.setIconResource(android.R.drawable.ic_media_ff);
    }

    // Ra lệnh ngắt phát âm và khôi phục trạng thái nút bấm về mặc định
    private void stopReading() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
        }
        isSpeaking = false;
        btnSpeak.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1565C0")));
        btnSpeak.setIconResource(android.R.drawable.ic_lock_silent_mode_off);
    }

    // Chặn đứng âm thanh lập tức khi người dùng ẩn ứng dụng hoặc có cuộc gọi đến
    @Override
    protected void onPause() {
        super.onPause();
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
            isSpeaking = false;
            btnSpeak.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1565C0")));
            btnSpeak.setIconResource(android.R.drawable.ic_lock_silent_mode_off);
        }
    }

    // Trích xuất ảnh bệnh phẩm từ luồng hiển thị hoạt động của màn hình chính
    private void captureCapturedImageFromMain() {
        try {
            if (MainApplication.getMainActivityContext() != null) {
                MainActivity mainActivity = MainApplication.getMainActivityContext();
                ImageView mainImageView = mainActivity.findViewById(R.id.ivSelectedImage);
                androidx.camera.view.PreviewView mainPreviewView = mainActivity.findViewById(R.id.viewFinder);

                if (mainImageView != null && mainImageView.getVisibility() == View.VISIBLE) {
                    if (mainImageView.getDrawable() instanceof BitmapDrawable) {
                        Bitmap origin = ((BitmapDrawable) mainImageView.getDrawable()).getBitmap();
                        if (origin != null && !origin.isRecycled()) {
                            diseaseBitmapMemory = origin.copy(origin.getConfig(), false);
                        }
                    }
                } else if (mainPreviewView != null) {
                    Bitmap cameraFrame = mainPreviewView.getBitmap();
                    if (cameraFrame != null) {
                        diseaseBitmapMemory = cameraFrame;
                    }
                }

                if (diseaseBitmapMemory != null) {
                    ivDiseaseDetail.setImageBitmap(diseaseBitmapMemory);
                    cardDiseaseImage.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Đọc dữ liệu JSON phác đồ giải pháp cục bộ và kích hoạt luồng lưu SQLite ngầm
    private void loadDiseaseSolution(String key) {
        try {
            InputStream is = getAssets().open("disease_solutions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            JSONObject jsonObject = new JSONObject(jsonString);

            if (jsonObject.has(key)) {
                JSONObject diseaseInfo = jsonObject.getJSONObject(key);

                String nameVi = diseaseInfo.getString("vietnamese_name");
                String cause = diseaseInfo.getString("cause");
                String symptoms = diseaseInfo.getString("symptoms");
                String treatment = diseaseInfo.getString("treatment");

                tvDiseaseName.setText(nameVi);
                tvCause.setText(cause);
                tvSymptoms.setText(symptoms);
                tvTreatment.setText(treatment);

                new Thread(() -> {
                    try {
                        HistoryDatabaseHelper dbHelper = new HistoryDatabaseHelper(DetailActivity.this);
                        Thread.sleep(500);
                        dbHelper.insertHistory(key, nameVi, diseaseBitmapMemory);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

            } else {
                tvDiseaseName.setText(key);
                tvCause.setText("Chưa có thông tin nguyên nhân trong hệ thống dữ liệu.");
                tvSymptoms.setText("Vui lòng cập nhật triệu chứng cụ thể vào file JSON sau.");
                tvTreatment.setText("Hãy tham khảo ý kiến chuyên gia bảo vệ thực vật tại địa phương.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            tvDiseaseName.setText("Lỗi hệ thống");
            tvCause.setText("Không thể đọc tệp dữ liệu JSON cục bộ.");
        }
    }

    // Hủy hoàn toàn bộ đọc TextToSpeech và giải phóng tài nguyên đồ họa bộ nhớ RAM
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
        try {
            if (ivDiseaseDetail != null) {
                ivDiseaseDetail.setImageDrawable(null);
            }
            if (diseaseBitmapMemory != null && !diseaseBitmapMemory.isRecycled()) {
                diseaseBitmapMemory.recycle();
                diseaseBitmapMemory = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}