package tv.pokemon.activity;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.HashMap;
import java.util.Map;

import tv.pokemon.R;
import tv.pokemon.model.Channel;
import tv.pokemon.model.Channel.Episode;

public class BrowseFragment extends android.support.v17.leanback.app.BrowseFragment
        implements LoaderManager.LoaderCallbacks<Object> {

    private static final int CATEGORY_LOADER = 123; // Unique ID for Category Loader.

    private ArrayObjectAdapter mCategoryRowAdapter;
    private Map<Integer, ArrayObjectAdapter> mEpisodeAdapters;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {

        super.onActivityCreated(savedInstanceState);

        setOnItemViewClickedListener(new ItemViewClickedListener());

        prepareEntranceTransition();

        mCategoryRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setAdapter(mCategoryRowAdapter);
    }

    @Override
    public void onAttach(Activity activity) {

        super.onAttach(activity);

        mEpisodeAdapters = new HashMap<>();

        getLoaderManager().initLoader(CATEGORY_LOADER, null, this);
    }

    @Override
    public void onResume() {

        super.onResume();

//        HeadersFragment headersFragment = getHeadersFragment();
//        headersFragment.setOnHeaderViewSelectedListener(new HeadersFragment.OnHeaderViewSelectedListener() {
//            @Override
//            public void onHeaderSelected(RowHeaderPresenter.ViewHolder viewHolder, Row row) {
//                if (viewHolder == null) return;
//                View view = viewHolder.view;
//            }
//        });
    }

    @Override
    public Loader<Object> onCreateLoader(int loaderId, final Bundle args) {

        if (loaderId == CATEGORY_LOADER) {

            return new AsyncTaskLoader<Object>(getActivity()) {

                private Channel[] mChannels;

                @Override
                protected void onStartLoading() {

                    super.onStartLoading();

                    if (takeContentChanged() || mChannels == null) {
                        forceLoad();
                    }
                }

                @Override
                public Object loadInBackground() {

                    Activity activity = getActivity();
                    Intent intent = activity.getIntent();
                    mChannels = (Channel[]) intent.getSerializableExtra("channels");

                    return mChannels;
                }
            };
        }

        return new AsyncTaskLoader<Object>(getActivity()) {

            private Episode[] mEpisodes;

            @Override
            protected void onStartLoading() {

                super.onStartLoading();

                if (takeContentChanged() || mEpisodes == null) {
                    forceLoad();
                }
            }

            @Override
            public Object loadInBackground() {
                Channel channel = (Channel) args.getSerializable("channel");
                mEpisodes = channel.episodes;
                return mEpisodes;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object object) {

        final int loaderId = loader.getId();

        if (loaderId == CATEGORY_LOADER) {

            // Every time we have to re-get the category loader, we must re-create the sidebar.
            mCategoryRowAdapter.clear();

            Channel[] channels = (Channel[]) object;

            // Iterate through each category entry and add it to the ArrayAdapter.
            for (Channel channel : channels) {

                String channelName = channel.channelName;
                int channelId = channel.channelId.hashCode();
                HeaderItem header = new HeaderItem(channelName);

                ArrayObjectAdapter existingAdapter = mEpisodeAdapters.get(channelId);

                if (existingAdapter == null) {

                    ArrayObjectAdapter episodeAdapter = new ArrayObjectAdapter(new CardPresenter());
                    mEpisodeAdapters.put(channelId, episodeAdapter);

                    ListRow row = new ListRow(header, episodeAdapter);
                    mCategoryRowAdapter.add(row);

                    Bundle args = new Bundle();
                    args.putSerializable("channel", channel);
                    getLoaderManager().initLoader(channelId, args, this);

                } else {

                    ListRow row = new ListRow(header, existingAdapter);
                    mCategoryRowAdapter.add(row);
                }
            }

            startEntranceTransition(); // TODO: Move startEntranceTransition to after all

        } else {

            mEpisodeAdapters.get(loaderId).clear();

            Episode[] episodes = (Episode[]) object;

            for (Episode episode : episodes) {
                mEpisodeAdapters.get(loaderId).add(episode);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {

        int loaderId = loader.getId();

        if (loaderId == CATEGORY_LOADER) {
            mCategoryRowAdapter.clear();
        } else {
            mEpisodeAdapters.get(loaderId).clear();
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {

        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder,
                                  Object item,
                                  RowPresenter.ViewHolder rowViewHolder,
                                  Row row) {

            if (item instanceof Episode) {

                Episode episode = (Episode) item;

                Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                intent.putExtra("episode", episode);
                startActivity(intent);
            }
        }
    }

    private class CardPresenter extends Presenter {

        private int mSelectedBackgroundColor = -1;
        private int mDefaultBackgroundColor = -1;
        private int mSelectedTextColor = -1;
        private int mDefaultTextColor = -1;
        private Drawable mDefaultCardImage;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {

            Resources resources = getResources();

            mDefaultBackgroundColor = resources.getColor(R.color.default_background);
            mSelectedBackgroundColor = resources.getColor(R.color.selected_background);

            mDefaultTextColor = resources.getColor(R.color.default_card_text);
            mSelectedTextColor = resources.getColor(R.color.selected_card_text);

            mDefaultCardImage = parent.getResources().getDrawable(R.drawable.default_card, null);

            ImageCardView cardView = new ImageCardView(parent.getContext()) {
                @Override
                public void setSelected(boolean selected) {
                    updateCardBackgroundColor(this, selected);
                    super.setSelected(selected);
                }
            };

            int width = resources.getDimensionPixelSize(R.dimen.card_width);
            int height = resources.getDimensionPixelSize(R.dimen.card_height);
            cardView.setMainImageDimensions(width, height);

            cardView.setFocusable(true);
            cardView.setFocusableInTouchMode(true);

            updateCardBackgroundColor(cardView, false);

            return new ViewHolder(cardView);
        }

        private void updateCardBackgroundColor(ImageCardView cardView, boolean selected) {

            int color = selected ? mSelectedBackgroundColor : mDefaultBackgroundColor;
            int colorText = selected ? mSelectedTextColor : mDefaultTextColor;

            TextView titleTextView = (TextView) cardView.findViewById(R.id.title_text);
            titleTextView.setTextColor(colorText);

            TextView contentTextView = (TextView) cardView.findViewById(R.id.content_text);
            contentTextView.setTextColor(colorText);
//            contentTextView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
//            contentTextView.setMarqueeRepeatLimit(-1);
//            contentTextView.setSelected(selected);

            // Both background colors should be set because the view's
            // background is temporarily visible during animations.
            cardView.setBackgroundColor(color);
            cardView.findViewById(R.id.info_field).setBackgroundColor(color);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {

            Episode episode = (Episode) item;

            String title = null;
            if (!TextUtils.isEmpty(episode.season)) {
                title = String.format("S%s|E%s", episode.season, episode.episode);
            }

            ImageCardView cardView = (ImageCardView) viewHolder.view;
            cardView.setTitleText(title);
            cardView.setContentText(episode.title);

            TextView titleTextView = (TextView) cardView.findViewById(R.id.title_text);
            titleTextView.setVisibility(TextUtils.isEmpty(title) ? View.GONE : View.VISIBLE);

            Glide.with(cardView.getContext())
                    .load(episode.images.smallUrl)
                    .error(mDefaultCardImage)
                    .into(cardView.getMainImageView());
        }

        @Override
        public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
            ImageCardView cardView = (ImageCardView) viewHolder.view;
            cardView.setMainImage(null);
        }
    }
}
