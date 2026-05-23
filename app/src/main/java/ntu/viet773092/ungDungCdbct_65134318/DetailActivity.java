package ntu.viet773092.ungDungCdbct_65134318;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DetailActivity extends AppCompatActivity {

    private TextView tvDiseaseName, tvCause, tvSymptoms, tvTreatment;
    private Button btnBack;

    // Khai báo thêm thành phần hiển thị ảnh chi tiết bệnh phẩm
    private ImageView ivDiseaseDetail;
    private androidx.cardview.widget.CardView cardDiseaseImage;
    private Bitmap diseaseBitmapMemory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        tvDiseaseName = findViewById(R.id.tvDiseaseName);
        tvCause = findViewById(R.id.tvCause);
        tvSymptoms = findViewById(R.id.tvSymptoms);
        tvTreatment = findViewById(R.id.tvTreatment);
        btnBack = findViewById(R.id.btnBack);

        // Ánh xạ các thành phần giao diện hình ảnh mới thêm
        ivDiseaseDetail = findViewById(R.id.ivDiseaseDetail);
        cardDiseaseImage = findViewById(R.id.cardDiseaseImage);

        btnBack.setOnClickListener(v -> finish());

        String diseaseKey = getIntent().getStringExtra("DISEASE_KEY");

        if (diseaseKey != null) {
            loadDiseaseSolution(diseaseKey);
        }

        // TIẾN TRÌNH TRÍCH XUẤT NGƯỢC ẢNH TỪ MAINACTIVITY KHÔNG ĐỔI CODE TRANG MAIN
        captureCapturedImageFromMain();
    }

    // Thuật toán tự động định vị và bóc tách dữ liệu ảnh từ View của MainActivity
    private void captureCapturedImageFromMain() {
        try {
            // Kiểm tra ngầm xem MainActivity có đang hoạt động trên hệ thống hay không
            if (MainApplication.getMainActivityContext() != null) {
                MainActivity mainActivity = MainApplication.getMainActivityContext();
                ImageView mainImageView = mainActivity.findViewById(R.id.ivSelectedImage);
                androidx.camera.view.PreviewView mainPreviewView = mainActivity.findViewById(R.id.viewFinder);

                // Trường hợp 1: Nếu ImageView đang hiện nghĩa là người dùng đang xem kết quả từ ảnh tĩnh thư viện
                if (mainImageView != null && mainImageView.getVisibility() == View.VISIBLE) {
                    if (mainImageView.getDrawable() instanceof BitmapDrawable) {
                        Bitmap origin = ((BitmapDrawable) mainImageView.getDrawable()).getBitmap();
                        if (origin != null && !origin.isRecycled()) {
                            diseaseBitmapMemory = origin.copy(origin.getConfig(), false);
                        }
                    }
                }
                // Trường hợp 2: Nếu không thì trích xuất trực tiếp khung hình đóng băng hiện tại của kính ngắm camera trực tiếp
                else if (mainPreviewView != null) {
                    Bitmap cameraFrame = mainPreviewView.getBitmap();
                    if (cameraFrame != null) {
                        diseaseBitmapMemory = cameraFrame;
                    }
                }

                // Nếu thu giữ ma trận điểm ảnh thành công, tiến hành gắn lên giao diện trang chi tiết
                if (diseaseBitmapMemory != null) {
                    ivDiseaseDetail.setImageBitmap(diseaseBitmapMemory);
                    cardDiseaseImage.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    // GIẢI PHÓNG TOÀN BỘ VÙNG NHỚ ĐỆM HÌNH ẢNH KHI THOÁT KHỎI TRANG DETAIL
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Xóa sạch liên kết ảnh trong ImageView để tránh rò rỉ bộ nhớ đồ họa
            if (ivDiseaseDetail != null) {
                ivDiseaseDetail.setImageDrawable(null);
            }
            // Gọi lệnh xóa sổ pixel của Bitmap bộ nhớ tạm ra khỏi thanh RAM ngay lập tức
            if (diseaseBitmapMemory != null && !diseaseBitmapMemory.isRecycled()) {
                diseaseBitmapMemory.recycle();
                diseaseBitmapMemory = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}