package troop.com.imageconverter;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.troop.freedcam.i_camera.AbstractCameraUiWrapper;
import com.troop.freedcam.i_camera.Size;
import com.troop.freedcam.i_camera.interfaces.I_CameraChangedListner;
import com.troop.freedcam.i_camera.interfaces.I_Module;
import com.troop.freedcam.i_camera.interfaces.I_Shutter_Changed;
import com.troop.freedcam.i_camera.modules.AbstractModuleHandler;
import com.troop.freedcam.i_camera.modules.I_ModuleEvent;
import com.troop.freedcam.ui.I_AspectRatio;


/**
 * Created by troop on 24.08.2015.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class PreviewHandler implements Camera.PreviewCallback, I_CameraChangedListner,I_ModuleEvent
{
    final String TAG = PreviewHandler.class.getSimpleName();
    private I_AspectRatio output;
    AbstractCameraUiWrapper cameraUiWrapper;

    private int mHeight;
    private int mWidth;
    private RenderScript mRS;
    private Allocation mAllocationOut;
    private Allocation mAllocationIn;
    private Surface mSurface;
    private ScriptC_focus_peak_cam1 mScriptFocusPeak;
    private boolean enable = false;
    private boolean doWork = false;
    Context context;

    public PreviewHandler(I_AspectRatio output, AbstractCameraUiWrapper cameraUiWrapper, Context context)
    {
        Log.d(TAG, "Ctor");
        this.output = output;
        this.cameraUiWrapper = cameraUiWrapper;
        this.context = context;
        cameraUiWrapper.moduleHandler.moduleEventHandler.addListner(this);
        output.setSurfaceTextureListener(previewSurfaceListner);
        clear_preview("Ctor");
    }

    public void Enable(boolean enable)
    {
        Log.d(TAG, "Enable:" + enable);
        this.enable = enable;
        setEnable(this.enable);
    }

    private void setEnable(boolean enabled)
    {
        Log.d(TAG, "setEnable" + enabled);
        if (enabled)
        {
            if(mRS == null) {
                mRS = RenderScript.create(context.getApplicationContext());
                mRS.setPriority(RenderScript.Priority.LOW);
            }
            show_preview("setEnable");
            final Size size = new Size(cameraUiWrapper.camParametersHandler.PreviewSize.GetValue());
            reset(size.width, size.height);
            Log.d(TAG, "Set PreviewCallback");
            Log.d(TAG, "enable focuspeak");
        }
        else
        {
            Log.d(TAG, "stop focuspeak");
            cameraUiWrapper.cameraHolder.ResetPreviewCallback();
            clear_preview("setEnable");
            if (mRS != null)
                mRS.finish();
            //mRS = null;

        }
        if(cameraUiWrapper.camParametersHandler.Focuspeak != null && cameraUiWrapper.camParametersHandler.Focuspeak.IsSupported())
            cameraUiWrapper.camParametersHandler.Focuspeak.BackgroundValueHasChanged(enabled +"");
    }

    private void clear_preview(String from)
    {
        if (!doWork || !enable) {
            output.setAlpha(0);
            Log.d(TAG, "Preview cleared from:" + from);
        }
    }
    private void show_preview(String from)
    {
        if (doWork && enable) {
            output.setAlpha(1);
            Log.d(TAG, "Preview show from:" + from);
        }
    }

    public boolean isEnable() { return  enable;}

    private void reset(int width, int height)
    {
        mHeight = height;
        mWidth = width;
        if (mRS == null)
        {
            Log.d(TAG, "rest called but mRS is null");
            clear_preview("reset");
            return;
        }
        Log.d(TAG, "reset allocs to :" + width + "x" + height);
        try {
            cameraUiWrapper.cameraHolder.ResetPreviewCallback();
        }
        catch (NullPointerException ex){}

        Type.Builder tbIn = new Type.Builder(mRS, Element.U8(mRS));
        tbIn.setX(mWidth);
        tbIn.setY(mHeight);
        tbIn.setYuvFormat(ImageFormat.NV21);
        if (mAllocationOut != null)
            mAllocationOut.setSurface(null);

        mAllocationIn = Allocation.createTyped(mRS, tbIn.create(), Allocation.MipmapControl.MIPMAP_NONE,  Allocation.USAGE_SCRIPT);

        Type.Builder tbOut = new Type.Builder(mRS, Element.RGBA_8888(mRS));
        tbOut.setX(mWidth);
        tbOut.setY(mHeight);

        mAllocationOut = Allocation.createTyped(mRS, tbOut.create(), Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT | Allocation.USAGE_IO_OUTPUT);
        if (mSurface != null)
            mAllocationOut.setSurface(mSurface);
        else
            Log.d(TAG, "surfaceNull");
        mScriptFocusPeak = new ScriptC_focus_peak_cam1(mRS);
        Log.d(TAG, "script done enabled: " +enable);
        cameraUiWrapper.cameraHolder.SetPreviewCallback(this);
    }

    TextureView.SurfaceTextureListener previewSurfaceListner = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
        {
            mWidth = width;
            mHeight = height;
            Log.d(TAG, "SurfaceSizeAvail");
            mSurface = new Surface(surface);
            if (mAllocationOut != null)
                mAllocationOut.setSurface(mSurface);
            else
                Log.d(TAG, "Allocout null");
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "SurfaceSizeChanged");
            mSurface = new Surface(surface);
            if (mAllocationOut != null)
                mAllocationOut.setSurface(mSurface);
            else {
                Log.d(TAG, "Allocout null");

            }
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "SurfaceDestroyed");
            clear_preview("onSurfaceTextureDestroyed");
            mSurface = null;


            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };


    public void SetAspectRatio(int w, int h)
    {
        Log.d(TAG, "SetAspectRatio enable: " +enable);
        output.setAspectRatio(w, h);
        if (enable)
            reset(w,h);
    }

    boolean isWorking = false;
    @Override
    public void onPreviewFrame(final byte[] data, Camera camera)
    {
        if (enable == false)
        {
            Log.d(TAG, "onPreviewFrame enabled:" +enable);
            camera.addCallbackBuffer(data);
            return;
        }
        if (doWork == false) {
            camera.addCallbackBuffer(data);
            return;
        }
        if (data == null)
            return;
        if (isWorking == true) {
            camera.addCallbackBuffer(data);
            return;
        }

        int teosize = mHeight * mWidth *
                ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
        if (teosize != data.length) {
            Log.d(TAG, "frame size does not match rendersize");
            Camera.Size s = camera.getParameters().getPreviewSize();
            reset(s.width, s.height);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run()
            {
                isWorking = true;
                mAllocationIn.copyFrom(data);
                mScriptFocusPeak.set_gCurrentFrame(mAllocationIn);
                mScriptFocusPeak.forEach_peak(mAllocationOut);
                mAllocationOut.ioSend();
                isWorking = false;
            }
        }).start();
        camera.addCallbackBuffer(data);
    }

    @Override
    public void onCameraOpen(String message)
    {

    }

    @Override
    public void onCameraOpenFinish(String message) {

    }

    @Override
    public void onCameraClose(String message) {

    }

    @Override
    public void onPreviewOpen(String message)
    {
        Log.d(TAG, "onPreviewOpen enable:" + enable);
        clear_preview("onPreviewOpen");
        setEnable(enable);
    }

    @Override
    public void onPreviewClose(String message)
    {
    }

    @Override
    public void onCameraError(String error) {
    }

    @Override
    public void onCameraStatusChanged(String status) {
    }

    @Override
    public void onModuleChanged(I_Module module) {
    }

    @Override
    public String ModuleChanged(String module)
    {
        Log.d(TAG, "ModuleChanged(String):" + module + " enabled:" +enable);
        if (module.equals(AbstractModuleHandler.MODULE_PICTURE)
                ||module.equals(AbstractModuleHandler.MODULE_HDR)
                ||module.equals(AbstractModuleHandler.MODULE_INTERVAL))
        {
            setDoWork(true);
            setEnable(enable);

        }
        else {
            setDoWork(false);
            setEnable(enable);
        }
        return null;
    }

    private void setDoWork(boolean work) {this.doWork = work;}
}
