package ntu.viet773092.ungDungCdbct_65134318.classifier;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class TFLiteClassifier {
    private Interpreter interpreter;
    private List<String> labelList;

    private final int INPUT_SIZE = 200; // Khớp chuẩn xác kích thước mô hình 200x200[cite: 5]
    private final int PIXEL_SIZE = 3;

    public TFLiteClassifier(File modelFile, Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        this.interpreter = new Interpreter(modelFile, options);
        this.labelList = loadLabelList(context);
    }

    public TFLiteClassifier(Context context, String modelPath) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        this.interpreter = new Interpreter(loadModelFile(context, modelPath), options);
        this.labelList = loadLabelList(context);
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(Context context) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getAssets().open("labels.txt")));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    public String classifyImage(Bitmap bitmap) {
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);
        float[][] result = new float[1][labelList.size()];

        interpreter.run(byteBuffer, result);

        int maxIndex = 0;
        float maxConfidence = -1.0f;
        for (int i = 0; i < result[0].length; i++) {
            if (result[0][i] > maxConfidence) {
                maxConfidence = result[0][i];
                maxIndex = i;
            }
        }

        return labelList.get(maxIndex) + " (" + String.format("%.1f", maxConfidence * 100) + "%)";
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < INPUT_SIZE; ++i) {
            for (int j = 0; j < INPUT_SIZE; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat((((val >> 16) & 0xFF)) / 255.0f);
                byteBuffer.putFloat((((val >> 8) & 0xFF)) / 255.0f);
                byteBuffer.putFloat(((val & 0xFF)) / 255.0f);
            }
        }
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle();
        }
        return byteBuffer;
    }
}