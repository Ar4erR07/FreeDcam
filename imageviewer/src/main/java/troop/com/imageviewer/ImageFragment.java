package troop.com.imageviewer;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.ortiz.touch.TouchImageView;

import java.io.File;

/**
 * Created by troop on 21.08.2015.
 */
public class ImageFragment extends Fragment  {
    TouchImageView imageView;
    private File file;
    ProgressBar spinner;
    ViewPager pager;
    Button playVideo;


    public ScreenSlideFragment activity;

    private final int animationTime = 500;

    public void SetFilePath(File filepath)
    {
        this.file = filepath;
    }

    public File GetFilePath()
    {
        return file;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.imageframent, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.imageView = (TouchImageView)view.findViewById(R.id.imageView_PicView);
        this.spinner = (ProgressBar)view.findViewById(R.id.progressBar);
        if(savedInstanceState != null && file == null)
        {
            file = new File((String) savedInstanceState.get(ScreenSlideFragment.SAVESTATE_FILEPATH));
        }
        pager = (ViewPager)view.findViewById(R.id.pager);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    Log.d("ImageFragment", "onLongPress");
                        activity.showHideBars();
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });
        this.playVideo = (Button)view.findViewById(R.id.button_playvideo);
        playVideo.setVisibility(View.GONE);

    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        if (file != null && file.getAbsolutePath() != null)
            outState.putString(ScreenSlideFragment.SAVESTATE_FILEPATH, file.getAbsolutePath());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (file != null) {
            spinner.post(new Runnable() {
                @Override
                public void run() {
                    fadeout();
                    spinner.setVisibility(View.VISIBLE);
                }
            });
            new Thread(new Runnable() {
                @Override
                public void run() {
                    loadImage();
                }
            }).start();
        }
    }

    private void loadImage()
    {

        final Bitmap response = activity.getBitmap(file);
        imageView.post(new Runnable() {
            @Override
            public void run()
            {
                fadein();
                imageView.setImageBitmap(response);
                if (response == null)
                    workDone.onWorkDone(false, file);
                else
                    workDone.onWorkDone(true, file);
            }
        });
    }


    private void fadeout()
    {
        imageView.animate().alpha(0f).setDuration(animationTime).setListener(null);
        spinner.animate().alpha(1f).setDuration(animationTime).setListener(null);
    }

    private void fadein()
    {
        spinner.animate().alpha(0f).setDuration(animationTime).setListener(null);
        imageView.animate().alpha(1f).setDuration(animationTime).setListener(null);
    }

    interface WorkeDoneInterface
    {
        void onWorkDone(boolean success, File file);
    }



    WorkeDoneInterface workDone = new WorkeDoneInterface() {
        @Override
        public void onWorkDone(final boolean success, final File file)
        {
            playVideo.post(new Runnable() {
                @Override
                public void run() {
                    if (success)
                    {
                        if (file.getAbsolutePath().endsWith(".mp4")) {
                            playVideo.setVisibility(View.VISIBLE);
                            playVideo.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {

                                    Uri uri = Uri.fromFile(file);
                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                    i.setDataAndType(uri, "video/*");
                                    startActivity(i);
                                }
                            });
                        }
                        else if (!file.getAbsolutePath().endsWith(".mp4")) {
                            playVideo.setVisibility(View.GONE);
                        }
                    }
                    else
                    {
                        playVideo.setVisibility(View.GONE);
                    }
                }
            });

        }
    };

}
