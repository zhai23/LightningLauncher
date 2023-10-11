package com.threethan.launcher.browser;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

// ADAPTED FROM https://gist.github.com/iyashamihsan/1ab5c1cfa47dea735ea46d8943a1bde4
// Replaces d-pad navigation with an on-screen cursor that behaves like a mouse
// This is necessary for web browsing to work, as many sites try to hook the dpad

public class CursorLayout extends LinearLayout {
    private static final float CURSOR_ACCEL = 0.01f;
    private static final float CURSOR_FRICTION = 0.012f;
    private static final float MAX_CURSOR_SPEED = 30.0f;
    private static final float MIN_CURSOR_SPEED = 1.5f;
    private static int CURSOR_RADIUS = 0;
    private static float CURSOR_STROKE_WIDTH = 0f;
    private static int SCROLL_START_PADDING = 100;
    private final Point cursorDirection = new Point(0, 0);
    private final PointF cursorPosition = new PointF(0.0f, 0.0f);
    /* access modifiers changed from: private */
    private final PointF cursorSpeed = new PointF(0.0f, 0.0f);
    private float sizeMult = 0.0f;
    public View targetView;
    private final Runnable cursorUpdateRunnable = new Runnable() {
        public void run() {
            long currentTimeMillis = System.currentTimeMillis();
            long deltaTime = currentTimeMillis - lastCursorUpdate;
            lastCursorUpdate = currentTimeMillis;
            float f = ((float) deltaTime) * CURSOR_ACCEL;
            CursorLayout cursorLayout = CursorLayout.this;
            float xSpeed = cursorSpeed.x;
            float xSpeedBound = cursorLayout.bound(xSpeed + (bound((float) cursorDirection.x, 1.0f) * f), CursorLayout.MAX_CURSOR_SPEED);
            cursorSpeed.set(xSpeedBound, bound(cursorSpeed.y + (bound((float) cursorDirection.y, 1.0f) * f), CursorLayout.MAX_CURSOR_SPEED));
            if (Math.abs(cursorSpeed.x) < 0.1f) cursorSpeed.x = 0.0f;
            else if (Math.abs(cursorSpeed.x) < MIN_CURSOR_SPEED && cursorDirection.x != 0)
                cursorSpeed.x += MIN_CURSOR_SPEED * (cursorDirection.x > 0 ? 1 : -1);

            if (Math.abs(cursorSpeed.y) < 0.1f) cursorSpeed.y = 0.0f;
            else if (Math.abs(cursorSpeed.y) < MIN_CURSOR_SPEED && cursorDirection.y != 0)
                cursorSpeed.y += MIN_CURSOR_SPEED * (cursorDirection.y > 0 ? 1 : -1);
            if (cursorDirection.x == 0)
                cursorSpeed.x -= cursorSpeed.x * Math.min(1f, deltaTime * CURSOR_FRICTION);
            if (cursorDirection.y == 0)
                cursorSpeed.y -= cursorSpeed.y * Math.min(1f, deltaTime * CURSOR_FRICTION);

            if (cursorDirection.x == 0 && cursorDirection.y == 0 && cursorSpeed.x == 0.0f && cursorSpeed.y == 0.0f) {
                if (getHandler() != null) {
                    // Hide cursor after timeout
                    getHandler().postDelayed(CursorLayout.this::visUpdate, 5000);
                }
                return;
            }
            tmpPointF.set(cursorPosition);
            cursorPosition.offset(cursorSpeed.x, cursorSpeed.y);
            if (cursorPosition.x < 0.0f) cursorPosition.x = 0.0f;
            else if (cursorPosition.x > ((float) (getWidth() - 1)))  cursorPosition.x = (float) (getWidth() - 1);
            if (cursorPosition.y < 0.0f) cursorPosition.y = 0.0f;
            else if (cursorPosition.y > ((float) (getHeight() - 1))) cursorPosition.y = (float) (getHeight() - 1);
            if (!tmpPointF.equals(cursorPosition))
                dispatchMotionEvent(cursorPosition.x, cursorPosition.y, MotionEvent.ACTION_DOWN); // Hover

            if (targetView != null) {
                try {
                    if (cursorPosition.y > ((float) (getHeight() - CursorLayout.SCROLL_START_PADDING))) {
                        if (cursorSpeed.y > 0.0f && targetView.canScrollVertically((int) cursorSpeed.y)) {
                            targetView.scrollTo(targetView.getScrollX(), targetView.getScrollY() + ((int) cursorSpeed.y));
                        }
                    } else if (cursorPosition.y < ((float) CursorLayout.SCROLL_START_PADDING) && cursorSpeed.y < 0.0f && targetView.canScrollVertically((int) cursorSpeed.y)) {
                        targetView.scrollTo(targetView.getScrollX(), targetView.getScrollY() + ((int) cursorSpeed.y));
                    }
                    if (cursorPosition.x > ((float) (getWidth() - CursorLayout.SCROLL_START_PADDING))) {
                        if (cursorSpeed.x > 0.0f && targetView.canScrollHorizontally((int) cursorSpeed.x)) {
                            targetView.scrollTo(targetView.getScrollX() + ((int) cursorSpeed.x), targetView.getScrollY());
                        }
                    } else if (cursorPosition.x < ((float) CursorLayout.SCROLL_START_PADDING) && cursorSpeed.x < 0.0f && targetView.canScrollHorizontally((int) cursorSpeed.x)) {
                        targetView.scrollTo(targetView.getScrollX() + ((int) cursorSpeed.x), targetView.getScrollY());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (getHandler() != null) {
                getHandler().post(this);
            }
            visUpdate();
        }
    };
    private void visUpdate() {
        invalidate();

        float cursorVisTarget = isCursorVisible() ? 1.0f : 0.0f;
        if (Math.abs(sizeMult - cursorVisTarget) < 0.1) sizeMult = cursorVisTarget;
        else {
            sizeMult += (cursorVisTarget-sizeMult) * (isCursorVisible() ? 0.02f : 0.2f);
            post(this::visUpdate);
        }
    }

    /* access modifiers changed from: private */
    public long lastCursorUpdate = System.currentTimeMillis() - 5000;
    private final Paint paint = new Paint();
    PointF tmpPointF = new PointF();

    /* access modifiers changed from: private */
    private float bound(float val, float bound) {
        if (val > bound) return bound;
        return Math.max(val, -bound);
    }

    public CursorLayout(Context context) {
        super(context);
        init();
    }

    public CursorLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            this.paint.setAntiAlias(true);
            setWillNotDraw(false);
            Display defaultDisplay = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            Point point = new Point();
            defaultDisplay.getSize(point);
            CURSOR_RADIUS = (int) (point.x / 150f);
            CURSOR_STROKE_WIDTH = CURSOR_RADIUS / 5f;
            SCROLL_START_PADDING = point.x / 15;
        }
    }
    /* access modifiers changed from: protected */
    public void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        super.onSizeChanged(w, h, oldWidth, oldHeight);
        if (!isInEditMode()) {
            this.cursorPosition.set(((float) w) / 2.0f, ((float) oldWidth) / 2.0f);
            if (getHandler() != null) {
                getHandler().postDelayed(this.cursorUpdateRunnable, 5000);
            }
        }
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int keyCode = keyEvent.getKeyCode();
        if (!(keyCode == 66 || keyCode == 160)) {
            switch (keyCode) {
                case 19:
                    if (keyEvent.getAction() == 0) {
                        if (this.cursorPosition.y <= 0.0f) {
                            return super.dispatchKeyEvent(keyEvent);
                        }
                        handleDirectionKeyEvent(keyEvent, -100, -1, true);
                    } else if (keyEvent.getAction() == 1) {
                        handleDirectionKeyEvent(keyEvent, -100, 0, false);
                    }
                    return true;
                case 20:
                    if (keyEvent.getAction() == 0) {
                        if (this.cursorPosition.y >= ((float) getHeight())) {
                            return super.dispatchKeyEvent(keyEvent);
                        }
                        handleDirectionKeyEvent(keyEvent, -100, 1, true);
                    } else if (keyEvent.getAction() == 1) {
                        handleDirectionKeyEvent(keyEvent, -100, 0, false);
                    }
                    return true;
                case 21:
                    if (keyEvent.getAction() == 0) {
                        if (this.cursorPosition.x <= 0.0f) {
                            return super.dispatchKeyEvent(keyEvent);
                        }
                        handleDirectionKeyEvent(keyEvent, -1, -100, true);
                    } else if (keyEvent.getAction() == 1) {
                        handleDirectionKeyEvent(keyEvent, 0, -100, false);
                    }
                    return true;
                case 22:
                    if (keyEvent.getAction() == 0) {
                        if (this.cursorPosition.x >= ((float) getWidth())) {
                            return super.dispatchKeyEvent(keyEvent);
                        }
                        handleDirectionKeyEvent(keyEvent, 1, -100, true);
                    } else if (keyEvent.getAction() == 1) {
                        handleDirectionKeyEvent(keyEvent, 0, -100, false);
                    }
                    return true;
                case 23:
                    break;
                default:
                    switch (keyCode) {
                        case 268:
                            if (keyEvent.getAction() == 0) {
                                handleDirectionKeyEvent(keyEvent, -1, -1, true);
                            } else if (keyEvent.getAction() == 1) {
                                handleDirectionKeyEvent(keyEvent, 0, 0, false);
                            }
                            return true;
                        case 269:
                            if (keyEvent.getAction() == 0) {
                                handleDirectionKeyEvent(keyEvent, -1, 1, true);
                            } else if (keyEvent.getAction() == 1) {
                                handleDirectionKeyEvent(keyEvent, 0, 0, false);
                            }
                            return true;
                        case 270:
                            if (keyEvent.getAction() == 0) {
                                handleDirectionKeyEvent(keyEvent, 1, -1, true);
                            } else if (keyEvent.getAction() == 1) {
                                handleDirectionKeyEvent(keyEvent, 0, 0, false);
                            }
                            return true;
                        case 271:
                            if (keyEvent.getAction() == 0) {
                                handleDirectionKeyEvent(keyEvent, 1, 1, true);
                            } else if (keyEvent.getAction() == 1) {
                                handleDirectionKeyEvent(keyEvent, 0, 0, false);
                            }
                            return true;
                    }
            }
        }
        if (isCursorVisible()) {
            // Click animation
            if (keyEvent.getAction() == 0 && !getKeyDispatcherState().isTracking(keyEvent)) {
                getKeyDispatcherState().startTracking(keyEvent, this);
                dispatchMotionEvent(this.cursorPosition.x, this.cursorPosition.y, 0);

                ValueAnimator viewAnimator = ValueAnimator.ofFloat(sizeMult, 0.7f);
                viewAnimator.setDuration(250);
                viewAnimator.setInterpolator(new OvershootInterpolator());
                viewAnimator.addUpdateListener(animation -> {
                    sizeMult = (float) animation.getAnimatedValue();
                    this.lastCursorUpdate = System.currentTimeMillis();
                    invalidate();
                });
                viewAnimator.start();
                postDelayed(() -> {
                    ValueAnimator viewAnimator2 = ValueAnimator.ofFloat(sizeMult, 1.0f);
                    viewAnimator2.setDuration(250);
                    viewAnimator2.setInterpolator(new OvershootInterpolator());
                    viewAnimator2.addUpdateListener(animation -> {
                        sizeMult = (float) animation.getAnimatedValue();
                        this.lastCursorUpdate = System.currentTimeMillis();
                        invalidate();
                    });
                    viewAnimator2.start();
                }, 250);

            } else if (keyEvent.getAction() == 1) {
                getKeyDispatcherState().handleUpEvent(keyEvent);
                dispatchMotionEvent(this.cursorPosition.x, this.cursorPosition.y, 1);
            }
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    /* access modifiers changed from: private */
    public void dispatchMotionEvent(float x, float y, int i) {
        long uptimeMillis = SystemClock.uptimeMillis();
        PointerProperties pointerProperties = new PointerProperties();
        pointerProperties.id = 0;
        pointerProperties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        PointerProperties[] pointerPropertiesArr = {pointerProperties};
        PointerCoords pointerCoords = new PointerCoords();
        pointerCoords.x = x;
        pointerCoords.y = y;
        pointerCoords.pressure = 1.0f;
        pointerCoords.size = 1.0f;
        dispatchTouchEvent(
                MotionEvent.obtain(uptimeMillis, uptimeMillis, i, 1, pointerPropertiesArr, new PointerCoords[]{pointerCoords}, 0, 0, 1.0f, 1.0f, 0, 0, 0, 0));
    }

    private void handleDirectionKeyEvent(KeyEvent keyEvent, int i, int i2, boolean hasInput) {
        this.lastCursorUpdate = System.currentTimeMillis();
        if (!hasInput) {
            cursorDirection.x = 0;
            cursorDirection.y = 0;
        }

        Handler handler = getHandler();
        handler.removeCallbacks(this.cursorUpdateRunnable);
        handler.post(this.cursorUpdateRunnable);

        try {
            getKeyDispatcherState().startTracking(keyEvent, this);
        } catch (Exception ignored) {}

        Point point = this.cursorDirection;
        if (i == -100) i = point.x;
        if (i2 == -100) i2 = this.cursorDirection.y;
        point.set(i, i2);
    }

    /* access modifiers changed from: protected */
    public void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);

        if (!isInEditMode()) {

            float x = this.cursorPosition.x;
            float y = this.cursorPosition.y;
            // Shadow
            this.paint.setColor(Color.argb(10, 0, 0, 0));
            this.paint.setStyle(Style.FILL);
            canvas.drawCircle(x, y+CURSOR_STROKE_WIDTH, (float) CURSOR_RADIUS * sizeMult, this.paint);
            this.paint.setColor(Color.argb(10, 0, 0, 0));
            this.paint.setStyle(Style.FILL);
            canvas.drawCircle(x, y+CURSOR_STROKE_WIDTH, (float) CURSOR_RADIUS * sizeMult + CURSOR_STROKE_WIDTH, this.paint);
            // Cursor
            this.paint.setColor(Color.argb(128, 255, 255, 255));
            this.paint.setStyle(Style.FILL);
            canvas.drawCircle(x, y, (float) CURSOR_RADIUS * sizeMult, this.paint);
            // Outline
            this.paint.setColor(Color.argb(200, 150, 150, 150));
            this.paint.setStrokeWidth(CURSOR_STROKE_WIDTH);
            this.paint.setStyle(Style.STROKE);
            canvas.drawCircle(x, y, (float) CURSOR_RADIUS * sizeMult, this.paint);
        }

    }

    private boolean isCursorVisible() {
        return System.currentTimeMillis() - this.lastCursorUpdate <= 4000;
    }

    /* access modifiers changed from: protected */
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
    }
}