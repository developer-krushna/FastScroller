package modder.hub.fastscroller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.widget.AbsListView;
import android.widget.ListView;

import java.lang.ref.WeakReference;

/*
 Author : @developer-krushna (Krushna Chandra)
 Idea Extracted From MT Manager
 
 A perfect optimized ListView Fast Scroller

*/

public class FastScrollerListView extends ListView {

    private static final int FADE_DELAY_MS = 1500;
    private static final int FADE_DURATION_MS = 300;

    private int trackColor;
    private int thumbColor;
    private float thumbWidth;
    private float thumbHeight;

    private boolean isDragging;
    private boolean isScrolling;
    private boolean isFastScrollEnabled = false;
    private boolean isScrollbarVisible;

    private long fadeStartTime = 0L;
    private RectF thumbRect = new RectF();
    private float dragOffsetY;

    private final ScrollHandler scrollHandler;
    private int lastKnownChildCount = 0;
    private AbsListView.OnScrollListener externalScrollListener;

	private boolean transparentTrackBackground = false; 

    public FastScrollerListView(Context context) {
        this(context, null);
    }

    public FastScrollerListView(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public FastScrollerListView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.scrollHandler = new ScrollHandler(this);

        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);

        super.setOnScrollListener(new InternalScrollListener(this));

        // Handle system fast scroll conflict
        if (super.isFastScrollEnabled()) {
            this.isFastScrollEnabled = true;
        }
        super.setFastScrollEnabled(false);

        if (!isInEditMode()) {
            setSelector(createListSelector(getContext()));
        }

        float density = context.getResources().getDisplayMetrics().density;
        this.thumbColor = 0xDD777777; 
        this.trackColor = 0x39777777;
        this.thumbWidth = 8.0f * density;
        this.thumbHeight = 48.0f * density;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (!isFastScrollEnabled) return;

        float alphaMultiplier = calculateAlphaMultiplier();
        if (alphaMultiplier <= 0.0f) {
            isScrollbarVisible = false;
            return;
        }

        int totalCount = getCount();
        int childCount = getChildCount();

        // Sync child count logic
        if (childCount != lastKnownChildCount) {
            lastKnownChildCount = Math.max(childCount, lastKnownChildCount);
        }

        int scrollableRange = totalCount - childCount;
        if (childCount <= 0 || scrollableRange <= 0) {
            isScrollbarVisible = false;
            return;
        }

        // Only show if list is long enough or we are currently dragging
        if (!isDragging && (totalCount / (float)childCount) <= 4) {
            isScrollbarVisible = false;
            return;
        }

        isScrollbarVisible = true;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int width = getWidth();
        int height = getHeight();

        // Draw Track
        int trackAlpha = (int) (Color.alpha(trackColor) * alphaMultiplier);
		if (!transparentTrackBackground) {
			paint.setColor((trackAlpha << 24) | (trackColor & 0x00FFFFFF));
		} else {
			paint.setColor(Color.TRANSPARENT);
		}
        float trackLeft = width - (thumbWidth * alphaMultiplier);
        canvas.drawRect(trackLeft, 0, width, height, paint);

        // Draw Thumb
        int activeThumbColor = isDragging ? 0xFF1E88E5 : thumbColor;
        int thumbAlpha = (int) (Color.alpha(activeThumbColor) * alphaMultiplier);
        paint.setColor((thumbAlpha << 24) | (activeThumbColor & 0x00FFFFFF));

        float thumbTop = ((height - thumbHeight) / scrollableRange) * getFirstVisiblePosition();
        float thumbBottom = thumbTop + thumbHeight;

        thumbRect.set(width - (thumbWidth * 2.0f), thumbTop, width, thumbBottom);
        canvas.drawRect(trackLeft, thumbTop, width, thumbBottom, paint);
    }

    private float calculateAlphaMultiplier() {
        if (isDragging || isScrolling) return 1.0f;

        long elapsed = SystemClock.uptimeMillis() - fadeStartTime;
        if (elapsed < FADE_DELAY_MS) return 1.0f;

        float fadeProgress = (elapsed - FADE_DELAY_MS) / (float) FADE_DURATION_MS;
        return Math.max(0.0f, 1.0f - fadeProgress);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Only intercept if we are touching the thumb area
        if (isFastScrollEnabled && isScrollbarVisible && ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (thumbRect.contains(ev.getX(), ev.getY())) {
                return true; 
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isFastScrollEnabled || !isScrollbarVisible) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                if (thumbRect.contains(event.getX(), event.getY())) {
                    isDragging = true;
                    dragOffsetY = thumbRect.top - event.getY();
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    float relativeY = (event.getY() + dragOffsetY) / (getHeight() - thumbHeight);
                    relativeY = Math.max(0.0f, Math.min(1.0f, relativeY));

                    int totalCount = getCount();
                    int position = (int) ((totalCount - getChildCount()) * relativeY);
                    setSelection(Math.min(position, totalCount - 1));
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    resetFadeTimer();
                    if (externalScrollListener != null) {
                        externalScrollListener.onScrollStateChanged(this, OnScrollListener.SCROLL_STATE_IDLE);
                    }
                    invalidate();
                    return true;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private void resetFadeTimer() {
        fadeStartTime = SystemClock.uptimeMillis();
        scrollHandler.removeMessages(0);
        scrollHandler.sendEmptyMessageDelayed(0, FADE_DELAY_MS);
    }

    @Override
    public void setFastScrollEnabled(boolean enabled) {
        this.isFastScrollEnabled = enabled;
        invalidate();
    }
	
	public void setTransparentTrackBackground(boolean bg){
		this.transparentTrackBackground = bg;
	}

    @Override
    public boolean isFastScrollEnabled() {
        return isFastScrollEnabled;
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        this.externalScrollListener = l;
    }

    public static Drawable createListSelector(Context context) {
        StateListDrawable stateList = new StateListDrawable();
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, value, true);

        int baseColor = value.data & 0x10ffffff;
        int highlightColor = value.data & 0x40ffffff;

        TransitionDrawable transition = new TransitionDrawable(new Drawable[]{
																   new ColorDrawable(baseColor),
																   new ColorDrawable(highlightColor)
															   });

        // Add states (Simplified)
        int[] pressed = {android.R.attr.state_pressed};
        int[] selected = {android.R.attr.state_selected};

        stateList.addState(pressed, new ColorDrawable(0));
        stateList.addState(selected, transition);
        stateList.addState(new int[]{}, new ColorDrawable(0));
        stateList.setExitFadeDuration(300);

        return stateList;
    }

    // Static inner classes to prevent memory leaks
    private static class ScrollHandler extends Handler {
        private final WeakReference<FastScrollerListView> viewRef;
        private boolean isFading = false;

        ScrollHandler(FastScrollerListView view) {
            viewRef = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            FastScrollerListView view = viewRef.get();
            if (view == null) return;

            if (view.isScrolling) {
                view.resetFadeTimer();
                return;
            }

            long elapsedSinceFadeStart = SystemClock.uptimeMillis() - view.fadeStartTime;
            if (elapsedSinceFadeStart < FADE_DELAY_MS) {
                sendEmptyMessageDelayed(0, FADE_DELAY_MS - elapsedSinceFadeStart);
                isFading = true;
                return;
            }

            if (elapsedSinceFadeStart < FADE_DELAY_MS + FADE_DURATION_MS) {
                isFading = true;
                view.invalidate();
                sendEmptyMessage(0); // Trigger next frame of fade
            } else if (isFading) {
                isFading = false;
                view.invalidate();
            }
        }
    }

    private static class InternalScrollListener implements AbsListView.OnScrollListener {
        private final WeakReference<FastScrollerListView> viewRef;

        InternalScrollListener(FastScrollerListView view) {
            viewRef = new WeakReference<>(view);
        }

        @Override
        public void onScroll(AbsListView view, int first, int visible, int total) {
            FastScrollerListView parent = viewRef.get();
            if (parent != null && parent.externalScrollListener != null) {
                parent.externalScrollListener.onScroll(view, first, visible, total);
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            FastScrollerListView parent = viewRef.get();
            if (parent == null) return;

            if (scrollState != SCROLL_STATE_IDLE) {
                parent.isScrolling = true;
            } else {
                parent.isScrolling = false;
                parent.resetFadeTimer();
            }

            if (parent.externalScrollListener != null) {
                parent.externalScrollListener.onScrollStateChanged(view, scrollState);
            }
        }
    }
}

