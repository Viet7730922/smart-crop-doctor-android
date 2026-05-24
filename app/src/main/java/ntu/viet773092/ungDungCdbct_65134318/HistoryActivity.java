package ntu.viet773092.ungDungCdbct_65134318;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TextView tvEmpty;
    private HistoryDatabaseHelper dbHelper;
    private Cursor historyCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        rvHistory = findViewById(R.id.rvHistory);
        tvEmpty = findViewById(R.id.tvEmpty);

        toolbar.setNavigationOnClickListener(v -> finish());
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        dbHelper = new HistoryDatabaseHelper(this);

        loadHistoryData();
    }

    // Nạp mới dữ liệu từ SQLite và cấu hình sự kiện Adapter lắng nghe làm mới danh sách
    private void loadHistoryData() {
        // Đóng con trỏ Cursor cũ nếu đang mở để giải phóng bộ nhớ hệ thống trước khi nạp lại
        if (historyCursor != null && !historyCursor.isClosed()) {
            historyCursor.close();
        }

        historyCursor = dbHelper.getAllHistory();

        if (historyCursor == null || historyCursor.getCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);

            // Khởi tạo bộ nạp dữ liệu adapter kèm tính năng tự làm mới danh sách khi xóa bằng nút bấm
            HistoryAdapter adapter = new HistoryAdapter(this, historyCursor, new HistoryAdapter.OnHistoryItemClickListener() {
                @Override
                public void onItemDeleted() {
                    loadHistoryData();
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