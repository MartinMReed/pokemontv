package tv.pokemon.activity;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.Html;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.Util;

import java.util.concurrent.TimeUnit;

import tv.pokemon.R;
import tv.pokemon.exoplayer.ExtractorRendererBuilder;
import tv.pokemon.exoplayer.VideoPlayer;
import tv.pokemon.model.Channel.Episode;

import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS;

public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackOverlayFragment
        implements TextureView.SurfaceTextureListener, VideoPlayer.Listener {

    private Episode mEpisode;

    private ArrayObjectAdapter mRowsAdapter;
    private MediaSessionCompat mSession;

    private PlaybackControlGlue mGlue;

    private VideoPlayer mPlayer;

    private MediaController mMediaController;
    private MediaController.Callback mMediaControllerCallback;

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);

        Intent intent = activity.getIntent();
        mEpisode = (Episode) intent.getSerializableExtra("episode");

        TextureView textureView = (TextureView) activity.findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);

        setBackgroundType(BG_LIGHT);

        createMediaSession();
    }

    @Override
    public void onStart() {

        super.onStart();

        Intent intent = new Intent(getActivity().getIntent());
        PendingIntent pendingIntent = PendingIntent.getActivity(getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pendingIntent);

        mGlue = new PlaybackControlGlue(getActivity(), this);
        PlaybackControlsRowPresenter controlsRowPresenter = mGlue.createControlsRowAndPresenter();
        PlaybackControlsRow controlsRow = mGlue.getControlsRow();
        mMediaControllerCallback = mGlue.createMediaControllerCallback();

        mMediaController = getActivity().getMediaController();
        mMediaController.registerCallback(mMediaControllerCallback);

        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
        presenterSelector.addClassPresenter(PlaybackControlsRow.class, controlsRowPresenter);

        mRowsAdapter = new ArrayObjectAdapter(presenterSelector);
        mRowsAdapter.add(controlsRow);
        updatePlaybackRow();
        setAdapter(mRowsAdapter);
    }

    @Override
    public void onResume() {

        super.onResume();

        if (mPlayer == null) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if (mPlayer == null) {
                        mPlayer = new VideoPlayer(getRendererBuilder());
                        mPlayer.addListener(PlaybackOverlayFragment.this);
                        mPlayer.seekTo(0L);
                        mPlayer.prepare();
                        mPlayer.setPlayWhenReady(true);
                    }
                }
            });
        }
    }

    @Override
    public void onPause() {

        super.onPause();

        if (mGlue.isMediaPlaying()) {
            boolean isVisibleBehind = getActivity().requestVisibleBehind(true);
            if (!isVisibleBehind) pause();
        } else {
            getActivity().requestVisibleBehind(false);
        }
    }

    @Override
    public void onStop() {

        super.onStop();

        mSession.release();
        releasePlayer();
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }

        mSession.release();
        releasePlayer();
    }

    private void createMediaSession() {

        if (mSession == null) {

            mSession = new MediaSessionCompat(getActivity(), getString(R.string.app_name));
            mSession.setCallback(new MediaSessionCallback());
            mSession.setFlags(FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS);
            mSession.setActive(true);

            try {
                FragmentActivity activity = (FragmentActivity) getActivity();
                activity.setSupportMediaController(new MediaControllerCompat(activity, mSession.getSessionToken()));
                setPlaybackState(PlaybackState.STATE_NONE);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void setPosition(long position) {
        if (position > mPlayer.getDuration()) {
            mPlayer.seekTo(mPlayer.getDuration());
        } else if (position < 0) {
            mPlayer.seekTo(0L);
        } else {
            mPlayer.seekTo(position);
        }
    }

    public long getBufferedPosition() {
        if (mPlayer == null) return 0L;
        return mPlayer.getBufferedPosition();
    }

    public long getCurrentPosition() {
        if (mPlayer == null) return 0L;
        return mPlayer.getCurrentPosition();
    }

    public long getDuration() {
        if (mPlayer == null) return ExoPlayer.UNKNOWN_TIME;
        return mPlayer.getDuration();
    }

    public CharSequence getMediaTitle() {
        return mEpisode.title;
    }

    public CharSequence getMediaSubtitle() {
        return Html.fromHtml(mEpisode.description).toString();
    }

    private void play() {

        if (mPlayer == null) {
            setPlaybackState(PlaybackState.STATE_NONE);
            return;
        }

        if (!mGlue.isMediaPlaying()) {
            mPlayer.getPlayerControl().start();
            setPlaybackState(PlaybackState.STATE_PLAYING);
        }
    }

    private void pause() {

        if (mPlayer == null) {
            setPlaybackState(PlaybackState.STATE_NONE);
            return;
        }

        if (mGlue.isMediaPlaying()) {
            mPlayer.getPlayerControl().pause();
            setPlaybackState(PlaybackState.STATE_PAUSED);
        }
    }

    void updatePlaybackRow() {
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
    }

    private VideoPlayer.RendererBuilder getRendererBuilder() {

        String userAgent = Util.getUserAgent(getActivity(), "ExoVideoPlayer");
        Uri contentUri = Uri.parse(mEpisode.offlineUrl);
        int contentType = Util.inferContentType(contentUri.getLastPathSegment());

        switch (contentType) {
            case Util.TYPE_OTHER: {
                return new ExtractorRendererBuilder(getActivity(), userAgent, contentUri);
            }
            default: {
                throw new IllegalStateException("Unsupported type: " + contentType);
            }
        }
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {

        switch (playbackState) {

            case ExoPlayer.STATE_READY: {

                if (playWhenReady) {
                    setPlaybackState(PlaybackState.STATE_PLAYING);
                }

                MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
                metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mPlayer.getDuration());
                mSession.setMetadata(metadataBuilder.build());

                break;
            }

            case ExoPlayer.STATE_ENDED: {
                Activity activity = getActivity();
                if (!activity.isFinishing()) {
                    activity.finish();
                }
                break;
            }

            case ExoPlayer.STATE_BUFFERING:
            case ExoPlayer.STATE_IDLE:
            case ExoPlayer.STATE_PREPARING:
            default:
                break;
        }
    }

    @Override
    public void onError(Exception e) {
        Log.e(PlaybackOverlayFragment.class.getSimpleName(), "An error occurred: " + e);
    }

    @Override
    public void onVideoSizeChanged(int width, int height,
                                   int unappliedRotationDegrees,
                                   float pixelWidthHeightRatio) {
        // do nothing
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (mPlayer != null) {
            mPlayer.setSurface(new Surface(surfaceTexture));
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // do nothing
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // do nothing
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mPlayer != null) {
            mPlayer.blockingClearSurface();
        }
        return true;
    }

    private int getPlaybackState() {

        Activity activity = getActivity();

        if (activity != null) {
            PlaybackState state = activity.getMediaController().getPlaybackState();
            if (state != null) {
                return state.getState();
            } else {
                return PlaybackState.STATE_NONE;
            }
        }

        return PlaybackState.STATE_NONE;
    }

    private void setPlaybackState(int state) {

        long currPosition = getCurrentPosition();

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder();
        stateBuilder.setActions(getAvailableActions(state));
        stateBuilder.setState(state, currPosition, 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    @PlaybackStateCompat.Actions
    private long getAvailableActions(int nextState) {
        return PlaybackState.ACTION_PLAY_PAUSE
                | PlaybackState.ACTION_FAST_FORWARD
                | PlaybackState.ACTION_REWIND;
    }

    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            play();
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            // do nothing
        }

        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onSkipToNext() {
            // do nothing
        }

        @Override
        public void onSkipToPrevious() {
            // do nothing
        }

        @Override
        public void onFastForward() {
            if (mPlayer.getDuration() != ExoPlayer.UNKNOWN_TIME) {
                setPosition(mPlayer.getCurrentPosition() + TimeUnit.SECONDS.toMillis(10));
            }
        }

        @Override
        public void onRewind() {
            setPosition(mPlayer.getCurrentPosition() - TimeUnit.SECONDS.toMillis(10));
        }

        @Override
        public void onSeekTo(long position) {
            setPosition(position);
        }
    }
}
