package ntu.viet773092.ungDungCdbct_65134318.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "crop_history.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_HISTORY = "history";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DISEASE_KEY = "disease_key";
    public static final String COLUMN_DISEASE_NAME_VI = "disease_name_vi";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_IMAGE_BYTES = "image_bytes";

    public HistoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_HISTORY + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_DISEASE_KEY + " TEXT, "
                + COLUMN_DISEASE_NAME_VI + " TEXT, "
                + COLUMN_TIMESTAMP + " TEXT, "
                + COLUMN_IMAGE_BYTES + " BLOB)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // Hàm chèn lịch sử chẩn đoán mới vào DB
    public void insertHistory(String key, String nameVi, Bitmap originalBitmap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_DISEASE_KEY, key);
        values.put(COLUMN_DISEASE_NAME_VI, nameVi);

        // Tạo thời gian
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());
        values.put(COLUMN_TIMESTAMP, currentDateTime);

        // Xử lý ảnh - GIẢM KÍCH THƯỚC TRƯỚC KHI LƯU
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            Bitmap resizedBitmap = null;
            try {
                // Resize về kích thước nhỏ (thumbnail)
                int targetWidth = 320;
                float ratio = (float) originalBitmap.getHeight() / originalBitmap.getWidth();
                int targetHeight = (int) (targetWidth * ratio);

                resizedBitmap = Bitmap.createScaledBitmap(originalBitmap,
                        targetWidth, targetHeight, true);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                // Dùng JPEG + chất lượng 85% để giảm dung lượng mạnh
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);

                byte[] imageBytes = outputStream.toByteArray();
                values.put(COLUMN_IMAGE_BYTES, imageBytes);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Giải phóng bitmap tạm
                if (resizedBitmap != null && resizedBitmap != originalBitmap) {
                    resizedBitmap.recycle();
                }
            }
        }

        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }

    // Lấy toàn bộ danh sách nhật ký chẩn đoán được sắp xếp từ mới nhất đến cũ nhất
    public android.database.Cursor getAllHistory() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(TABLE_HISTORY, null, null, null, null, null, COLUMN_ID + " DESC");
    }

    public android.database.Cursor searchHistory(String queryText) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = COLUMN_DISEASE_NAME_VI + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + queryText + "%"};

        // Trả về Cursor kết quả và sắp xếp ID giảm dần (mới nhất lên đầu)
        return db.query(TABLE_HISTORY, null, selection, selectionArgs, null, null, COLUMN_ID + " DESC");
    }

    public void deleteHistoryItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}