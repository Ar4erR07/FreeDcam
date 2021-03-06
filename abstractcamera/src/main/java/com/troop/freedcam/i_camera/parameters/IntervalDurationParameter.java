package com.troop.freedcam.i_camera.parameters;

import android.os.Handler;

/**
 * Created by troop on 08.01.2016.
 */
public class IntervalDurationParameter extends AbstractModeParameter
{

    String current = "1 min";

    public IntervalDurationParameter(Handler uiHandler) {
        super(uiHandler);
    }

    @Override
    public boolean IsSupported() {
        return true;
    }

    @Override
    public void SetValue(String valueToSet, boolean setToCamera) {
        current = valueToSet;
    }

    @Override
    public String GetValue() {
        return current;
    }

    @Override
    public String[] GetValues() {
        return new String[] {"1 min", "2 min", "5 min","10 min","15 min","20 min ","25 min","30 min","60 min"/*,"Bulb"*/};
    }
}
