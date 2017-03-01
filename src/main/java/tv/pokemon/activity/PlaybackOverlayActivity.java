package tv.pokemon.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.WindowManager;

import tv.pokemon.R;

public class PlaybackOverlayActivity extends FragmentActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        super.onNewIntent(intent);

        // Change the intent returned by {@link #getIntent()}.
        // Note that getIntent() only returns the initial intent that created the activity
        // but we need the latest intent that contains the information of the latest video
        // that the user is selected.
        setIntent(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

    @Override
    public void onVisibleBehindCanceled() {
        getMediaController().getTransportControls().pause();
        super.onVisibleBehindCanceled();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
            getMediaController().getTransportControls().skipToNext();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
            getMediaController().getTransportControls().skipToPrevious();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
            getMediaController().getTransportControls().rewind();
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            getMediaController().getTransportControls().fastForward();
        }

        return super.onKeyDown(keyCode, event);
    }
}
