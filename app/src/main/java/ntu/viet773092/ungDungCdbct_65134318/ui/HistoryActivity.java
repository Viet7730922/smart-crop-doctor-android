package ntu.viet773092.ungDungCdbct_65134318.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import ntu.viet773092.ungDungCdbct_65134318.R;
import ntu.viet773092.ungDungCdbct_65134318.adapter.HistoryAdapter;
import ntu.viet773092.ungDungCdbct_65134318.database.HistoryDatabaseHelper;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TextView tvEmpty;
    private TextInputEditText etSearchHistory; //  Biến kiểm soát ô tìm kiếm
    private HistoryDatabaseHelper dbHelper;
    private Cursor historyCursor;
    private String currentSearchQuery = ""; //  Lưu trữ trạng thái từ khóa lọc hiện tại

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        rvHistory = findViewById(R.id.rvHistory);
        tvEmpty = findViewById(R.id.tvEmpty);
        etSearchHistory = findViewById(R.id.etSearchHistory); //  Ánh xạ ô nhập liệu tìm kiếm từ XML

        toolbar.setNavigationOnClickListener(v -> finish());
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new HistoryDatabaseHelper(this);

        // Nạp cấu trúc dữ liệu mặc định ban đầu (chuỗi rỗng - lấy toàn bộ)
        loadHistoryData("");

        //  Lắng nghe sự kiện gõ phím của người dùng để lọc thời gian thực
        etSearchHistory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                loadHistoryData(currentSearchQuery); // Khởi chạy truy vấn lọc động liên tục
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // 🟢 ĐÃ SỬA: Hàm nạp dữ liệu tích hợp cơ chế nhận diện từ khóa tìm kiếm
    private void loadHistoryData(String query) {
        // Đóng con trỏ Cursor cũ nếu đang mở để giải phóng bộ nhớ hệ thống trước khi nạp lại
        if (historyCursor != null && !historyCursor.isClosed()) {
            historyCursor.close();
        }

        // Điều phối hàm gọi từ lớp Database Helper dựa trên từ khóa nhập vào
        if (query.isEmpty()) {
            historyCursor = dbHelper.getAllHistory();
        } else {
            historyCursor = dbHelper.searchHistory(query);
        }

        if (historyCursor == null || historyCursor.getCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);

            // Thay đổi thông báo hiển thị linh hoạt tùy thuộc vào ngữ cảnh tìm kiếm
            if (!query.isEmpty()) {
                tvEmpty.setText("Không tìm thấy kết quả phù hợp");
            } else {
                tvEmpty.setText("Chưa có lịch sử chẩn đoán");
            }
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            // Khởi tạo bộ nạp dữ liệu adapter kèm tính năng tự làm mới danh sách khi xóa bằng nút bấm
            HistoryAdapter adapter = new HistoryAdapter(this, historyCursor, new HistoryAdapter.OnHistoryItemClickListener() {
                @Override
                public void onItemDeleted() {
                    loadHistoryData(currentSearchQuery);
                }
            });
            rvHistory.setAdapter(adapter);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyCursor != null && !historyCursor.isClosed()) {
            historyCursor.close();
        }
    }
}