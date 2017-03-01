package tv.pokemon.activity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.view.KeyEvent;
import android.view.View;

class PlaybackControlGlue extends android.support.v17.leanback.app.PlaybackControlGlue {

    private static final int[] SEEK_SPEEDS = {0};
    private static final int DEFAULT_UPDATE_PERIOD = 500;
    private static final int UPDATE_PERIOD = 16;

    private final MediaController mMediaController;
    private final MediaController.TransportControls mTransportControls;

    private PlaybackOverlayFragment mFragment;
    private PlaybackControlsRow.FastForwardAction mFastForwardAction;
    private PlaybackControlsRow.RewindAction mRewindAction;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mUpdateProgressRunnable;

    public PlaybackControlGlue(Context context, PlaybackOverlayFragment fragment) {

        super(context, fragment, SEEK_SPEEDS);

        mFragment = fragment;

        mMediaController = mFragment.getActivity().getMediaController();
        mTransportControls = mMediaController.getTransportControls();
    }

    @Override
    public PlaybackControlsRowPresenter createControlsRowAndPresenter() {

        PlaybackControlsRowPresenter presenter = super.createControlsRowAndPresenter();

        mFastForwardAction = (PlaybackControlsRow.FastForwardAction) getPrimaryActionsAdapter()
                .lookup(ACTION_FAST_FORWARD);

        mRewindAction = (PlaybackControlsRow.RewindAction) getPrimaryActionsAdapter()
                .lookup(ACTION_REWIND);

        return presenter;
    }

    @Override
    public void enableProgressUpdating(boolean enable) {
        mHandler.removeCallbacks(mUpdateProgressRunnable);
        if (enable) mHandler.post(mUpdateProgressRunnable);
    }

    @Override
    public int getUpdatePeriod() {

        View view = mFragment.getView();
        int totalTime = getControlsRow().getTotalTime();

        if (view == null || totalTime <= 0 || view.getWidth() == 0) {
            return DEFAULT_UPDATE_PERIOD;
        }

        return Math.max(UPDATE_PERIOD, totalTime / view.getWidth());
    }

    @Override
    public void updateProgress() {

        if (mUpdateProgressRunnable == null) {

            mUpdateProgressRunnable = new Runnable() {

                @Override
                public void run() {

                    int totalTime = getControlsRow().getTotalTime();
                    int currentTime = getCurrentPosition();
                    getControlsRow().setCurrentTime(currentTime);

                    int progress = (int) mFragment.getBufferedPosition();
                    getControlsRow().setBufferedProgress(progress);

                    if (totalTime > 0 && totalTime <= currentTime) {
                        stopProgressAnimation();
                    } else {
                        updateProgress();
                    }
                }
            };
        }

        mHandler.postDelayed(mUpdateProgressRunnable, getUpdatePeriod());
    }

    @Override
    public boolean hasValidMedia() {
        return true;
    }

    @Override
    public boolean isMediaPlaying() {

        if (mMediaController.getPlaybackState() == null) {
            return false;
        }

        int state = mMediaController.getPlaybackState().getState();
        return (state == PlaybackState.STATE_BUFFERING
                || state == PlaybackState.STATE_CONNECTING
                || state == PlaybackState.STATE_PLAYING);
    }

    @Override
    public CharSequence getMediaTitle() {
        return mFragment.getMediaTitle();
    }

    @Override
    public CharSequence getMediaSubtitle() {
        return mFragment.getMediaSubtitle();
    }

    @Override
    public int getMediaDuration() {
        return (int) mFragment.getDuration();
    }

    @Override
    public Drawable getMediaArt() {
        return null;
    }

    @Override
    public long getSupportedActions() {
        return ACTION_PLAY_PAUSE
                | ACTION_FAST_FORWARD
                | ACTION_REWIND;
    }

    @Override
    public int getCurrentSpeedId() {
        return isMediaPlaying() ? PLAYBACK_SPEED_NORMAL : PLAYBACK_SPEED_PAUSED;
    }

    @Override
    public int getCurrentPosition() {
        return (int) mFragment.getCurrentPosition();
    }

    @Override
    protected void startPlayback(int speed) {
        if (getCurrentSpeedId() == speed) return;
        mTransportControls.play();
    }

    @Override
    protected void pausePlayback() {
        mTransportControls.pause();
    }

    @Override
    protected void skipToNext() {
        mTransportControls.skipToNext();
    }

    @Override
    protected void skipToPrevious() {
        mTransportControls.skipToPrevious();
    }

    @Override
    protected void onRowChanged(PlaybackControlsRow row) {
        // do nothing
    }

    @Override
    public void onActionClicked(Action action) {
        if (action == mFastForwardAction) {
            mTransportControls.fastForward();
        } else if (action == mRewindAction) {
            mTransportControls.rewind();
        } else {
            super.onActionClicked(action);
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKey(v, keyCode, event);
        }

        SparseArrayObjectAdapter primaryActionsAdapter = getPrimaryActionsAdapter();
        Action action = getControlsRow().getActionForKeyCode(primaryActionsAdapter, keyCode);

        if (action != null) {
            if (action == mRewindAction
                    || action == mFastForwardAction) {
                onActionClicked(action);
                return true;
            }
        }

        return super.onKey(v, keyCode, event);
    }

    private void stopProgressAnimation() {
        if (mHandler != null && mUpdateProgressRunnable != null) {
            mHandler.removeCallbacks(mUpdateProgressRunnable);
            mUpdateProgressRunnable = null;
        }
    }

    private SparseArrayObjectAdapter getPrimaryActionsAdapter() {
        return (SparseArrayObjectAdapter) getControlsRow().getPrimaryActionsAdapter();
    }

    public MediaController.Callback createMediaControllerCallback() {
        return new MediaControllerCallback();
    }

    private class MediaControllerCallback extends MediaController.Callback {

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {

            int nextState = state.getState();
            if (nextState != PlaybackState.STATE_NONE) {
                updateProgress();
            }

            onStateChanged();
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            PlaybackControlGlue.this.onMetadataChanged();
            mFragment.updatePlaybackRow();
        }
    }
}
