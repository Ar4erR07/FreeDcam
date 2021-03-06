package com.troop.freedcam.camera.parameters.manual;

import com.troop.freedcam.i_camera.parameters.AbstractParameterHandler;
import com.troop.freedcam.utils.DeviceUtils;

import java.util.HashMap;

/**
 * Created by Ingo on 12.04.2015.
 */
public class SkintoneManualPrameter extends BaseManualParameter {
    /**
     * @param parameters
     * @param value
     * @param maxValue
     * @param MinValue
     * @param camParametersHandler
     */
    public SkintoneManualPrameter(HashMap<String, String> parameters, String value, String maxValue, String MinValue, AbstractParameterHandler camParametersHandler)
    {
        super(parameters, value, maxValue, MinValue, camParametersHandler);
        try
        {
            /*final String skin = parameters.get("skinToneEnhancement");
            if (skin != null && !skin.equals("")) {
                this.isSupported = true;
                this.value = "skinToneEnhancement";
            }*/
            if (DeviceUtils.IS_DEVICE_ONEOF(DeviceUtils.ZTE_DEVICES)
                    ||DeviceUtils.IS_DEVICE_ONEOF(DeviceUtils.AlcatelIdol3_Moto_MSM8982_8994)
                    ||DeviceUtils.IS_DEVICE_ONEOF(DeviceUtils.MI3_4))
                this.isSupported = true;
        }
        catch (Exception ex)
        {
            this.isSupported = false;

        }
    }

    @Override
    public int GetMaxValue() {
        return 100;
    }

    @Override
    public int GetMinValue() {
        return -100;
    }

    @Override
    protected void setvalue(int valueToSet) {
        camParametersHandler.SceneMode.SetValue("portrait", true);
        parameters.put("skinToneEnhancement",valueToSet + "");
        if (valueToSet == 0)
            camParametersHandler.SceneMode.SetValue("auto", true);
    }
}
