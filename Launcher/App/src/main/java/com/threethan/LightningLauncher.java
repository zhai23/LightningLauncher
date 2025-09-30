package com.threethan;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.threethan.launcher.activity.LauncherActivitySearchable;

/** Quest OS displays the activity classpath in the app list.
 *  This activity and package exist for cosmetic reasons only. */
public class LightningLauncher extends LauncherActivitySearchable {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }
    
    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null && "lightninglauncher".equals(uri.getScheme())) {
                Log.d(TAG, "Received custom protocol launch: " + uri.toString());
                // 处理自定义协议启动
                // 这里可以根据uri的host或path执行不同的操作
                String host = uri.getHost();
                if ("open".equals(host)) {
                    // 执行启动操作，例如显示主界面
                    Log.d(TAG, "Lightning Launcher opened via custom protocol");
                }
            }
        }
    }
}
