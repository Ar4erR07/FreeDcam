package com.troop.freedcam.camera2.modules;

import android.annotation.TargetApi;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Rational;

import com.troop.androiddng.DngSupportedDevices;
import com.troop.androiddng.Matrixes;
import com.troop.androiddng.RawToDng;
import com.troop.freedcam.camera2.BaseCameraHolderApi2;
import com.troop.freedcam.i_camera.modules.AbstractModuleHandler;
import com.troop.freedcam.i_camera.modules.ModuleEventHandler;
import com.troop.freedcam.manager.MediaScannerManager;
import com.troop.freedcam.ui.AppSettingsManager;
import com.troop.freedcam.utils.DeviceUtils;
import com.troop.freedcam.utils.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by troop on 12.12.2014.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PictureModuleApi2 extends AbstractModuleApi2
{
    private static String TAG = StringUtils.TAG +PictureModuleApi2.class.getSimpleName();
    BaseCameraHolderApi2 cameraHolder;
    int mState;
    /**
     * Camera state: Showing camera preview.
     */
    public static final int STATE_PREVIEW = 0;
    /**
     * Camera state: Waiting for the focus to be locked.
     */
    public static final int STATE_WAITING_LOCK = 1;
    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    public static final int STATE_WAITING_PRECAPTURE = 2;
    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    public static final int STATE_WAITING_NON_PRECAPTURE = 3;
    /**
     * Camera state: Picture was taken.
     */
    public static final int STATE_PICTURE_TAKEN = 4;
    private TotalCaptureResult mDngResult;
    Handler backgroundHandler;

    int imagecount = 0;

    public PictureModuleApi2(BaseCameraHolderApi2 cameraHandler, AppSettingsManager Settings, ModuleEventHandler eventHandler, Handler backgroundHandler) {
        super(cameraHandler, Settings, eventHandler);
        this.cameraHolder = (BaseCameraHolderApi2)cameraHandler;
        this.Settings = Settings;
        this.backgroundHandler = backgroundHandler;
        this.name = AbstractModuleHandler.MODULE_PICTURE;
    }

    @Override
    public String LongName() {
        return "Picture";
    }

    @Override
    public String ShortName() {
        return "Pic";
    }

    @Override
    public boolean DoWork()
    {
        if (!cameraHolder.isWorking)
        {
            /*get pic size*/
            workstarted();
            TakePicture();
        }
        return true;
    }

    public void TakePicture()
    {
        isWorking = true;
        Log.d(TAG, Settings.getString(AppSettingsManager.SETTING_PICTUREFORMAT));
        Log.d(TAG, "dng:" + Boolean.toString(ParameterHandler.IsDngActive()));

        cameraHolder.mImageReader.setOnImageAvailableListener(mOnRawImageAvailableListener, backgroundHandler);

        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                captureStillPicture();
            }
        });

        //lockFocus();
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     *
     */
    private void captureStillPicture() {
        try {
            Log.d(TAG, "StartStillCapture");
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder = cameraHolder.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(cameraHolder.mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            try {
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AF_MODE));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AE_MODE));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.FLASH_MODE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.FLASH_MODE));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.COLOR_CORRECTION_MODE));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.COLOR_CORRECTION_GAINS));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.COLOR_CORRECTION_TRANSFORM));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.TONEMAP_CURVE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.TONEMAP_CURVE));
            }catch (NullPointerException ex){};
            try {
                int awb = cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AWB_MODE);
                captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, awb );
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.EDGE_MODE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.EDGE_MODE));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.HOT_PIXEL_MODE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.HOT_PIXEL_MODE));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.NOISE_REDUCTION_MODE));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION));
            }catch (NullPointerException ex){};
            try {
                long val = 0;
                if(!cameraHolder.ParameterHandler.ManualShutter.GetStringValue().equals("Auto"))
                    val = (long)(StringUtils.getMilliSecondStringFromShutterString(cameraHolder.ParameterHandler.ManualShutter.getStringValues()[cameraHolder.ParameterHandler.ManualShutter.GetValue()]) * 1000f);
                else
                    val= cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.SENSOR_EXPOSURE_TIME);
                Log.d(TAG, "Set ExposureTime for Capture to:" + val);
                captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, val);
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.SENSOR_SENSITIVITY));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.CONTROL_EFFECT_MODE));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.CONTROL_SCENE_MODE));
            }catch (NullPointerException ex){};
            try {
                captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, cameraHolder.mPreviewRequestBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE));
            }catch (NullPointerException ex){};
            try {
                if (Settings.getString(AppSettingsManager.SETTING_OrientationHack).equals(StringUtils.ON))
                    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 180);
                else
                    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
            }catch (NullPointerException ex){};
            List<CaptureRequest> captureList = new ArrayList<CaptureRequest>();
            for (int i=0; i< ParameterHandler.Burst.GetValue()+1; i++)
            {
                captureList.add(captureBuilder.build());
            }
            imagecount = 0;
            cameraHolder.mCaptureSession.stopRepeating();
            //captureBuilder.removeTarget(cameraHolder.previewsurface);
            mDngResult = null;
            //cameraHolder.mCaptureSession.captureBurst(captureList, CaptureCallback, backgroundHandler);
            cameraHolder.mCaptureSession.capture(captureBuilder.build(),CaptureCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    CameraCaptureSession.CaptureCallback CaptureCallback
            = new CameraCaptureSession.CaptureCallback()
    {

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                       TotalCaptureResult result)
        {
            mDngResult = result;
            try {
                Log.d(TAG, "CaptureResult Recieved");
            }
            catch (NullPointerException ex){};
            try {
                Log.d(TAG, "ColorCorrectionGains" + mDngResult.get(CaptureResult.COLOR_CORRECTION_GAINS));
            }catch (NullPointerException ex){};
            try {
                Log.d(TAG, "ColorCorrectionTransform" + mDngResult.get(CaptureResult.COLOR_CORRECTION_TRANSFORM));
            }
            catch (NullPointerException ex){};
            try {
                Log.d(TAG, "ToneMapCurve" + mDngResult.get(CaptureResult.TONEMAP_CURVE));
            }
            catch (NullPointerException ex){};
            try {
                Log.d(TAG, "Sensor Sensitivity" + mDngResult.get(CaptureResult.SENSOR_SENSITIVITY));
            }
            catch (NullPointerException ex){};
            try {
                Log.d(TAG, "Sensor ExposureTime" + mDngResult.get(CaptureResult.SENSOR_EXPOSURE_TIME));
            }
            catch (NullPointerException ex){};
            try {
                Log.d(TAG, "Sensor FrameDuration" + mDngResult.get(CaptureResult.SENSOR_FRAME_DURATION));
            }
            catch (NullPointerException ex){};
            try {
                Log.d(TAG, "Sensor GreenSplit" + mDngResult.get(CaptureResult.SENSOR_GREEN_SPLIT));
            }
            catch (NullPointerException ex){};
            try {
                Log.d(TAG, "Sensor NoiseProfile" + mDngResult.get(CaptureResult.SENSOR_NOISE_PROFILE).toString());
            }
            catch (NullPointerException ex){};
            try {
                Log.d(TAG, "Sensor NeutralColorPoint" + mDngResult.get(CaptureResult.SENSOR_NEUTRAL_COLOR_POINT).toString());
            }
            catch (NullPointerException ex){};
            //Toast.makeText(getActivity(), "Saved: " + mFile, Toast.LENGTH_SHORT).show();

        }
    };

    private void finishCapture() {
        try
        {
            Log.d(TAG, "CaptureDone");
            //cameraHolder.SetLastUsedParameters(cameraHolder.mPreviewRequestBuilder);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;

            //cameraHolder.mCaptureSession.abortCaptures();
            try {
                cameraHolder.mCaptureSession.setRepeatingRequest(cameraHolder.mPreviewRequestBuilder.build(), cameraHolder.mCaptureCallback,
                        null);
            }
            catch (CameraAccessException ex)
            {
                cameraHolder.CloseCamera();
                cameraHolder.OpenCamera(Settings.GetCurrentCamera());
            }

        }
        catch (NullPointerException ex) {
            ex.printStackTrace();
        }

        isWorking = false;
    }

    public void checkFileExists(File fileName) {
        if(!fileName.getParentFile().exists())
            fileName.getParentFile().mkdirs();
        if (!fileName.exists())
            try {
                fileName.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private final ImageReader.OnImageAvailableListener mOnRawImageAvailableListener = new ImageReader.OnImageAvailableListener()
    {
        @Override
        public void onImageAvailable(final ImageReader reader)
        {
            new Thread(new Runnable() {
                @Override
                public void run()
                {
                    while (mDngResult == null)
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    int burstcount = ParameterHandler.Burst.GetValue()+1;
                    File file = null;
                    Handler handler = new Handler(Looper.getMainLooper());
                    imagecount++;
                    if (reader.getImageFormat() == ImageFormat.JPEG)
                    {
                        file = process_jpeg(burstcount, reader);
                    }

                    else  if (reader.getImageFormat() == ImageFormat.RAW10)
                    {
                        file = process_raw10(burstcount, reader);

                    }
                    else if (reader.getImageFormat() == ImageFormat.RAW_SENSOR /*&& cameraHolder.ParameterHandler.IsDngActive()*/)
                    {
                        file = process_rawSensor(burstcount, reader);
                    }


                    isWorking = false;
                    MediaScannerManager.ScanMedia(Settings.context.getApplicationContext(), file);
                    eventHandler.WorkFinished(file);
                    if (burstcount == imagecount) {
                        workfinished(true);
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                finishCapture();
                            }
                        });
                    }
                }
            }).start();
        }
    };

    @NonNull
    private File process_jpeg(int burstcount, ImageReader reader) {
        File file;
        Log.d(TAG, "Create JPEG");
        if (burstcount > 1)
            file = new File(StringUtils.getFilePath(Settings.GetWriteExternal(), "_" + imagecount + ".jpg"));
        else
            file = new File(StringUtils.getFilePath(Settings.GetWriteExternal(), ".jpg"));
        checkFileExists(file);
        Image image = reader.acquireNextImage();
        while (image == null) {
            image = reader.acquireNextImage();

        }
        new ImageSaver(image, file).run();
        return file;
    }

    @NonNull
    private File process_rawSensor(int burstcount, ImageReader reader) {
        File file;
        Log.d(TAG, "Create DNG");
        if (burstcount > 1)
            file = new File(StringUtils.getFilePath(Settings.GetWriteExternal(), "_" + imagecount + ".dng"));
        else
            file = new File(StringUtils.getFilePath(Settings.GetWriteExternal(), ".dng"));
        checkFileExists(file);
        Image image = reader.acquireNextImage();
        while (image == null) {
            image = reader.acquireNextImage();
        }


        if(DeviceUtils.IS(DeviceUtils.Devices.Moto_MSM8974) || DeviceUtils.IS(DeviceUtils.Devices.OnePlusTwo))
        {
            final RawToDng dngConverter = RawToDng.GetInstance();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            dngConverter.SetBayerData(bytes, file.getAbsolutePath());
            float fnum, focal = 0;
            fnum = 2.0f;
            focal = 4.7f;
            Log.d("Freedcam RawCM2",String.valueOf(bytes.length));

            //  int mISO = mDngResult.get(CaptureResult.SENSOR_SENSITIVITY));
            double mExposuretime;
            int mFlash;


            dngConverter.setExifData(0, 0, 0, fnum, focal, "0", "0", 0);

            dngConverter.WriteDNG(DeviceUtils.DEVICE());
            dngConverter.RELEASE();
            image.close();
            bytes = null;
        }
        else
        {
            DngCreator dngCreator = new DngCreator(cameraHolder.characteristics, mDngResult);

            try {
                dngCreator.writeImage(new FileOutputStream(file), image);
            } catch (IOException e) {
                e.printStackTrace();
            }
            image.close();
        }
        return file;
    }

    @NonNull
    private File process_raw10(int burstcount, ImageReader reader) {
        File file;
        Log.d(TAG, "Create DNG VIA RAw2DNG");
        if (burstcount > 1)
            file = new File(StringUtils.getFilePath(Settings.GetWriteExternal(), "_" + imagecount + ".dng"));
        else
            file = new File(StringUtils.getFilePath(Settings.GetWriteExternal(), ".dng"));
        checkFileExists(file);
        Image image = reader.acquireNextImage();
        while (image == null) {
            image = reader.acquireNextImage();
        }
        final RawToDng dngConverter = RawToDng.GetInstance();
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        dngConverter.SetBayerData(bytes, file.getAbsolutePath());
        float fnum, focal = 0;
        fnum = mDngResult.get(CaptureResult.LENS_APERTURE);
        focal = mDngResult.get(CaptureResult.LENS_FOCAL_LENGTH);
        Log.d("Freedcam RawCM2",String.valueOf(bytes.length));

        int mISO = mDngResult.get(CaptureResult.SENSOR_SENSITIVITY).intValue();
        double mExposuretime = mDngResult.get(CaptureResult.SENSOR_EXPOSURE_TIME).doubleValue();
        int mFlash = mDngResult.get(CaptureResult.FLASH_STATE).intValue();

        dngConverter.setExifData(mISO, mExposuretime, mFlash, fnum, focal, "0", "0", 0);

        int black  = cameraHolder.characteristics.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN).getOffsetForIndex(0,0);
        int c= cameraHolder.characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT);
        String colorpattern;
        switch (c)
        {
            case 1:
                colorpattern = DngSupportedDevices.GRBG;
                break;
            case 2:
                colorpattern = DngSupportedDevices.GBRG;
                break;
            case 3:
                colorpattern = DngSupportedDevices.BGGR;
                break;
            default:
                colorpattern = DngSupportedDevices.RGGB;
                break;
        }
        float[] m1  = getFloatMatrix(cameraHolder.characteristics.get(CameraCharacteristics.SENSOR_COLOR_TRANSFORM2));
        float[] m2 = getFloatMatrix(cameraHolder.characteristics.get(CameraCharacteristics.SENSOR_COLOR_TRANSFORM1));
        Rational[] n =  mDngResult.get(CaptureResult.SENSOR_NEUTRAL_COLOR_POINT);
        float[] neutral = new float[3];
        neutral[0] = n[0].floatValue();
        neutral[1] = n[1].floatValue();
        neutral[2] = n[0].floatValue();
        float[] f1  = getFloatMatrix(cameraHolder.characteristics.get(CameraCharacteristics.SENSOR_FORWARD_MATRIX2));
        float[] f2  = getFloatMatrix(cameraHolder.characteristics.get(CameraCharacteristics.SENSOR_FORWARD_MATRIX1));
        DngSupportedDevices d = new DngSupportedDevices();
        DngSupportedDevices.DngProfile prof = d.getProfile(black,image.getWidth(), image.getHeight(), DngSupportedDevices.Mipi, colorpattern, 0,
                m1,
                m2,
                neutral,
                f1,
                f2,
                Matrixes.G4_reduction_matrix1,
                Matrixes.G4_reduction_matrix2,
                Matrixes.G4_noise_3x1_matrix
                );

        dngConverter.WriteDngWithProfile(prof);
        dngConverter.RELEASE();
        image.close();
        bytes = null;
                       /* }
                        else
                        {
                            Log.d(TAG, "Create RAW 10");
                            if (burstcount > 1)
                                file = new File(StringUtils.getFilePath(Settings.GetWriteExternal(), "_"+ imagecount +".raw"));
                            else
                                file = new File(StringUtils.getFilePath(Settings.GetWriteExternal(), ".raw"));
                            checkFileExists(file);
                            //Image image = reader.acquireNextImage();
                            while (image == null) {
                                image = reader.acquireNextImage();

                            }
                            new ImageSaver(image, file).run();
                        }*/
        return file;
    }

    private float[]getFloatMatrix(ColorSpaceTransform transform)
    {
        float[] ret = new float[9];

        ret[0] = transform.getElement(0,0).floatValue();
        ret[1] = transform.getElement(0,1).floatValue();
        ret[2] = transform.getElement(0,2).floatValue();
        ret[3] = transform.getElement(1,0).floatValue();
        ret[4] = transform.getElement(1,1).floatValue();
        ret[5] = transform.getElement(1,2).floatValue();
        ret[6] = transform.getElement(2,0).floatValue();
        ret[7] = transform.getElement(2,1).floatValue();
        ret[8] = transform.getElement(2,2).floatValue();
        return ret;
    }

    /**
     * Saves a JPEG {@link android.media.Image} into the specified {@link File}.
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();

                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void LoadNeededParameters()
    {
        //cameraHolder.StopPreview();
        //cameraHolder.StartPreview();
        super.LoadNeededParameters();
    }

    @Override
    public void UnloadNeededParameters() {
        super.UnloadNeededParameters();
        //cameraHolder.StopPreview();
    }

}
