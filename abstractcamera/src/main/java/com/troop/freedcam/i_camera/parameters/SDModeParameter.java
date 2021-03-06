package com.troop.freedcam.i_camera.parameters;

import android.os.Handler;

import com.troop.freedcam.ui.AppSettingsManager;
import com.troop.freedcam.utils.StringUtils;

import java.io.File;

/**
 * Created by troop on 21.07.2015.
 */
public class SDModeParameter extends AbstractModeParameter
{
    final public static String internal = "Internal";
    final public static String external ="External";

    AppSettingsManager appSettingsManager;
    public SDModeParameter(Handler uiHandler, AppSettingsManager appSettingsManager) {
        super(uiHandler);
        this.appSettingsManager = appSettingsManager;
    }

    @Override
    public void addEventListner(I_ModeParameterEvent eventListner) {
        super.addEventListner(eventListner);
    }

    @Override
    public void removeEventListner(I_ModeParameterEvent parameterEvent) {
        super.removeEventListner(parameterEvent);
    }

    @Override
    public boolean IsSupported()
    {
        try {
            File file = new File(StringUtils.GetExternalSDCARD());
            if (file.exists())
                return true;
            else
                return false;
        }
        catch (Exception ex)
        {
            return false;
        }

    }

    @Override
    public void SetValue(String valueToSet, boolean setToCamera)
    {

    }

    @Override
    public String GetValue()
    {
        if (appSettingsManager.GetWriteExternal())
            return external;
        else
            return internal;
    }

    @Override
    public String[] GetValues() {
        return new String[] {internal,external};
    }

    @Override
    public void BackgroundValueHasChanged(String value) {
        super.BackgroundValueHasChanged(value);
    }

    @Override
    public void BackgroundValuesHasChanged(String[] value) {
        super.BackgroundValuesHasChanged(value);
    }

    @Override
    public void BackgroundIsSupportedChanged(boolean value) {
        super.BackgroundIsSupportedChanged(value);
    }

    @Override
    public void BackgroundSetIsSupportedHasChanged(boolean value) {
        super.BackgroundSetIsSupportedHasChanged(value);
    }

}
