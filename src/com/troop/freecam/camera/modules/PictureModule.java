package com.troop.freecam.camera.modules;

import android.hardware.Camera;
import android.util.Log;

import com.troop.freecam.camera.BaseCameraHolder;
import com.troop.freecam.manager.AppSettingsManager;
import com.troop.freecam.manager.SoundPlayer;
import com.troop.freecam.utils.DeviceUtils;

import java.io.File;

/**
 * Created by troop on 15.08.2014.
 */
public class PictureModule extends AbstractModule implements Camera.PictureCallback {


    public final String TAG = "freecam.PictureModule";

    public PictureModule(BaseCameraHolder baseCameraHolder, SoundPlayer soundPlayer, AppSettingsManager appSettingsManager)
    {
        super(baseCameraHolder, soundPlayer, appSettingsManager);
        name = "PictureModule";
    }

//I_Module START
    @Override
    public String ModuleName() {
        return name;
    }

    @Override
    public void DoWork()
    {
        takePicture();
    }

    @Override
    public boolean IsWorking() {
        return false;
    }
//I_Module END

    private void takePicture()
    {
        isWorking = true;
        Log.d(TAG, "Start Taking Picture");
        try
        {
            soundPlayer.PlayShutter();
            baseCameraHolder.GetCamera().takePicture(null, null, this);
            Log.d(TAG, "Picture Taking is Started");

        }
        catch (Exception ex)
        {
            Log.d(TAG,"Take Picture Failed");
            ex.printStackTrace();
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera)
    {
        //TODO add Picture saving logic
    }
}
