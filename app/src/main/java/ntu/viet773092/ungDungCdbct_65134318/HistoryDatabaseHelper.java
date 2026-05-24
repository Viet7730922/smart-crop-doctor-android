package ntu.viet773092.ungDungCdbct_65134318;

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
    public void insertHistory(String key, String nameVi, Bitmap bitmap) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_DISEASE_KEY, key);
        values.put(COLUMN_DISEASE_NAME_VI, nameVi);

        // Tạo thời gian theo định dạng dd/MM/yyyy HH:mm
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        values.put(COLUMN_TIMESTAMP, currentDateTime);

        // Chuyển Bitmap sang byte[] để lưu vào BLOB
        if (bitmap != null && !bitmap.isRecycled()) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                byte[] imageBytes = outputStream.toByteArray();
                values.put(COLUMN_IMAGE_BYTES, imageBytes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }
}