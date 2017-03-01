package tv.pokemon.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Calendar;

import tv.pokemon.R;
import tv.pokemon.model.Channel;

public class SplashActivity extends Activity {

    private DownloadManifestTask mDownloadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_splash);

        int year = Calendar.getInstance().get(Calendar.YEAR);

        TextView textView = (TextView) findViewById(R.id.splash_legal);
        textView.setText(getString(R.string.splash_legal, year));

        DisplayMetrics localDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(localDisplayMetrics);
        int screenHeight = localDisplayMetrics.heightPixels;
        final int animateDistance = screenHeight / 2;

        View pokeball = findViewById(R.id.splash_pokeball);

        ViewTreeObserver localViewTreeObserver = pokeball.getViewTreeObserver();
        localViewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            public void onGlobalLayout() {

                View view = findViewById(R.id.splash_pokeball);

                ViewTreeObserver localViewTreeObserver = view.getViewTreeObserver();
                if (localViewTreeObserver.isAlive()) {
                    localViewTreeObserver.removeOnGlobalLayoutListener(this);
                }

                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);

                FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) view.getLayoutParams();
                layoutParams.setMargins(0, animateDistance - rect.centerY(), 0, 0);

                view.setVisibility(View.VISIBLE);
            }
        });

        ViewCompat.animate(findViewById(R.id.splash_pikachu))
                .translationY((screenHeight / 3) - animateDistance)
                .setStartDelay(300L)
                .setDuration(500L)
                .setInterpolator(new DecelerateInterpolator(1.2F))
                .start();

        ViewCompat.animate(findViewById(R.id.splash_pokeball))
                .translationY(((screenHeight * 2) / 3) - animateDistance)
                .setStartDelay(300L)
                .setDuration(500L)
                .setInterpolator(new DecelerateInterpolator(1.2F))
                .start();
    }

    @Override
    protected void onStart() {

        super.onStart();

        mDownloadTask = new DownloadManifestTask(SplashActivity.this);
        mDownloadTask.execute();
    }

    @Override
    protected void onStop() {

        super.onStop();

        if (mDownloadTask != null && mDownloadTask.getStatus() != AsyncTask.Status.FINISHED) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
    }

    private static final class DownloadManifestTask extends tv.pokemon.util.DownloadManifestTask<Void, Integer> {

        private static final long MIN_DURATION = 1500L;

        public DownloadManifestTask(Activity activity) {
            super(activity);
        }

        @Override
        protected Channel[] doInBackground(Void... params) {

            long startTime = System.currentTimeMillis();

            try {
                return super.doInBackground(params);
            } catch (Exception e) {
                Log.e(SplashActivity.class.getSimpleName(), "Unable to download manifest.", e);
                return null;
            } finally {
                waitMinimumDuration(startTime);
            }
        }

        @Override
        protected void onPostExecute(Channel[] channels) {

            final Activity activity = mActivity.get();
            if (activity == null) return;

            super.onPostExecute(channels);

            if (activity.isFinishing()) return;

            if (channels == null) {

                new AlertDialog.Builder(activity)
                        .setTitle(R.string.manifest_error_dialog_title)
                        .setMessage(R.string.manifest_error_dialog_message)
                        .setPositiveButton(R.string.manifest_error_dialog_positive, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                activity.finish();
                            }
                        })
                        .create().show();

                return;
            }

            Intent intent = new Intent(activity, BrowseActivity.class);
            intent.putExtra("channels", channels);

            activity.startActivity(intent);
            activity.finish();
        }

        private void waitMinimumDuration(long startTime) {
            long duration;
            while ((duration = System.currentTimeMillis() - startTime) < MIN_DURATION) {
                try {
                    Thread.sleep(MIN_DURATION - duration);
                } catch (InterruptedException e) {
                    // do nothing
                }
            }
        }
    }
}
