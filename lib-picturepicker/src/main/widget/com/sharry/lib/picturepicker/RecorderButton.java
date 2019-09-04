package com.sharry.lib.picturepicker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * 相机拍摄录制的按钮
 * <p>
 * Please ensure u Activity implement {@link Interaction} correct when u use this widget.
 *
 * @author Sharry <a href="xiaoyu.zhu@1hai.cn">Contact me.</a>
 * @version 1.0
 * @since 2019-09-04 09:15
 */
public class RecorderButton extends View implements View.OnTouchListener, View.OnClickListener {

    private static final int MSG_WHAT_CALL_RECORD_START = 848;

    /**
     * 用于绘制的相关属性
     */

    private final Paint mPaint;
    private final RectF mRect = new RectF();
    private final Point mCenterPoint = new Point();
    private int[] mInnerRadiusRange = new int[2];
    private int[] mOuterRadiusRange = new int[2];
    private int mCurInnerRadius;
    private int mCurOuterRadius;
    private long mMaxDuration = 100;
    private long mCurDuration = 0;

    private AnimatorSet mStartAnimSet;
    private AnimatorSet mFinishAnimSet;
    private int mProgressColor;

    private boolean mIsLongClickEnable = false;
    private boolean mIsRecording = false;

    /**
     * 用于和外界交互
     */
    private Interaction mInteraction;

    /**
     * 处理录制开始的回调
     */
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_WHAT_CALL_RECORD_START) {
                mCurDuration = 0;
                mInteraction.onRecordStart();
            }
        }
    };

    public RecorderButton(Context context) {
        this(context, null);
    }

    public RecorderButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecorderButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (context instanceof Interaction) {
            mInteraction = (Interaction) context;
        } else {
            throw new UnsupportedOperationException("Please ensure u activity implements RecorderButton.Interaction");
        }
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mProgressColor = ContextCompat.getColor(context, R.color.picture_picker_base_primary_color);
        setOnClickListener(this);
        setOnTouchListener(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mIsLongClickEnable) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    handleRecordStart();
                    break;
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    handleRecordFinish();
                    break;
                default:
                    break;
            }
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        mInteraction.onTakePicture();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int validWidth = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
        int validHeight = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        int validSize = Math.min(validWidth, validHeight);
        // 计算中心位置
        mCenterPoint.x = getPaddingLeft() + validWidth >> 1;
        mCenterPoint.y = getPaddingTop() + validHeight >> 1;
        // 计算外环半径范围
        mOuterRadiusRange[1] = validSize >> 1;
        mOuterRadiusRange[0] = mOuterRadiusRange[1] * 3 / 4;
        // 计算内环半径范围
        mInnerRadiusRange[1] = mOuterRadiusRange[0] * 3 / 4;
        mInnerRadiusRange[0] = mOuterRadiusRange[1] / 3;
        // 初始化区域
        mCurInnerRadius = mInnerRadiusRange[1];
        mCurOuterRadius = mOuterRadiusRange[0];
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制外部圆环
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(ContextCompat.getColor(getContext(), R.color.picture_picker_widget_record_progress_outer_corner_gray));
        canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, mCurOuterRadius, mPaint);
        // 绘制内部圆环
        mPaint.setColor(Color.WHITE);
        canvas.drawCircle(mCenterPoint.x, mCenterPoint.y, mCurInnerRadius, mPaint);
        // 绘制进度
        int strokeWidth = mInnerRadiusRange[0] >> 2;
        int halfOfStrokeWidth = strokeWidth >> 1;
        // 确定进度的范围
        mRect.top = mCenterPoint.y - mCurOuterRadius + halfOfStrokeWidth;
        mRect.left = mCenterPoint.x - mCurOuterRadius + halfOfStrokeWidth;
        mRect.right = mRect.left + (mCurOuterRadius << 1) - strokeWidth;
        mRect.bottom = mRect.top + (mCurOuterRadius << 1) - strokeWidth;
        // 配置画笔
        mPaint.setStrokeWidth(strokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(mProgressColor);
        canvas.drawArc(mRect, -90, (mCurDuration * 360f / mMaxDuration), false, mPaint);
    }

    /**
     * 设置进度条的颜色
     */
    public void setProgressColor(@ColorInt int color) {
        mProgressColor = color;
    }

    /**
     * 是否支持长按
     */
    public void setLongClickEnable(boolean isLongClickEnable) {
        mIsLongClickEnable = isLongClickEnable;
    }

    /**
     * 设置录制的最大时长
     */
    public void setMaxProgress(long maxDuration) {
        this.mMaxDuration = maxDuration;
    }

    /**
     * 设置当前录制的时长
     */
    public void setCurrentProgress(long curDuration) {
        // 若处于未录制状态, 则无需影响进度更新
        if (!mIsRecording) {
            return;
        }
        // 说明此时正在处理结束的动作, 无效再响应进度更新了
        if (mFinishAnimSet != null && mFinishAnimSet.isStarted()) {
            return;
        }
        if (curDuration <= mCurDuration) {
            return;
        }
        this.mCurDuration = curDuration;
        if (mCurDuration >= mMaxDuration) {
            // 停止点击事件的响应
            setEnabled(false);
            // 处理录制结束
            handleRecordFinish();
        } else {
            invalidate();
        }
    }

    /**
     * 按下的动画
     */
    private void handleRecordStart() {
        if (mStartAnimSet == null) {
            // 内圆缩小
            ValueAnimator innerAnimator = ObjectAnimator.ofInt(mCurInnerRadius, mInnerRadiusRange[0])
                    .setDuration(200);
            innerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurInnerRadius = (int) animation.getAnimatedValue();
                    invalidate();
                }
            });
            // 外圆放大
            ValueAnimator outerAnimator = ObjectAnimator.ofInt(mCurOuterRadius, mOuterRadiusRange[1])
                    .setDuration(200);
            outerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurOuterRadius = (int) animation.getAnimatedValue();
                }
            });
            // 执行动画集合
            mStartAnimSet = new AnimatorSet();
            mStartAnimSet.playTogether(innerAnimator, outerAnimator);
            mStartAnimSet.addListener(new AnimatorListenerAdapter() {

                @Override
                public void onAnimationStart(Animator animation) {
                    mIsRecording = true;
                    // 1s 之后回调外界开始录制, 1s 之内视为拍照
                    mHandler.sendMessageDelayed(
                            Message.obtain(mHandler, MSG_WHAT_CALL_RECORD_START),
                            1000
                    );
                }

            });
        }
        if (mStartAnimSet.isStarted()) {
            return;
        }
        mStartAnimSet.start();
    }

    /**
     * 录制结束的动画
     */
    private void handleRecordFinish() {
        mIsRecording = false;
        if (mFinishAnimSet == null) {
            // 内圆放大
            ValueAnimator innerAnimator = ObjectAnimator.ofInt(mCurInnerRadius, mInnerRadiusRange[1]).setDuration(200);
            innerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurInnerRadius = (int) animation.getAnimatedValue();
                    invalidate();
                }
            });
            // 外圆缩小
            ValueAnimator outerAnimator = ObjectAnimator.ofInt(mCurOuterRadius, mOuterRadiusRange[0]).setDuration(200);
            outerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurOuterRadius = (int) animation.getAnimatedValue();
                }
            });
            // 执行动画集合
            mFinishAnimSet = new AnimatorSet();
            mFinishAnimSet.playTogether(innerAnimator, outerAnimator);
            mFinishAnimSet.addListener(new AnimatorListenerAdapter() {

                boolean isTakePicture = false;

                @Override
                public void onAnimationStart(Animator animation) {
                    // 尝试移除录制开始的消息, 若移除成功则说明录制尚未启动, 则触发拍照
                    isTakePicture = mHandler.hasMessages(MSG_WHAT_CALL_RECORD_START);
                    if (isTakePicture) {
                        mHandler.removeMessages(MSG_WHAT_CALL_RECORD_START);
                    }
                    // 重置为 0
                    mCurDuration = 0;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (isTakePicture) {
                        mInteraction.onTakePicture();
                    } else {
                        mInteraction.onRecordFinish(mCurDuration);
                    }
                    // 重新响应触摸事件
                    setEnabled(true);
                }
            });
        }
        if (mFinishAnimSet.isStarted()) {
            return;
        }
        // 取消开始动画
        mStartAnimSet.cancel();
        // 启动结束动画
        mFinishAnimSet.start();
    }

    public interface Interaction {

        /**
         * Take a picture
         */
        void onTakePicture();

        /**
         * Record video.
         */
        void onRecordStart();

        /**
         * Record complete.
         *
         * @param duration total duration.
         */
        void onRecordFinish(long duration);
    }

}
