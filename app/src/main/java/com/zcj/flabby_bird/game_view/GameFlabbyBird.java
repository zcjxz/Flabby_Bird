package com.zcj.flabby_bird.game_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.zcj.flabby_bird.R;
import com.zcj.flabby_bird.Util;

import java.util.ArrayList;
import java.util.List;


public class GameFlabbyBird extends SurfaceView implements SurfaceHolder.Callback,Runnable{

    private static final String TAG="zcj_draw";
    private SurfaceHolder mHolder;
    //背景图
    private Bitmap mBg;
    //线程的控制开关
    private boolean isRunnging;
    //绘图线程
    private Thread drawThread;
    //锁定的画布
    private Canvas mCanvas;
    //帧数
    private int fsp=60;
    //当前View的尺寸
    private int mWidth;
    private int mHeight;
    private RectF mGamePanelRect=new RectF();
    private Bitmap mBirdBitmap;
    private Bird mBird;
    private Floor mFloor;
    private int mSpeed;
    private Bitmap mFloorBitmap;
    private Paint mPaint;
    //管道相关
    private Bitmap mPipeTop;
    private Bitmap mPipeBottom;
    private RectF mPipeRect;
    private int mPipeWidth;
    /*
    管道的宽度
     */
    private static final int PIPE_WIDTH=60;

    private List<Pipe> mPipes=new ArrayList<Pipe>();

    /**
     * 分数
     */
    private final int[] mNums = new int[] { R.drawable.n0, R.drawable.n1,
            R.drawable.n2, R.drawable.n3, R.drawable.n4, R.drawable.n5,
            R.drawable.n6, R.drawable.n7, R.drawable.n8, R.drawable.n9 };
    private Bitmap[] mNumBitmap;
    private int mGrade = 0;
    /**
     * 单个数字的高度的1/15
     */
    private static final float RADIO_SINGLE_NUM_HEIGHT = 1 / 15f;
    /**
     * 单个数字的宽度
     */
    private int mSingleGradeWidth;
    /**
     * 单个数字的高度
     */
    private int mSingleGradeHeight;
    /**
     * 单个数字的范围
     */
    private RectF mSingleNumRectF;

    /**
     * 记录游戏的状态
     */
    private GameStatus mStatus =GameStatus.WAITING;

    /**
     * 触摸上升的距离，因为是上升，所以为负值
     */
    private static final int TOUCH_UP_SIZE = -16;
    /**
     * 将上升的距离转化为px
     *
     */
    private final int mBirdUpDis = Util.dp2px(getContext(), TOUCH_UP_SIZE);
    /**
     * 这里多存储一个变量，变量在run中计算
     */
    private int mTmpBirdDis;
    /**
     * 鸟自动下落的距离
     */
    private final int mAutoDownSpeed = Util.dp2px(getContext(), 2);

    /**
     * 两个管道间距离
     */
    private final int PIPE_DIS_BETWEEN_TWO = Util.dp2px(getContext(), 250);
    /**
     * 记录移动的距离，达到 PIPE_DIS_BETWEEN_TWO 则生成一个管道
     */
    private int mTmpMoveDistance;

    /**
     * 记录需要移除的管道
     */
    private List<Pipe> mNeedRemovePipe = new ArrayList<Pipe>();
    private int mRemovedPipe;

    public GameFlabbyBird(Context context) {
        this(context,null);
    }

    public GameFlabbyBird(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.addCallback(this);
        //设置画布，背景透明
        setZOrderOnTop(true);//把surfaceView设置到最顶层视图
        mHolder.setFormat(PixelFormat.TRANSLUCENT);//透明背景
        //设置可获得焦点
        setFocusable(true);
        setFocusableInTouchMode(true);
        //设置屏幕常亮
        this.setKeepScreenOn(true);

        initBitmaps();
        mSpeed= Util.dp2px(getContext(),2);
        mPipeWidth=Util.dp2px(getContext(),PIPE_WIDTH);
    }

    private void initBitmaps() {
        mPaint=new Paint();
        //抗锯齿
        mPaint.setAntiAlias(true);
        //防止抖动
        mPaint.setDither(true);
        mBg = loadImgByResId(R.drawable.bg1);
        mBirdBitmap = loadImgByResId(R.drawable.b1);
        mFloorBitmap = loadImgByResId(R.drawable.floor_bg2);
        mPipeTop=loadImgByResId(R.drawable.g2);
        mPipeBottom=loadImgByResId(R.drawable.g1);
        //获取分数的Bitmap
        mNumBitmap = new Bitmap[mNums.length];
        for (int i = 0; i < mNumBitmap.length; i++)
        {
            mNumBitmap[i] = loadImgByResId(mNums[i]);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth=w;
        mHeight=h;
        mGamePanelRect.set(0,0,w,h);
        // 初始化mBird
        mBird = new Bird(getContext(), mWidth, mHeight, mBirdBitmap);
        mFloor=new Floor(mWidth,mHeight,mFloorBitmap);
        // 初始化管道范围
        mPipeRect = new RectF(0, 0, mPipeWidth, mHeight);
//        Pipe pipe = new Pipe(getContext(), w, h, mPipeTop, mPipeBottom);
//        mPipes.add(pipe);
        // 初始化分数
        mSingleGradeHeight = (int) (h * RADIO_SINGLE_NUM_HEIGHT);
        mSingleGradeWidth = (int) (mSingleGradeHeight * 1.0f
                / mNumBitmap[0].getHeight() * mNumBitmap[0].getWidth());
        mSingleNumRectF = new RectF(0, 0, mSingleGradeWidth, mSingleGradeHeight);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //开启线程
        isRunnging=true;
        drawThread = new Thread(this);
        drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        //关闭线程
        isRunnging=false;
    }

    @Override
    public void run() {
        while(isRunnging){
            long startTime = System.currentTimeMillis();
            logic();
            doDraw();
            long endTime = System.currentTimeMillis();
//            int interval = 1000/fsp;
            int interval = 30;
            long consumeTime = endTime - startTime;
            if(consumeTime <interval){
                SystemClock.sleep(interval- (consumeTime));
            }
        }
    }

    /**
     * 处理一些逻辑上的计算
     */
    private void logic()
    {
        switch (mStatus)
        {
            case RUNNING:
                //重置分数
                mGrade=0;
                // 管道
                mTmpMoveDistance += mSpeed;
                // 生成一个管道
                if (mTmpMoveDistance >= PIPE_DIS_BETWEEN_TWO)
                {
                    Log.i(TAG, "addPipe");
                    Pipe pipe = new Pipe(getContext(), getWidth(), getHeight(),
                            mPipeTop, mPipeBottom);
                    mPipes.add(pipe);
                    mTmpMoveDistance = 0;
                }
                // 管道移动
                for (Pipe pipe : mPipes)
                {
                    if (pipe.getX() < -mPipeWidth)
                    {
                        mNeedRemovePipe.add(pipe);
                        mRemovedPipe++;
                        continue;
                    }
                    pipe.setX(pipe.getX() - mSpeed);
                }
                //移除管道
                mPipes.removeAll(mNeedRemovePipe);

                Log.e(TAG, "现存管道数量：" + mPipes.size());
                // 更新我们地板绘制的x坐标，地板移动
                mFloor.setX(mFloor.getX() - mSpeed);

                mTmpBirdDis += mAutoDownSpeed;
                mBird.setY(mBird.getY() + mTmpBirdDis);

                // 计算分数
                mGrade += mRemovedPipe;
                for (Pipe pipe : mPipes)
                {
                    if (pipe.getX() + mPipeWidth < mBird.getX())
                    {
                        mGrade++;
                    }
                }
                //检查游戏是否结束
                checkGameOver();
                break;

            case STOP: // 鸟落下

                // 如果鸟还在空中，先让它掉下来
                if (mBird.getY() < mFloor.getY() - mBird.getWidth())
                {
                    mTmpBirdDis += mAutoDownSpeed;
                    mBird.setY(mBird.getY() + mTmpBirdDis);
                } else
                {
                    mStatus = GameStatus.WAITING;
                    initPos();
                }
                break;
            default:
                break;
        }

    }

    private void initPos() {
        mPipes.clear();
        mNeedRemovePipe.clear();
        //重置鸟的位置
        mBird.setY(mHeight * 2 / 3);
        //重置下落速度
        mTmpBirdDis = 0;
        //重置通过的管道
        mRemovedPipe=0;
    }

    private void doDraw() {
        //捕获异常，防止退出时，mHolder为空，锁定画布报错。
        try {
            mCanvas = mHolder.lockCanvas();
            if (mCanvas!=null){
                drawBg();
                drawBird();
                drawPipes();
                drawFloor();
                drawGrades();
            }
        }catch (Exception e){

        }finally {
            if (mCanvas!=null){
                mHolder.unlockCanvasAndPost(mCanvas);
            }
        }

    }

    /**
     * 绘制分数
     */
    private void drawGrades()
    {
        String grade = mGrade + "";
        mCanvas.save(Canvas.MATRIX_SAVE_FLAG);
        mCanvas.translate(mWidth / 2 - grade.length() * mSingleGradeWidth / 2,
                1f / 8 * mHeight);
        // draw single num one by one
        for (int i = 0; i < grade.length(); i++)
        {
            String numStr = grade.substring(i, i + 1);
            int num = Integer.valueOf(numStr);
            mCanvas.drawBitmap(mNumBitmap[num], null, mSingleNumRectF, null);
            mCanvas.translate(mSingleGradeWidth, 0);
        }
        mCanvas.restore();
        Log.i(TAG, "drawGrades: ");
    }

    private void drawPipes() {
        for (Pipe pipe:mPipes){
            pipe.draw(mCanvas,mPipeRect);
        }
    }

    private void drawFloor() {
        mFloor.draw(mCanvas,mPaint);
        Log.i(TAG, "drawFloor: ");
    }

    private void drawBird() {
        mBird.draw(mCanvas);
    }

    private void drawBg() {
        mCanvas.drawBitmap(mBg,null,mGamePanelRect,null);
    }

    private void checkGameOver(){
        //如果触碰到地板，gg
        if(mBird.getY()+mBird.getHeight()>mFloor.getY()){
            mStatus=GameStatus.STOP;
        }
        //如果撞到管道
        for (Pipe pipe:mPipes){
            //已经穿过的
            if (pipe.getX()+mPipeWidth<mBird.getX()){
                continue;
            }
            if (pipe.touchBird(mBird)){
                mStatus=GameStatus.STOP;
                break;
            }
        }
    }

    /**
     * 根据id加载图片
     * @param resId 资源id
     * @return 返回bitmap资源
     */
    private Bitmap loadImgByResId(int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        return bitmap;
    }
    /**
     * 游戏的状态
     *
     * @author zhy
     *
     */
    private enum GameStatus
    {
        WAITING, RUNNING, STOP
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN)
        {
            switch (mStatus)
            {
                case WAITING:
                    mStatus = GameStatus.RUNNING;
                    break;
                case RUNNING:
                    mTmpBirdDis = mBirdUpDis;
                    break;

            }

        }

        return true;

    }
}
