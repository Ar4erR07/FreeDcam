package com.troop.freecamv2.camera.modules;

import android.util.Log;

import com.troop.freecamv2.camera.BaseCameraHolder;

import com.troop.freecam.manager.SoundPlayer;
import com.troop.freecamv2.ui.AppSettingsManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by troop on 16.08.2014.
 */
public class ModuleHandler
{
    HashMap<String, AbstractModule> moduleList;
    BaseCameraHolder cameraHolder;
    AppSettingsManager appSettingsManager;
    SoundPlayer soundPlayer;
    AbstractModule currentModule;
    final String TAG = "freecam.ModuleHandler";
    public ModuleEventHandler moduleEventHandler;

    public ArrayList<String> PictureModules;
    public ArrayList<String> VideoModules;
    public ArrayList<String> AllModules;

    public static final String MODULE_VIDEO = "module_video";
    public static final String MODULE_PICTURE = "module_picture";
    public static final String MODULE_HDR = "module_hdr";
    public static final String MODULE_BURST = "module_burst";
    public static final String MODULE_ALL = "module_all";

    public  ModuleHandler (BaseCameraHolder cameraHolder, AppSettingsManager appSettingsManager, SoundPlayer soundPlayer)
    {
        this.cameraHolder = cameraHolder;
        this.appSettingsManager = appSettingsManager;
        this.soundPlayer = soundPlayer;
        moduleList  = new HashMap<String, AbstractModule>();
        moduleEventHandler = new ModuleEventHandler();
        PictureModules = new ArrayList<String>();
        PictureModules.add(MODULE_PICTURE);
        PictureModules.add(MODULE_BURST);
        PictureModules.add(MODULE_HDR);
        VideoModules = new ArrayList<String>();
        VideoModules.add(MODULE_VIDEO);
        AllModules = new ArrayList<String>();
        AllModules.add(MODULE_ALL);
        initModules();

    }

    public void SetModule(String name)
    {
        currentModule = moduleList.get(name);
        moduleEventHandler.ModuleHasChanged(currentModule.ModuleName());
        Log.d(TAG, "Set Module to " + name);
    }

    public String GetCurrentModuleName()
    {
        if (currentModule != null)
            return currentModule.name;
        else return "";
    }

    public boolean DoWork()
    {
        if (currentModule != null && !currentModule.IsWorking()) {
            currentModule.DoWork();
            return true;
        }
        else
            return false;
    }

    private void initModules()
    {
        PictureModule pictureModule = new PictureModule(cameraHolder, soundPlayer, appSettingsManager);
        moduleList.put(pictureModule.ModuleName(), pictureModule);

        VideoModule videoModule = new VideoModule(cameraHolder, soundPlayer, appSettingsManager);
        moduleList.put(videoModule.ModuleName(), videoModule);

        HdrModule hdrModule = new HdrModule(cameraHolder,soundPlayer,appSettingsManager);
        moduleList.put(hdrModule.ModuleName(), hdrModule);
    }

}
