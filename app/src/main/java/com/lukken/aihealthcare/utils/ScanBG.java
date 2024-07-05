package com.lukken.aihealthcare.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import eu.id3.face.Rectangle;

public class ScanBG extends View
{
    private int maxX, maxY;
    public int getMaxX(){return maxX;}
    public int getMaxY(){return maxY;}

    private Paint mBGPaint;
    private Paint mCirclePaint;
    private Paint mCenterpaint;
    private Rect mBGRect;
    Rect faceRect = null;
    Rect centerRect = null;

    int sminX;
    int smaxX;
    int sminY;
    int smaxY;


    //region 생성
    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        setMeasuredDimension(measureWidth(widthMeasureSpec),measureHight(heightMeasureSpec));

        mBGRect = new Rect(0, 0, maxX, maxY);

        int centerX = maxX / 2;
        int centerY = maxY / 2;
        int gX = maxX / 3;

        sminX = centerX-gX;
        smaxX = centerX+gX;
        gX = maxX / 3;
        sminY = centerY-gX;
        gX = (int) (maxX / 2.5f);
        smaxY = centerY+gX;
        centerRect = new Rect(sminX, sminY, smaxX, smaxY);
    }

    private int measureHight(int measureSpec)
    {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if(specMode == MeasureSpec.EXACTLY)
            result = specSize;
        else
        {
            if(specMode == MeasureSpec.AT_MOST)
                result = Math.min(result, specSize);
        }
        maxY = result;
        return result;
    }

    private int measureWidth(int measureSpec)
    {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if(specMode == MeasureSpec.EXACTLY)
            result = specSize;
        else
        {
            if(specMode == MeasureSpec.AT_MOST)
                result = Math.min(result, specSize);
        }
        maxX = result;
        return result;
    }

    public ScanBG(Context context){
        super(context);
        Init();
    }
    public ScanBG(Context context, AttributeSet attrs){
        super(context, attrs);
        Init();
    }
    public ScanBG(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
        Init();
    }
    //endregion

    void Init(){
        mBGPaint = new Paint();
        mBGPaint.setAntiAlias(true);
        mBGPaint.setStyle(Paint.Style.FILL);
        mBGPaint.setColor(Color.WHITE);

        mCenterpaint = new Paint();
        mCenterpaint.setAntiAlias(true);
        mCenterpaint.setStyle(Paint.Style.STROKE);
        mCenterpaint.setStrokeWidth(2);

        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setColor(Color.TRANSPARENT);
        mCirclePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    }

    public boolean setBounding(Rectangle r, int pw, int ph){
        int faceL = r.topLeft.x;
        int faceR = r.bottomRight.x;
        int faceT = r.topLeft.y;
        int faceB = r.bottomRight.y;

        float pfL = faceL * 100f / (float)pw;
        float pfR = faceR * 100f / (float)pw;
        float pfT = faceT * 100f / (float)ph;
        float pfB = faceB * 100f / (float)ph;

        float dpL = maxX - ((float)maxX * pfL / 100f);
        float dpR = maxX - ((float)maxX * pfR / 100f);
        float dpT = (float)maxY * pfT / 100f;
        float dpB = (float)maxY * pfB / 100f;
        faceRect = new Rect((int)dpL, (int) dpT, (int)dpR, (int) dpB);

        int centerFaceX = faceRect.centerX();
        int centerFaceY = faceRect.centerY();

        boolean center = false;
        if(centerFaceX >= sminX && centerFaceX <= smaxX && centerFaceY >= sminY && centerFaceY <= smaxY) {
            center = true;
            mCenterpaint.setColor(Color.GREEN);
        }else
            mCenterpaint.setColor(Color.RED);

        invalidate();
        return center;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        canvas.drawRect(mBGRect, mBGPaint);

        float centerX = maxX / 2f;
        float centerY = maxY / 2f;
        float w = maxX / 2.2f;
        canvas.drawCircle(centerX, centerY, w, mCirclePaint);

        /*if(faceRect != null) {
            //canvas.drawCircle(centerRect.centerX(), centerRect.centerY(), (float)centerRect.width()/2, mCenterpaint);
            canvas.drawRect(centerRect, mCenterpaint);
            //canvas.drawCircle(faceRect.centerX(), faceRect.centerY(), 5, mCenterpaint);
        }*/
    }
}
