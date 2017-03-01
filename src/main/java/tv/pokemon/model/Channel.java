package tv.pokemon.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Channel implements Serializable {

    @SerializedName("channel_id")
    public String channelId;

    @SerializedName("channel_name")
    public String channelName;

    @SerializedName("media")
    public Episode[] episodes;

    public class Episode implements Serializable {

        public String id;
        public Object size;
        public int count;
        public int rating;

        public String season;
        public String episode;

        public String title;
        public String description;

        @SerializedName("is_country_whitelist")
        public boolean countryWhitelist;
        @SerializedName("country_codes")
        public String[] countryCodes;

        @SerializedName("offline_url")
        public String offlineUrl;
        @SerializedName("stream_url")
        public String streamUrl;
        @SerializedName("captions")
        public String captionsUrl;

        public EpisodeImages images;

        public class EpisodeImages implements Serializable {

            @SerializedName("small")
            public String smallUrl;
            @SerializedName("large")
            public String largeUrl;
            @SerializedName("medium")
            public String mediumUrl;
        }
    }
}
