package ntu.viet773092.ungDungCdbct_65134318;

import android.app.Application;
import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// Lớp ứng dụng tự động theo dõi và cung cấp vị trí bộ nhớ của MainActivity mà không can thiệp vào mã nguồn của nó
public class MainApplication extends Application {

    private static MainActivity mainActivityContext = null;

    @Override
    public void onCreate() {
        super.onCreate();

        // Đăng ký bộ giám sát vòng đời ứng dụng tự động
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity instanceof MainActivity) {
                    mainActivityContext = (MainActivity) activity;
                }
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (activity instanceof MainActivity) {
                    mainActivityContext = null;
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {}
            @Override
            public void onActivityResumed(@NonNull Activity activity) {}
            @Override
            public void onActivityPaused(@NonNull Activity activity) {}
            @Override
            public void onActivityStopped(@NonNull Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
        });
    }

    // Hàm tĩnh giúp DetailActivity gọi lấy ngữ cảnh của trang Main bất cứ lúc nào
    public static MainActivity getMainActivityContext() {
        return mainActivityContext;
    }
}