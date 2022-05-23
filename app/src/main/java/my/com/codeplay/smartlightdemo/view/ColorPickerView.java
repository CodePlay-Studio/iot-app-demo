package my.com.codeplay.smartlightdemo.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import my.com.codeplay.smartlightdemo.R;

/**
 * Created by Tham on 3/26/16.
 */
public class ColorPickerView extends View {
    public interface OnColorChangedListener {
        void onColorChanged(int color);
    }
    private OnColorChangedListener mOnColorChangedListener;

    private static final String TAG = ColorPickerView.class.getSimpleName();
    private static final float PI = 3.1415926f;
    private final int[] COLOR_SET = new int[] {
            0xFFFF0000, 0xFFFF00FF, 0xFF0000FF, 0xFF00FFFF, 0xFF00FF00, 0xFFFFFF00, 0xFFFF0000
    };
    private Paint mPaint;
    private Paint mCenterPaint;
    private int mCenterX;
    private int mCenterY;
    private int mRadius;
    private int mStrokeWidth;
    private boolean mPickedColorInCenter;
    private boolean mHighlightCenter;
    private int mColorPicked;

    /**
     * The constructor that are called when the view is created from code.
     *
     * @param context the context of an Activity
     * @param showPickedColorInCenter set true to show picked color in the center
     * @param color the picked color
     * @param mCenterPaintStrokeWidth set the stroke width
     * @param listener an optional listener to listen for picked color change
     */
    public ColorPickerView(Context context, boolean showPickedColorInCenter, int color, int mCenterPaintStrokeWidth, OnColorChangedListener listener) {
        super(context);

        init(showPickedColorInCenter, mCenterPaintStrokeWidth, getResources().getColor(color), listener);
    }

    /**
     * The constructors that are called when the view is inflated from a layout file which
     * should parse and apply any attributes defined in the layout file.
     *
     * @param context the context of an Activity
     * @param attrs the attributes to be inherited from a theme
     */
    public ColorPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ColorPickerView, defStyleAttr, 0);
        int strokeWidth = typedArray.getInt(R.styleable.ColorPickerView_pickerStrokeWidth, 0);
        boolean showPickedColorInCenter = typedArray.getBoolean(R.styleable.ColorPickerView_showPickedColorInCenter, false);
        int color = typedArray.getColor(R.styleable.ColorPickerView_defaultColor, getResources().getColor(R.color.colorPrimary));
        typedArray.recycle();

        init(showPickedColorInCenter, strokeWidth, color, null);
    }
    // */

    /**
     * Called to determine the size requirements for this view and all of its children.
     *
     * @param widthMeasureSpec integer codes representing dimensions
     * @param heightMeasureSpec integer codes representing dimensions
     *                          * A dimension contains two separate pieces of data:
     *                            available space, e.g.
     *                                  int widthPixels = View.MeasureSpec.getSize(widthMeasureSpec);
     *                            and
     *                            measure mode, e.g.
     *                                  int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthPixels = MeasureSpec.getSize(widthMeasureSpec);
        int heightPixels = MeasureSpec.getSize(heightMeasureSpec);
        Log.d(TAG, "Measure width=" + widthPixels + " | height=" + heightPixels);

        mCenterX = widthPixels / 2;
        mCenterY = heightPixels / 2;
        mRadius = mCenterX / 4;
        // setMeasuredDimension() must be called to store the measured width and height of this view.
        setMeasuredDimension((int) (mCenterX * 1.5), (int) (mCenterX * 2.0));
    }

    /**
     * Called when this view should assign a size and position to all of its children.
     *
     * @param changed This is a new size or position for this view
     * @param left Left position, relative to parent
     * @param top Top position, relative to parent
     * @param right Right position, relative to parent
     * @param bottom Bottom position, relative to parent
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    /**
     * Called when the size of this view has changed.
     *
     * @param w Current width of this view
     * @param h Current height of this view
     * @param oldw Old width of this view
     * @param oldh Old height of this view
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * Called when the view should render its content.
     *
     * @param canvas the canvas on which the background will be drawn
     */
    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);

        // Draw color palette
        float r = mCenterX; // without stroke
        //float r = mCenterX - mPaint.getStrokeWidth()*0.5f; // with stroke
        canvas.translate(mCenterX, mCenterX);
        // Todo: Avoid object allocations during draw/layout operations (preallocate and reuse instead)
        canvas.drawOval(new RectF(-r, -r, r, r), mPaint);

        if (mPickedColorInCenter) {
            if (mHighlightCenter)
                mCenterPaint.setAlpha(0xFF);
            else
                mCenterPaint.setAlpha(0x80);

            canvas.drawCircle(0, 0, mRadius, mCenterPaint);
        }

        /*if (mTrackingCenter) {
            int c = mCenterPaint.getColor();
            mCenterPaint.setStyle(Paint.Style.STROKE);

            if (mHighlightCenter) {
                mCenterPaint.setAlpha(0xFF);
            } else {
                mCenterPaint.setAlpha(0x80);
            }
            canvas.drawCircle(0, 0,
                    CENTER_RADIUS + mCenterPaint.getStrokeWidth(),
                    mCenterPaint);

            mCenterPaint.setStyle(Paint.Style.FILL);
            mCenterPaint.setColor(c);
        }*/
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX() - mCenterX;
        float y = event.getY() - mCenterX;
        boolean inCenterPaintArea = Math.sqrt(x*x + y*y) <= mRadius;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mPickedColorInCenter && inCenterPaintArea) {
                    mHighlightCenter = inCenterPaintArea;
                    invalidate();
                    break;
                }
            case MotionEvent.ACTION_MOVE:
                mHighlightCenter = inCenterPaintArea;

                if (mPickedColorInCenter && inCenterPaintArea) {
                    invalidate();
                } else {
                    float angle = (float) Math.atan2(y, x);
                    float unit = angle / (2*PI); // turn angle [-PI ... PI] into unit [0...1]
                    if (unit < 0) {
                        unit += 1;
                    }
                    mCenterPaint.setColor(interpColor(COLOR_SET, unit));
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                mColorPicked = mCenterPaint.getColor();
                if (mOnColorChangedListener!=null)
                    mOnColorChangedListener.onColorChanged(mColorPicked);
                break;
        }
        return true;
    }

    public void setColorPicked(int color) {
        mCenterPaint.setColor(mColorPicked = color);
        invalidate();
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mOnColorChangedListener = listener;
    }

    private void init(boolean showPickedColorInCenter, int strokeWidth, int color, OnColorChangedListener listener) {
        mPickedColorInCenter = showPickedColorInCenter;
        mStrokeWidth = strokeWidth;
        mColorPicked = color;
        mOnColorChangedListener = listener;

        Shader shader = new SweepGradient(0, 0, COLOR_SET, null);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setShader(shader);
        if (mStrokeWidth>0) {
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(mStrokeWidth);
        } else {
            mPaint.setStyle(Paint.Style.FILL); // Paint.Style.FILL_AND_STROKE
        }

        mCenterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCenterPaint.setStyle(Paint.Style.FILL);
        mCenterPaint.setColor(mColorPicked);
    }

    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }

    private int interpColor(int[] colors, float unit) {
        if (unit <= 0) {
            return colors[0];
        }
        if (unit >= 1) {
            return colors[colors.length - 1];
        }

        float p = unit * (colors.length - 1);
        int i = (int)p;
        p -= i;

        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i+1];
        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);

        return Color.argb(a, r, g, b);
    }
}
