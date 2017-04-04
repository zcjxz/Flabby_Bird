package com.zcj.flabby_bird.game_view;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;

/**
 * Created by ZCJ on 2017/4/2.
 */

public class Floor {
    /**
     * 地板位置游戏面板高度的4/5到底部
     */
    private static final float FLOOR_Y_POS_RADIO=4/5F;
    /**
     * x 坐标
     */
    private int x;
    /**
     * y 坐标
     */
    private int y;
    /**
     * 填充物
     */
    private BitmapShader mFloorShader;

    private int mGameWidth;
    private int mGameHeight;

    public Floor(int gameWidth, int gameHeight, Bitmap floorBitmap){
        mGameWidth=gameWidth;
        mGameHeight=gameHeight;
        y= (int) (gameHeight*FLOOR_Y_POS_RADIO);
        mFloorShader=new BitmapShader(floorBitmap, Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
    }
    public void draw(Canvas mCanvas,Paint mPaint){
        if (-x>mGameWidth){
            x=x%mGameWidth;
        }
        mCanvas.save();
        mCanvas.translate(x,y);
        mPaint.setShader(mFloorShader);
        mCanvas.drawRect(x,0,-x+mGameWidth,mGameHeight-y,mPaint);
        mCanvas.restore();
        mPaint.setShader(null);
    }
    public int getX()
    {
        return x;
    }

    public void setX(int x)
    {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}
