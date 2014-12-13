package com.troop.freedcam.camera2.parameters;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.util.Log;

import com.troop.freedcam.camera.parameters.CameraParametersEventHandler;
import com.troop.freedcam.camera2.BaseCameraHolderApi2;
import com.troop.freedcam.camera2.parameters.modes.FlashModeApi2;
import com.troop.freedcam.camera2.parameters.modes.PictureSizeModeApi2;
import com.troop.freedcam.camera2.parameters.modes.SceneModeApi2;
import com.troop.freedcam.i_camera.I_CameraHolder;
import com.troop.freedcam.i_camera.parameters.AbstractParameterHandler;
import com.troop.freedcam.ui.AppSettingsManager;

import java.util.List;

/**
 * Created by troop on 12.12.2014.
 */
public class ParameterHandlerApi2 extends AbstractParameterHandler
{
    public static String TAG = ParameterHandlerApi2.class.getSimpleName();

    BaseCameraHolderApi2 cameraHolder;

    public ParameterHandlerApi2(I_CameraHolder cameraHolder, AppSettingsManager appSettingsManager)
    {
        this.cameraHolder = (BaseCameraHolderApi2) cameraHolder;
        ParametersEventHandler = new CameraParametersEventHandler();
        this.appSettingsManager = appSettingsManager;



    }

    public void Init()
    {
        List<CaptureRequest.Key<?>> keys = this.cameraHolder.characteristics.getAvailableCaptureRequestKeys();
        for (int i = 0; i< keys.size(); i++)
        {
            Log.d(TAG, keys.get(i).getName());
        }
        boolean muh = this.cameraHolder.characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE );
        FlashMode = new FlashModeApi2(this.cameraHolder);
        SceneMode = new SceneModeApi2(this.cameraHolder);
        PictureSize = new PictureSizeModeApi2(this.cameraHolder);
        ParametersEventHandler.ParametersHasLoaded();
    }


}