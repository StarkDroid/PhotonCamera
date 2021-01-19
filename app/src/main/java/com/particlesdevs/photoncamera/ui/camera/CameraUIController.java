package com.particlesdevs.photoncamera.ui.camera;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import com.particlesdevs.photoncamera.R;
import com.particlesdevs.photoncamera.api.CameraMode;
import com.particlesdevs.photoncamera.app.PhotonCamera;
import com.particlesdevs.photoncamera.capture.CaptureController;
import com.particlesdevs.photoncamera.control.CountdownTimer;
import com.particlesdevs.photoncamera.settings.PreferenceKeys;
import com.particlesdevs.photoncamera.ui.camera.views.FlashButton;
import com.particlesdevs.photoncamera.ui.camera.views.TimerButton;

/**
 * Implementation of {@link CameraUIView.CameraUIEventsListener}
 * <p>
 * Responsible for converting user inputs into actions
 */
public final class CameraUIController implements CameraUIView.CameraUIEventsListener {
    private static final String TAG = "CameraUIController";
    private final CameraFragment mCameraFragment;
    private CountDownTimer countdownTimer;
    private View shutterButton;

    public CameraUIController(CameraFragment cameraFragment) {
        this.mCameraFragment = cameraFragment;
    }

    private void restartCamera() {
        resetTimer();
        this.mCameraFragment.getCaptureController().restartCamera();
    }

    @Override
    public void onClick(View view) {
        CaptureController captureController = mCameraFragment.getCaptureController();
        switch (view.getId()) {
            case R.id.shutter_button:
                shutterButton = view;
                switch (PhotonCamera.getSettings().selectedMode) {
                    case PHOTO:
                    case NIGHT:
                        if (view.isHovered()) resetTimer();
                        else startTimer();
                        break;
                    case UNLIMITED:
                        if (!captureController.onUnlimited) {
                            captureController.callUnlimitedStart();
                            view.setActivated(false);
                        } else {
                            captureController.callUnlimitedEnd();
                            view.setActivated(true);
                        }
                        break;
                    case VIDEO:
                        if (!captureController.mIsRecordingVideo) {
                            captureController.VideoStart();
                            view.setActivated(false);
                        } else {
                            captureController.VideoEnd();
                            view.setActivated(true);
                        }
                        break;
                }
                break;
            case R.id.settings_button:
                mCameraFragment.launchSettings();
                break;

            case R.id.hdrx_toggle_button:
                PreferenceKeys.setHdrX(!PreferenceKeys.isHdrXOn());
                if (PreferenceKeys.isHdrXOn())
                    CaptureController.setTargetFormat(CaptureController.RAW_FORMAT);
                else
                    CaptureController.setTargetFormat(CaptureController.YUV_FORMAT);
                mCameraFragment.showSnackBar(mCameraFragment.getString(R.string.hdrx) + ':' + onOff(PreferenceKeys.isHdrXOn()));
                restartCamera();
                break;

            case R.id.gallery_image_button:
                mCameraFragment.launchGallery();
                break;

            case R.id.eis_toggle_button:
                PreferenceKeys.setEisPhoto(!PreferenceKeys.isEisPhotoOn());
                mCameraFragment.showSnackBar(mCameraFragment.getString(R.string.eis_toggle_text) + ':' + onOff(PreferenceKeys.isEisPhotoOn()));
                break;

            case R.id.fps_toggle_button:
                PreferenceKeys.setFpsPreview(!PreferenceKeys.isFpsPreviewOn());
                mCameraFragment.showSnackBar(mCameraFragment.getString(R.string.fps_60_toggle_text) + ':' + onOff(PreferenceKeys.isFpsPreviewOn()));
                break;

            case R.id.quad_res_toggle_button:
                PreferenceKeys.setQuadBayer(!PreferenceKeys.isQuadBayerOn());
                mCameraFragment.showSnackBar(mCameraFragment.getString(R.string.quad_bayer_toggle_text) + ':' + onOff(PreferenceKeys.isQuadBayerOn()));
                restartCamera();
                break;

            case R.id.flip_camera_button:
                view.animate().rotationBy(180).setDuration(450).start();
                mCameraFragment.findViewById(R.id.texture).animate().rotationBy(360).setDuration(450).start();
                PreferenceKeys.setCameraID(mCameraFragment.cycler(PreferenceKeys.getCameraID()));
                restartCamera();
                break;
            case R.id.grid_toggle_button:
                PreferenceKeys.setGridValue((PreferenceKeys.getGridValue() + 1) % view.getResources().getStringArray(R.array.vf_grid_entryvalues).length);
                view.setSelected(PreferenceKeys.getGridValue() != 0);
                mCameraFragment.invalidateSurfaceView();
                break;

            case R.id.flash_button:
                PreferenceKeys.setAeMode((PreferenceKeys.getAeMode() + 1) % 4); //cycles in 0,1,2,3
                ((FlashButton) view).setFlashValueState(PreferenceKeys.getAeMode());
                captureController.setPreviewAEModeRebuild();
                break;

            case R.id.countdown_timer_button:
                PreferenceKeys.setCountdownTimerIndex((PreferenceKeys.getCountdownTimerIndex() + 1) % view.getResources().getIntArray(R.array.countdowntimer_entryvalues).length);
                ((TimerButton) view).setTimerIconState(PreferenceKeys.getCountdownTimerIndex());
                break;
        }
    }

    private int getTimerValue(Context context) {
        int[] timerValues = context.getResources().getIntArray(R.array.countdowntimer_entryvalues);
        return timerValues[PreferenceKeys.getCountdownTimerIndex()];
    }

    private void startTimer() {
        if (shutterButton != null) {
            shutterButton.setHovered(true);
            countdownTimer = new CountdownTimer(
                    mCameraFragment.findViewById(R.id.frameTimer),
                    getTimerValue(shutterButton.getContext()) * 1000L, 1000,
                    this::onTimerFinished).start();
        }
    }

    private void resetTimer() {
        if (countdownTimer != null) countdownTimer.cancel();
        if (shutterButton != null) shutterButton.setHovered(false);
    }

    @Override
    public void onAuxButtonClicked(String id) {
        Log.d(TAG, "onAuxButtonClicked() called with: id = [" + id + "]");
        PreferenceKeys.setCameraID(String.valueOf(id));  //i = RadioButton's resource ID
        restartCamera();

    }

    @Override
    public void onCameraModeChanged(CameraMode cameraMode) {
        PreferenceKeys.setCameraModeOrdinal(cameraMode.ordinal());
        Log.d(TAG, "onCameraModeChanged() called with: cameraMode = [" + cameraMode + "]");
        switch (cameraMode) {
            case PHOTO:
            case NIGHT:
            case UNLIMITED:
            default:
                break;
            case VIDEO:
                PreferenceKeys.setCameraModeOrdinal(CameraMode.PHOTO.ordinal()); //since Video Mode is broken at the moment
                break;
        }
        restartCamera();
    }

    private String onOff(boolean value) {
        return value ? "On" : "Off";
    }

    public void onTimerFinished() {
        shutterButton.setHovered(false);
        shutterButton.setActivated(false);
        shutterButton.setClickable(false);
        mCameraFragment.getCaptureController().takePicture();
    }
}
