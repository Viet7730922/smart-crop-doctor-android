package ntu.viet773092.ungDungCdbct_65134318;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class DetailActivity extends AppCompatActivity {

    private TextView tvDiseaseName, tvCause, tvSymptoms, tvTreatment;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Ánh xạ giao diện
        tvDiseaseName = findViewById(R.id.tvDiseaseName);
        tvCause = findViewById(R.id.tvCause);
        tvSymptoms = findViewById(R.id.tvSymptoms);
        tvTreatment = findViewById(R.id.tvTreatment);
        btnBack = findViewById(R.id.btnBack);

        // Bấm nút quay lại để đóng màn hình chi tiết
        btnBack.setOnClickListener(v -> finish());

        // Nhận từ khóa tên bệnh từ MainActivity truyền sang
        String diseaseKey = getIntent().getStringExtra("DISEASE_KEY");

        if (diseaseKey != null) {
            loadDiseaseSolution(diseaseKey);
        }
    }

    private void loadDiseaseSolution(String key) {
        try {
            // 1. Đọc tệp JSON từ thư mục assets vào chuỗi String
            InputStream is = getAssets().open("disease_solutions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            // 2. Phân tích chuỗi thành đối tượng JSON cục bộ
            JSONObject jsonObject = new JSONObject(jsonString);

            // 3. Kiểm tra xem mã khóa bệnh có tồn tại trong file JSON hay không
            if (jsonObject.has(key)) {
                JSONObject diseaseInfo = jsonObject.getJSONObject(key);

                // Trích xuất các trường dữ liệu
                String nameVi = diseaseInfo.getString("vietnamese_name");
                String cause = diseaseInfo.getString("cause");
                String symptoms = diseaseInfo.getString("symptoms");
                String treatment = diseaseInfo.getString("treatment");

                // Đổ dữ liệu mượt mà lên giao diện người dùng
                tvDiseaseName.setText(nameVi);
                tvCause.setText(cause);
                tvSymptoms.setText(symptoms);
                tvTreatment.setText(treatment);
            } else {
                // Nếu mô hình AI trả về nhãn lạ chưa cập nhật trong file JSON
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
}