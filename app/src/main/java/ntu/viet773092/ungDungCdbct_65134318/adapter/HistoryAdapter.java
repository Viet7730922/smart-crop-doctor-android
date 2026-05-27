package ntu.viet773092.ungDungCdbct_65134318.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import ntu.viet773092.ungDungCdbct_65134318.ui.DetailActivity;
import ntu.viet773092.ungDungCdbct_65134318.database.HistoryDatabaseHelper;
import ntu.viet773092.ungDungCdbct_65134318.R;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final Context context;
    private final Cursor cursor;
    private final OnHistoryItemClickListener itemClickListener;

    // Định nghĩa Interface giao tiếp xử lý hành động xóa bản ghi kết quả
    public interface OnHistoryItemClickListener {
        void onItemDeleted();
    }

    public HistoryAdapter(Context context, Cursor cursor, OnHistoryItemClickListener itemClickListener) {
        this.context = context;
        this.cursor = cursor;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) return;

        final int id = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COLUMN_ID));
        final String key = cursor.getString(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COLUMN_DISEASE_KEY));
        final String nameVi = cursor.getString(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COLUMN_DISEASE_NAME_VI));
        final String timestamp = cursor.getString(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COLUMN_TIMESTAMP));
        byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow(HistoryDatabaseHelper.COLUMN_IMAGE_BYTES));

        holder.tvName.setText(nameVi);
        holder.tvTime.setText(timestamp);

        if (imageBytes != null && imageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            holder.ivImage.setImageBitmap(bitmap);
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Nhấn vào khung thẻ sẽ mở xem lại giải pháp chẩn đoán chi tiết bệnh
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, DetailActivity.class);
            intent.putExtra("DISEASE_KEY", key);
            intent.putExtra("IS_FROM_HISTORY", true);
            intent.putExtra("HISTORY_ID", id);
            context.startActivity(intent);
        });

        holder.btnDelete.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Xóa nhật ký chẩn đoán?")
                    .setMessage("Hành động này sẽ xóa vĩnh viễn phác đồ và hình ảnh bệnh phẩm này khỏi bộ nhớ thiết bị. Bạn có chắc chắn không?")
                    .setIcon(R.drawable.ic_attention)
                    .setCancelable(true)
                    .setPositiveButton("Xóa dữ liệu", (dialog, which) -> {
                        // Khởi tạo luồng xóa SQLite cục bộ
                        HistoryDatabaseHelper dbHelper = new HistoryDatabaseHelper(context);
                        dbHelper.deleteHistoryItem(id);

                        if (itemClickListener != null) {
                            itemClickListener.onItemDeleted();
                        }

                        // Hiển thị thông báo Snackbar xác nhận trạng thái xóa thành công ở đáy màn hình
                        Snackbar.make(holder.itemView, "Đã xóa bản ghi thành công", Snackbar.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy bỏ", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvTime;
        MaterialButton btnDelete;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivHistoryItem);
            tvName = itemView.findViewById(R.id.tvHistoryName);
            tvTime = itemView.findViewById(R.id.tvHistoryTime);
            btnDelete = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }
}