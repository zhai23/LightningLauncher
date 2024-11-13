package com.threethan.launchercore.lib;

import android.os.Handler;
import android.os.Looper;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

/** @noinspection unused*/
public abstract class DelayLib {
    public static void delayed(Runnable runnable) {
        delayed(runnable, 750);
    }
    public static void delayed(Runnable runnable, int delayMs) {
        new Timer().schedule(new TimerTask() {
        @Override
        public void run() {
            runnable.run();
        }
    }, delayMs);
    }

    public static void repeatUntil(Runnable runnable, Supplier<Boolean> until, int intervalMs) {
        if (until.get()) return;
        // Start updating system config
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable[] r = new Runnable[1];
        r[0] = () -> {
            if (until.get()) return;
            runnable.run();
            handler.postDelayed(r[0], intervalMs);
        };
        r[0].run();
    }
}
