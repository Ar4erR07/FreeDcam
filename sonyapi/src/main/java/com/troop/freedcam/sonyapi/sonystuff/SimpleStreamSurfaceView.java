/*
 * Copyright 2014 Sony Corporation
 */

package com.troop.freedcam.sonyapi.sonystuff;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.Type;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.troop.freedcam.i_camera.modules.I_Callbacks;
import com.troop.freedcam.i_camera.parameters.AbstractModeParameter;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import troop.com.imageconverter.ScriptC_brightness;
import troop.com.imageconverter.ScriptC_contrast;
import troop.com.imageconverter.ScriptC_focuspeak_argb;
import troop.com.imageconverter.ScriptC_imagestack_argb;
import troop.com.imageconverter.ScriptC_starfinder;

/**
 * A SurfaceView based class to draw liveview frames serially.
 */
public class SimpleStreamSurfaceView extends SurfaceView implements SurfaceHolder.Callback, AbstractModeParameter.I_ModeParameterEvent {

    private static final String TAG = SimpleStreamSurfaceView.class.getSimpleName();

    private boolean mWhileFetching;
    private final BlockingQueue<DataExtractor> mJpegQueue = new ArrayBlockingQueue<DataExtractor>(2);
    private final BlockingQueue<DataExtractor> frameQueue = new ArrayBlockingQueue<DataExtractor>(2);
    private final boolean mInMutableAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    private Thread mDrawerThread;
    private int mPreviousWidth = 0;
    private int mPreviousHeight = 0;
    private final Paint mFramePaint;
    private  Paint paint;
    private StreamErrorListener mErrorListener;
    I_Callbacks.PreviewCallback previewFrameCallback;
    public boolean focuspeak = false;
    public NightPreviewModes nightmode = NightPreviewModes.off;
    int currentImageStackCount = 0;

    RenderScript mRS;
    private Allocation mInputAllocation;
    private Allocation mInputAllocation2;
    private Allocation mOutputAllocation;
    ScriptC_focuspeak_argb focuspeak_argb;
    ScriptC_imagestack_argb imagestack_argb;
    ScriptC_brightness brightnessRS;
    ScriptC_contrast contrastRS;
    ScriptC_starfinder starfinderRS;
    ScriptIntrinsicBlur blurRS;
    Bitmap drawBitmap;
    Bitmap stackBitmap;

    public enum NightPreviewModes
    {
        on,
        off,
        grayscale,
        zoompreview,
    }

    private void initRenderScript()
    {
        drawBitmap = Bitmap.createBitmap(mPreviousWidth, mPreviousHeight, Bitmap.Config.ARGB_8888);
        stackBitmap = Bitmap.createBitmap(mPreviousWidth, mPreviousHeight, Bitmap.Config.ARGB_8888);
        Type.Builder tbIn = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        tbIn.setX(mPreviousWidth);
        tbIn.setY(mPreviousHeight);
        Type.Builder tbIn2 = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        tbIn2.setX(mPreviousWidth);
        tbIn2.setY(mPreviousHeight);

        Type.Builder tbOut = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        tbOut.setX(mPreviousWidth);
        tbOut.setY(mPreviousHeight);

        mInputAllocation = Allocation.createTyped(mRS, tbIn.create(), Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        mInputAllocation2 = Allocation.createTyped(mRS, tbIn.create(), Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        mOutputAllocation = Allocation.createTyped(mRS, tbOut.create(), Allocation.MipmapControl.MIPMAP_NONE,  Allocation.USAGE_SCRIPT);

        //mScriptFocusPeak = new ScriptC_focus_peak(mRS);
    }


    /**
     * Constructor
     * 
     * @param context
     */
    public SimpleStreamSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
        mFramePaint = new Paint();
        mFramePaint.setDither(true);
        initBitmaps(context);
    }

    /**
     * Constructor
     * 
     * @param context
     * @param attrs
     */
    public SimpleStreamSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        mFramePaint = new Paint();
        mFramePaint.setDither(true);
        initBitmaps(context);
    }

    /**
     * Constructor
     * 
     * @param context
     * @param attrs
     * @param defStyle
     */
    public SimpleStreamSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getHolder().addCallback(this);
        mFramePaint = new Paint();
        mFramePaint.setDither(true);
        initBitmaps(context);
    }

    public void SetOnPreviewFrame(I_Callbacks.PreviewCallback previewCallback)
    {
        this.previewFrameCallback = previewCallback;
    }

    private void initBitmaps(Context context)
    {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        if (Build.VERSION.SDK_INT >= 18) {
            mRS = RenderScript.create(context);
            focuspeak_argb = new ScriptC_focuspeak_argb(mRS);
            imagestack_argb = new ScriptC_imagestack_argb(mRS);
            brightnessRS = new ScriptC_brightness(mRS);
            contrastRS = new ScriptC_contrast(mRS);
            blurRS = ScriptIntrinsicBlur.create(mRS, Element.U8_4(mRS));
            starfinderRS = new ScriptC_starfinder(mRS);
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // do nothing.
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // do nothing.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mWhileFetching = false;
    }

    /**
     * Start retrieving and drawing liveview frame data by new threads.
     * 
     * @return true if the starting is completed successfully, false otherwise.

     */
    public boolean start(final String streamUrl, StreamErrorListener listener) {
        mErrorListener = listener;

        if (streamUrl == null) {
            Log.e(TAG, "start() streamUrl is null.");
            mWhileFetching = false;
            mErrorListener.onError(StreamErrorListener.StreamErrorReason.OPEN_ERROR);
            return false;
        }
        if (mWhileFetching) {
            Log.w(TAG, "start() already starting.");
            return false;
        }

        mWhileFetching = true;

        // A thread for retrieving liveview data from server.
        new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Starting retrieving streaming data from server.");
                SimpleLiveviewSlicer slicer = null;

                try {

                    // Create Slicer to open the stream and parse it.
                    slicer = new SimpleLiveviewSlicer();
                    slicer.open(streamUrl);

                    while (mWhileFetching)
                    {
                        if (fetchPayLoad(slicer))
                        {

                            continue;

                        }
                    }
                } catch (IOException e) {
                    Log.w(TAG, "IOException while fetching: " + e.getMessage());
                    mErrorListener.onError(StreamErrorListener.StreamErrorReason.IO_EXCEPTION);
                } finally {
                    if (slicer != null) {
                        slicer.close();
                    }

                    if (mDrawerThread != null) {
                        mDrawerThread.interrupt();
                    }

                    mJpegQueue.clear();
                    frameQueue.clear();
                    mWhileFetching = false;
                }
            }
        }.start();

        // A thread for drawing liveview frame fetched by above thread.
        mDrawerThread = new Thread() {
            @Override
            public void run() {
                Log.d(TAG, "Starting drawing stream frame.");
                Bitmap frameBitmap = null;

                BitmapFactory.Options factoryOptions = new BitmapFactory.Options();
                factoryOptions.inSampleSize = 1;
                factoryOptions.inPreferQualityOverSpeed = true;
                factoryOptions.inDither = false;
                factoryOptions.inScaled = false;

                if (mInMutableAvailable) {
                    initInBitmap(factoryOptions);
                }

                while (mWhileFetching)
                {
                    DataExtractor dataExtractor = null;
                    DataExtractor frameExtractor =null;
                    try {
                        dataExtractor = mJpegQueue.take();
                        if (!frameQueue.isEmpty())
                            frameExtractor = frameQueue.take();


                    } catch (IllegalArgumentException e) {
                        if (mInMutableAvailable) {
                            clearInBitmap(factoryOptions);
                        }
                        continue;
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Drawer thread is Interrupted.");
                        break;
                    }
                    frameBitmap = BitmapFactory.decodeByteArray(dataExtractor.jpegData, 0, dataExtractor.jpegData.length, factoryOptions);

                    drawFrame(frameBitmap, dataExtractor, frameExtractor);
                }

                if (frameBitmap != null) {
                    frameBitmap.recycle();
                }
                mWhileFetching = false;
            }
        };
        mDrawerThread.start();
        return true;
    }

    private boolean fetchPayLoad(SimpleLiveviewSlicer slicer) throws IOException {
        final DataExtractor payload = slicer.nextDataExtractor();
        if (payload.commonHeader == null) { // never occurs
            //Log.e(TAG, "Liveview Payload is null.");
            return true;
        }
        if (payload.commonHeader.PayloadType == 1)
        {
            if (mJpegQueue.size() == 2) {
                mJpegQueue.remove();
            }
            mJpegQueue.add(payload);
            if (previewFrameCallback != null)
            {
                previewFrameCallback.onPreviewFrame(payload.jpegData.clone(), I_Callbacks.JPEG);
            }
        }
        if (payload.commonHeader.PayloadType == 2) {
            if (frameQueue.size() == 2) {
                frameQueue.remove();
            }
            frameQueue.add(payload);
        }
        return false;
    }


    private int convert(int length, int val)
    {
        double pro = ((double)val /(double)10000 * 100);
        double newret = (double)length /100 * pro;
        return (int)newret;
    }

    /**
     * Request to stop retrieving and drawing liveview data.
     */
    public void stop() {
        mWhileFetching = false;
    }

    /**
     * Check to see whether start() is already called.
     * 
     * @return true if start() is already called, false otherwise.
     */
    public boolean isStarted() {
        return mWhileFetching;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void initInBitmap(BitmapFactory.Options options) {
        options.inBitmap = null;
        options.inMutable = true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void clearInBitmap(BitmapFactory.Options options) {
        if (options.inBitmap != null) {
            options.inBitmap.recycle();
            options.inBitmap = null;
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setInBitmap(BitmapFactory.Options options, Bitmap bitmap) {
        options.inBitmap = bitmap;
    }

    /**
     * Draw frame bitmap onto a canvas.
     * 
     * @param frame
     */
    private void drawFrame(Bitmap frame, DataExtractor dataExtractor, DataExtractor frameExtractor)
    {
        try {
            if (frame.getWidth() != mPreviousWidth || frame.getHeight() != mPreviousHeight) {
                onDetectedFrameSizeChanged(frame.getWidth(), frame.getHeight());
                return;
            }

            //canvas.drawColor(Color.BLACK);
            int w = frame.getWidth();
            int h = frame.getHeight();
            Rect src = new Rect(0, 0, w, h);
            if (nightmode == NightPreviewModes.zoompreview)
            {
                int w4 = w /8;
                int h4 = w/8;
                src = new Rect(w/2 -w4, h/2 -h4, w/2 +w4, h/2+h4);
                mInputAllocation.copyFrom(frame);
                blurRS.setInput(mInputAllocation);
                blurRS.setRadius(0.3f);
                blurRS.forEach(mOutputAllocation);
                mOutputAllocation.copyTo(drawBitmap);
            }


            float by = Math.min((float) getWidth() / w, (float) getHeight() / h);
            int offsetX = (getWidth() - (int) (w * by)) / 2;
            int offsetY = (getHeight() - (int) (h * by)) / 2;
            Rect dst = new Rect(offsetX, offsetY, getWidth() - offsetX, getHeight() - offsetY);
            if (nightmode == NightPreviewModes.on)
            {
                if(!drawNightPreview(frame, frameExtractor, src, dst))
                    return;
            }
            else if (nightmode == NightPreviewModes.grayscale)
            {
                mInputAllocation.copyFrom(frame);
                blurRS.setInput(mInputAllocation);
                blurRS.setRadius(1.5f);
                blurRS.forEach(mOutputAllocation);
                mInputAllocation.copyFrom(mOutputAllocation);
                starfinderRS.set_gCurrentFrame(mInputAllocation);
                starfinderRS.forEach_processBrightness(mOutputAllocation);
                mOutputAllocation.copyTo(drawBitmap);

            }
            if (focuspeak) {
                if (nightmode != NightPreviewModes.off)
                    mInputAllocation.copyFrom(drawBitmap);
                else
                    mInputAllocation.copyFrom(frame);
                focuspeak_argb.set_gCurrentFrame(mInputAllocation);
                focuspeak_argb.forEach_peak(mOutputAllocation);
                mOutputAllocation.copyTo(drawBitmap);

            }
            Canvas canvas = getHolder().lockCanvas();
            if (canvas == null) {
                return;
            }
            if (nightmode != NightPreviewModes.off || focuspeak)
                canvas.drawBitmap(drawBitmap, src, dst, mFramePaint);
            else
                canvas.drawBitmap(frame, src, dst, mFramePaint);
            if (frameExtractor != null)
                drawFrameInformation(frameExtractor, canvas, dst);

            getHolder().unlockCanvasAndPost(canvas);
        }
        catch(IllegalStateException ex)
        {}
    }

    private boolean drawNightPreview(Bitmap frame, DataExtractor frameExtractor, Rect src, Rect dst) {
        mInputAllocation.copyFrom(frame);
        blurRS.setInput(mInputAllocation);
        blurRS.setRadius(1.5f);
        blurRS.forEach(mOutputAllocation);
        mInputAllocation.copyFrom(mOutputAllocation);
        if (currentImageStackCount == 0)
            mInputAllocation2.copyFrom(frame);
        else
            mInputAllocation2.copyFrom(drawBitmap);
        imagestack_argb.set_gCurrentFrame(mInputAllocation);
        imagestack_argb.set_gLastFrame(mInputAllocation2);
        imagestack_argb.forEach_stackimage(mOutputAllocation);
        mOutputAllocation.copyTo(drawBitmap);

        if (currentImageStackCount < 3)
            currentImageStackCount++;
        else
            currentImageStackCount = 0;
        if (currentImageStackCount < 3)
            return false;
        else
        {
            mInputAllocation.copyFrom(drawBitmap);
            brightnessRS.set_gCurrentFrame(mInputAllocation);
            brightnessRS.set_brightness((float) (100 / 255.0f));
            brightnessRS.forEach_processBrightness(mOutputAllocation);
            mOutputAllocation.copyTo(drawBitmap);
            mInputAllocation.copyFrom(drawBitmap);
            contrastRS.set_gCurrentFrame(mInputAllocation);
            contrastRS.invoke_setBright(200f);
            contrastRS.forEach_processContrast(mOutputAllocation);
            mOutputAllocation.copyTo(drawBitmap);
            return true;
        }
    }

    private void drawFrameInformation(DataExtractor dataExtractor, Canvas canvas, Rect dst)
    {
        if (dataExtractor.frameInfoList == null)
            return;
        for (int i=0; i< dataExtractor.frameInfoList.size(); i++)
        {
            DataExtractor.FrameInfo frameInfo =  dataExtractor.frameInfoList.get(i);
            int w = getWidth();
            int h = getHeight();
            int top = convert(h, frameInfo.Top);
            int left = convert(w, frameInfo.Left);
            int right =convert(w,frameInfo.Right);
            int bottom = convert(h,frameInfo.Bottom);
            if (frameInfo.Category == 0x01)
            {
                dst = new Rect(left, top, right, bottom);
                //Rect src = new Rect(0, 0, crosshairs[0].getWidth(), crosshairs[0].getHeight());
                if (frameInfo.Status == 0x01)
                    paint.setColor(Color.BLUE);
                    //canvas.drawBitmap(crosshairs[0], src, dst, mFramePaint);
                if (frameInfo.Status == 0x00)
                    paint.setColor(Color.RED);
                    //canvas.drawBitmap(crosshairs[1], src, dst, mFramePaint);
                if (frameInfo.Status == 0x04)
                    paint.setColor(Color.GREEN);
                    //canvas.drawBitmap(crosshairs[2], src, dst, mFramePaint);
            }
            else if (frameInfo.Category == 0x05 ||frameInfo.Category == 0x04)
            {
                paint.setColor(Color.BLUE);

            }
            canvas.drawRect(left, top, right, bottom, paint);

        }
    }

    /**
     * Called when the width or height of liveview frame image is changed.
     * 
     * @param width
     * @param height
     */
    private void onDetectedFrameSizeChanged(int width, int height) {
        Log.d(TAG, "Change of aspect ratio detected");
        mPreviousWidth = width;
        mPreviousHeight = height;
        initRenderScript();
        drawBlackFrame();
        drawBlackFrame();
        drawBlackFrame(); // delete triple buffers

    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

    }
    /**
     * Draw black screen.
     */
    private void drawBlackFrame() {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas == null) {
            return;
        }

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawRect(new Rect(0, 0, getWidth(), getHeight()), paint);
        getHolder().unlockCanvasAndPost(canvas);
    }

    @Override
    public void onValueChanged(String val) {

    }

    @Override
    public void onIsSupportedChanged(boolean isSupported) {

    }

    @Override
    public void onIsSetSupportedChanged(boolean isSupported) {

    }

    @Override
    public void onValuesChanged(String[] values) {

    }

    @Override
    public void onVisibilityChanged(boolean visible) {

    }

    public interface StreamErrorListener {

        enum StreamErrorReason {
            IO_EXCEPTION,
            OPEN_ERROR,
        }

        void onError(StreamErrorReason reason);
    }
}
