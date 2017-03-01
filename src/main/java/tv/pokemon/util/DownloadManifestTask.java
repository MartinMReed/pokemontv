package tv.pokemon.util;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

import tv.pokemon.model.Channel;

public class DownloadManifestTask<Params, Progress> extends AsyncTask<Params, Progress, Channel[]> {

    private static final String URL = "http://www.pokemon.com/api/pokemontv/channels?region=us";

    protected final WeakReference<Activity> mActivity;

    public DownloadManifestTask(Activity activity) {
        mActivity = new WeakReference<>(activity);
    }

    @Override
    protected Channel[] doInBackground(Params... params) {

        Activity activity = mActivity.get();
        if (activity == null) return null;

        return downloadChannels();
    }

    private Channel[] downloadChannels() throws IllegalStateException {

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        Reader reader = null;

        try {

            URL url = new URL(URL);

            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("Invalid HTTP status code: " + status);
            }

            inputStream = connection.getInputStream();
            reader = new InputStreamReader(inputStream);

            Gson gson = new GsonBuilder().create();
            Channel[] channels = gson.fromJson(reader, new TypeToken<Channel[]>() {
            }.getType());

            if (channels == null || channels.length == 0) {
                throw new IOException("Missing channels from download.");
            }

            return channels;

        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            safeClose(reader);
            safeClose(inputStream);
            safeClose(connection);
        }
    }

    private static void safeClose(HttpURLConnection connection) {
        if (connection == null) return;
        try {
            connection.disconnect();
        } catch (Exception e) {
            Log.e(DownloadManifestTask.class.getSimpleName(), "Unable to close " + connection.getClass().getSimpleName(), e);
        }
    }

    private static void safeClose(AutoCloseable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (Exception e) {
            Log.e(DownloadManifestTask.class.getSimpleName(), "Unable to close " + closeable.getClass().getSimpleName(), e);
        }
    }
}
