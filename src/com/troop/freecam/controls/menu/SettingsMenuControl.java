package com.troop.freecam.controls.menu;


import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.troop.freecam.MainActivity;
import com.troop.freecam.R;
import com.troop.freecam.camera.CameraManager;
import com.troop.freecam.controls.base.InfoScreenControl;
import com.troop.freecam.controls.base.MenuItemControl;
import com.troop.freecam.controls.base.NumericUpDownControl;
import com.troop.freecam.interfaces.INumericUpDownValueCHanged;
import com.troop.freecam.manager.parameters.ParametersManager;
import com.troop.freecam.manager.SettingsManager;
import com.troop.menu.FlashMenu;
import com.troop.menu.FocusMenu;
import com.troop.menu.PictureSizeMenu;
import com.troop.menu.VideoSizesMenu;
import com.troop.menu.switchcameramenu;

/**
 * Created by troop on 01.01.14.
 */
public class SettingsMenuControl extends LinearLayout
{
    public Switch upsidedown;
    public Switch tripod;
    public Switch ExynosRaw;
    public Switch crop_box;
    InfoScreenControl infoScreenFragment;
    public Switch checkBoxOnScreen;
    public MenuItemControl switchCamera;
    MenuItemControl switchFlash;
    MenuItemControl switchFocus;
    //MenuItemFragment switchPictureFormat;
    MenuItemControl switchPictureSize;

    MenuItemControl switchVideoSize;
    NumericUpDownControl captureFrames;

    CameraManager camMan;
    MainActivity activity;
    PreviewSubMenuControl previewSubMenu;
    QualitySubMenuControl qualitySubMenu;
    HdrSubMenuControl hdrSubMenu;

    public SettingsMenuControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SettingsMenuControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SettingsMenuControl(Context context) {
        super(context);
    }

    public void SetStuff(CameraManager cameraManager, MainActivity activity, InfoScreenControl infoScreenFragment)
    {
        this.camMan = cameraManager;
        this.activity = activity;
        this.infoScreenFragment = infoScreenFragment;
        initSettingsMenuButtons();
    }

    private void initSettingsMenuButtons()
    {

        LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.settingsmenufragment, this);

        previewSubMenu = (PreviewSubMenuControl)activity.findViewById(R.id.preview_submenu_control);
        previewSubMenu.Init(activity, camMan);
        qualitySubMenu = (QualitySubMenuControl)activity.findViewById(R.id.quality_submenu_control);
        qualitySubMenu.Init(activity,camMan);
        hdrSubMenu = (HdrSubMenuControl)activity.findViewById(R.id.hdr_submenu_control);
        hdrSubMenu.Init(activity, camMan);


        upsidedown = (Switch)findViewById(R.id.button_fixupsidedown);

        if (camMan.Settings.OrientationFix.GET())
            upsidedown.setChecked(true);
        upsidedown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (upsidedown.isChecked())
                {
                    camMan.Settings.OrientationFix.Set(true);
                    camMan.Stop();
                    camMan.Start();

                    camMan.ReloadCameraParameters(true);
                }
                else
                {
                    camMan.Settings.OrientationFix.Set(false);
                    camMan.Stop();
                    camMan.Start();
                    camMan.ReloadCameraParameters(true);
                }

            }
        });

        //tripod = (Switch)findViewById(R.id.button_tripod);

        tripod = (Switch)findViewById(R.id.button_tripod);
        tripod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (tripod.isChecked())
                {
                    camMan.parametersManager.setNightEnable("tripod");

                }
                else
                {
                    camMan.parametersManager.setNightEnable("off");

                }
            }
        });

        ExynosRaw = (Switch)findViewById(R.id.button_rawsave);
        ExynosRaw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (ExynosRaw.isChecked())
                {
                    camMan.parametersManager.setExynosRaw("raw-save");

                }
                else
                {
                    camMan.parametersManager.setExynosRaw("single");

                }
            }
        });

        crop_box = (Switch)findViewById(R.id.checkBox_crop);
        crop_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (crop_box.isChecked())
                {
                    camMan.crop = true;
                    camMan.Settings.CropImage.Set(true);
                }
                else
                {
                    camMan.crop = false;
                    camMan.Settings.CropImage.Set(false);
                }
            }
        });
        if(!camMan.Settings.Cameras.GetCamera().equals(SettingsManager.Preferences.MODE_3D))
        {
            crop_box.setVisibility(View.GONE);
        }
        else
        {
            crop_box.setChecked(true);
        }
        checkBoxOnScreen = (Switch)findViewById(R.id.checkBoxOnscreen);
        checkBoxOnScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (checkBoxOnScreen.isChecked())
                {
                    infoScreenFragment.showCurrentConfig();
                }
                else
                {
                    infoScreenFragment.hideCurrentConfig();
                }
            }
        });

        switchCamera = (MenuItemControl)findViewById(R.id.switch_camera_control);
        switchCamera.SetOnClickListner(new switchcameramenu(camMan, activity));

        switchFlash = (MenuItemControl)findViewById(R.id.switch_flash_control);
        switchFlash.SetOnClickListner(new FlashMenu(camMan, activity));

        switchFocus = (MenuItemControl)findViewById(R.id.switch_focus_control);
        switchFocus.SetOnClickListner(new FocusMenu(camMan, activity));

        switchPictureSize = (MenuItemControl)findViewById(R.id.switch_picturesize_control);
        switchPictureSize.SetOnClickListner(new PictureSizeMenu(camMan, activity));

        switchVideoSize = (MenuItemControl)findViewById(R.id.switch_videosize_control);
        switchVideoSize.SetOnClickListner(new VideoSizesMenu(camMan, activity));

        captureFrames = (NumericUpDownControl) findViewById(R.id.numericUpDown_CaptureFrames);
        captureFrames.setMinMax(0, 30);
        captureFrames.setCurrent(30);
        captureFrames.setOnValueCHanged(new INumericUpDownValueCHanged() {
            @Override
            public void ValueHasCHanged(int value) {
                camMan.Settings.captureFrameRate.set( value);
            }
        });




    }
    public void Hide()
    {
        setVisibility(View.GONE);
    }

    public void Show()
    {
        setVisibility(View.VISIBLE);
    }

    public void UpdateUI(boolean parametersReseted, ParametersManager.enumParameters paras)
    {
        if (parametersReseted)
        {
            checkVisibility();
            hdrSubMenu.UpdateUI();
            String tmp = camMan.Settings.Cameras.GetCamera();
            switchCamera.SetButtonText(tmp);
        }
        if (paras == ParametersManager.enumParameters.PictureSize || paras == ParametersManager.enumParameters.All)
        {
            String size1 = String.valueOf(camMan.parametersManager.getParameters().getPictureSize().width) + "x" + String.valueOf(camMan.parametersManager.getParameters().getPictureSize().height);
            switchPictureSize.SetButtonText(size1);
        }
        if (paras == ParametersManager.enumParameters.VideoModes || paras == ParametersManager.enumParameters.All)
            switchVideoSize.SetButtonText(camMan.parametersManager.VideoProfileClass.getProfileString());

        //switch3dButton.SetValue(tmp);

        previewSubMenu.UpdateUI(paras);
        qualitySubMenu.UpdateUI();


        //ZeroShutterLag

        if (paras == ParametersManager.enumParameters.FocusMode || paras == ParametersManager.enumParameters.All)
            switchFocus.SetButtonText(camMan.parametersManager.getParameters().getFocusMode());
        //ToDo add FlashToParamertersMAnager
        if (camMan.parametersManager.getSupportFlash() && (paras == ParametersManager.enumParameters.FlashMode || paras == ParametersManager.enumParameters.All))
            switchFlash.SetButtonText(camMan.parametersManager.getParameters().getFlashMode());

        if (camMan.Settings.CameraMode.get() == SettingsManager.Preferences.MODE_VIDEO)
        {
            switchVideoSize.setVisibility(VISIBLE);
            if (camMan.Settings.VideoProfileSETTINGS.get().contains("Timelapse"))
            {
                captureFrames.setVisibility(VISIBLE);
            }
            else
                captureFrames.setVisibility(GONE);
        }
        else {
            switchVideoSize.setVisibility(GONE);
            captureFrames.setVisibility(GONE);
        }

        if (camMan.Settings.CameraMode.get() == SettingsManager.Preferences.MODE_HDR)
        {
            hdrSubMenu.setVisibility(VISIBLE);
        }
        else
            hdrSubMenu.setVisibility(GONE);

        if (camMan.Settings.CameraMode.get() == SettingsManager.Preferences.MODE_PIC || camMan.Settings.CameraMode.get() == SettingsManager.Preferences.MODE_HDR)
        {
            switchPictureSize.setVisibility(VISIBLE);
        }
        else
            switchPictureSize.setVisibility(GONE);
    }

    private void checkVisibility()
    {

        if (camMan.Settings.Cameras.is3DMode())
        {
            crop_box.setVisibility(View.VISIBLE);
            crop_box.setChecked(camMan.parametersManager.doCropping());
        }
        else
            crop_box.setVisibility(View.GONE);
        //FLASH
        if (!camMan.parametersManager.getSupportFlash())
            switchFlash.setVisibility(View.GONE);
        else
        {
            if (switchFlash.getVisibility() == View.GONE)
                switchFlash.setVisibility(View.VISIBLE);

        }
    }
}
