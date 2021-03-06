package org.nuclearfog.twidda.adapter;

import android.graphics.Color;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.squareup.picasso.Picasso;

import org.nuclearfog.tag.Tagger;
import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.items.Tweet;
import org.nuclearfog.twidda.backend.items.User;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

import static android.graphics.PorterDuff.Mode.SRC_IN;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.RecyclerView.NO_ID;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static org.nuclearfog.twidda.backend.utils.StringTools.getTimeString;

/**
 * Adapter class for tweet list
 *
 * @author nuclearfog
 * @see org.nuclearfog.twidda.fragment.TweetFragment
 */
public class TweetAdapter extends Adapter<ViewHolder> {

    /**
     * index of {@link #loadingIndex} if no index is defined
     */
    private static final int NO_INDEX = -1;

    /**
     * View type for a tweet item
     */
    private static final int VIEW_TWEET = 0;

    /**
     * View type for a placeholder item
     */
    private static final int VIEW_GAP = 1;

    /**
     * Minimum count of new Tweets to insert a placeholder.
     */
    private static final int MIN_COUNT = 2;

    private TweetClickListener itemClickListener;
    private GlobalSettings settings;

    private final List<Tweet> tweets = new ArrayList<>();
    private NumberFormat formatter = NumberFormat.getIntegerInstance();
    private int loadingIndex = NO_INDEX;


    public TweetAdapter(GlobalSettings settings, TweetClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
        this.settings = settings;
    }

    /**
     * Insert data at specific index of the list
     *
     * @param data  Tweet data
     * @param index position to insert
     */
    @MainThread
    public void insertAt(@NonNull List<Tweet> data, int index) {
        disableLoading();
        if (data.size() > MIN_COUNT) {
            if (tweets.isEmpty() || tweets.get(index) != null) {
                // Add placeholder
                tweets.add(index, null);
                notifyItemInserted(index);
            }
        } else {
            if (!tweets.isEmpty() && tweets.get(index) == null) {
                // remove placeholder
                tweets.remove(index);
                notifyItemRemoved(index);
            }
        }
        if (!data.isEmpty()) {
            tweets.addAll(index, data);
            notifyItemRangeInserted(index, data.size());
        }
    }

    /**
     * Replace all items in the list
     *
     * @param data tweet data
     */
    @MainThread
    public void replaceAll(@NonNull List<Tweet> data) {
        tweets.clear();
        tweets.addAll(data);
        if (data.size() > MIN_COUNT) {
            tweets.add(null);
        }
        loadingIndex = NO_INDEX;
        notifyDataSetChanged();
    }

    /**
     * update a single item
     *
     * @param tweet updated tweet
     */
    @MainThread
    public void updateItem(Tweet tweet) {
        int index = tweets.indexOf(tweet);
        if (index >= 0) {
            tweets.set(index, tweet);
            notifyItemChanged(index);
        }
    }

    /**
     * Remove specific tweet from list
     *
     * @param id ID of the tweet
     */
    @MainThread
    public void remove(long id) {
        for (int pos = 0; pos < tweets.size(); pos++) {
            Tweet tweet = tweets.get(pos);
            if (tweet != null && tweet.getId() == id) {
                tweets.remove(pos);
                notifyItemRemoved(pos);
                break;
            }
        }
    }

    /**
     * removes all items from adapter
     */
    @MainThread
    public void clear() {
        tweets.clear();
        notifyDataSetChanged();
    }

    /**
     * check if list is empty
     *
     * @return true if list is empty
     */
    public boolean isEmpty() {
        return tweets.isEmpty();
    }

    /**
     * disable placeholder load animation
     */
    public void disableLoading() {
        if (loadingIndex != NO_INDEX) {
            int oldIndex = loadingIndex;
            loadingIndex = NO_INDEX;
            notifyItemChanged(oldIndex);
        }
    }

    @Override
    public long getItemId(int index) {
        Tweet tweet = tweets.get(index);
        if (tweet != null)
            return tweet.getId();
        return NO_ID;
    }


    @Override
    public int getItemCount() {
        return tweets.size();
    }


    @Override
    public int getItemViewType(int index) {
        if (tweets.get(index) == null)
            return VIEW_GAP;
        return VIEW_TWEET;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TWEET) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tweet, parent, false);
            final TweetHolder vh = new TweetHolder(v, settings);
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getLayoutPosition();
                    Tweet tweet = tweets.get(position);
                    if (position != NO_POSITION && tweet != null) {
                        itemClickListener.onTweetClick(tweet);
                    }
                }
            });
            return vh;
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_placeholder, parent, false);
            final PlaceHolder vh = new PlaceHolder(v, settings);
            vh.loadBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getLayoutPosition();
                    if (position != NO_POSITION) {
                        long sinceId = 0;
                        long maxId = 0;
                        if (position == 0) {
                            sinceId = tweets.get(position + 1).getId();
                        } else if (position == tweets.size() - 1) {
                            maxId = tweets.get(position - 1).getId();
                        } else {
                            sinceId = tweets.get(position + 1).getId();
                            maxId = tweets.get(position - 1).getId();
                        }
                        itemClickListener.onHolderClick(sinceId, maxId, position);
                        vh.loadCircle.setVisibility(VISIBLE);
                        vh.loadBtn.setVisibility(INVISIBLE);
                        loadingIndex = position;
                    }
                }
            });
            return vh;
        }
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int index) {
        Tweet tweet = tweets.get(index);
        if (holder instanceof TweetHolder && tweet != null) {
            TweetHolder vh = (TweetHolder) holder;
            User user = tweet.getUser();
            if (tweet.getEmbeddedTweet() != null) {
                vh.textViews[5].setText(user.getScreenname());
                vh.textViews[5].setVisibility(VISIBLE);
                vh.rtUser.setVisibility(VISIBLE);
                tweet = tweet.getEmbeddedTweet();
                user = tweet.getUser();
            } else {
                vh.textViews[5].setVisibility(INVISIBLE);
                vh.rtUser.setVisibility(INVISIBLE);
            }
            Spanned text = Tagger.makeTextWithLinks(tweet.getTweet(), settings.getHighlightColor());
            vh.textViews[2].setText(text);
            vh.textViews[0].setText(user.getUsername());
            vh.textViews[1].setText(user.getScreenname());
            vh.textViews[3].setText(formatter.format(tweet.getRetweetCount()));
            vh.textViews[4].setText(formatter.format(tweet.getFavoriteCount()));
            vh.textViews[6].setText(getTimeString(tweet.getTime()));

            if (tweet.retweeted()) {
                vh.rtIcon.setColorFilter(Color.GREEN, SRC_IN);
            } else {
                vh.rtIcon.setColorFilter(settings.getIconColor(), SRC_IN);
            }
            if (tweet.favored()) {
                vh.favIcon.setColorFilter(Color.YELLOW, SRC_IN);
            } else {
                vh.favIcon.setColorFilter(settings.getIconColor(), SRC_IN);
            }
            if (user.isVerified()) {
                vh.verifiedIcon.setVisibility(VISIBLE);
            } else {
                vh.verifiedIcon.setVisibility(GONE);
            }
            if (user.isLocked()) {
                vh.lockedIcon.setVisibility(VISIBLE);
            } else {
                vh.lockedIcon.setVisibility(GONE);
            }
            if (settings.getImageLoad() && user.hasProfileImage()) {
                String pbLink = user.getImageLink();
                if (!user.hasDefaultProfileImage())
                    pbLink += settings.getImageSuffix();
                Picasso.get().load(pbLink).transform(new RoundedCornersTransformation(2, 0))
                        .error(R.drawable.no_image).into(vh.profile);
            } else {
                vh.profile.setImageResource(0);
            }
        } else if (holder instanceof PlaceHolder) {
            PlaceHolder vh = (PlaceHolder) holder;
            if (loadingIndex != NO_INDEX) {
                vh.loadCircle.setVisibility(VISIBLE);
                vh.loadBtn.setVisibility(INVISIBLE);
            } else {
                vh.loadCircle.setVisibility(INVISIBLE);
                vh.loadBtn.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * Holder class for the tweet view
     */
    private final class TweetHolder extends ViewHolder {
        final TextView[] textViews = new TextView[7];
        final ImageView profile, rtUser, verifiedIcon, lockedIcon, rtIcon, favIcon;

        TweetHolder(@NonNull View v, GlobalSettings settings) {
            super(v);
            CardView background = (CardView) v;
            profile = v.findViewById(R.id.tweetPb);
            verifiedIcon = v.findViewById(R.id.verified_icon);
            lockedIcon = v.findViewById(R.id.locked_icon);
            rtUser = v.findViewById(R.id.rt_user_icon);
            rtIcon = v.findViewById(R.id.rt_icon);
            favIcon = v.findViewById(R.id.fav_icon);
            textViews[0] = v.findViewById(R.id.username);
            textViews[1] = v.findViewById(R.id.screenname);
            textViews[2] = v.findViewById(R.id.tweettext);
            textViews[3] = v.findViewById(R.id.retweet_number);
            textViews[4] = v.findViewById(R.id.favorite_number);
            textViews[5] = v.findViewById(R.id.retweeter);
            textViews[6] = v.findViewById(R.id.time);

            for (TextView tv : textViews) {
                tv.setTextColor(settings.getFontColor());
                tv.setTypeface(settings.getFontFace());
            }
            verifiedIcon.setImageResource(R.drawable.verify);
            lockedIcon.setImageResource(R.drawable.lock);
            rtUser.setImageResource(R.drawable.retweet);
            rtIcon.setImageResource(R.drawable.retweet);
            favIcon.setImageResource(R.drawable.favorite);
            verifiedIcon.setColorFilter(settings.getIconColor(), SRC_IN);
            lockedIcon.setColorFilter(settings.getIconColor(), SRC_IN);
            rtUser.setColorFilter(settings.getIconColor(), SRC_IN);
            background.setCardBackgroundColor(settings.getCardColor());
        }
    }

    /**
     * Holder class for the placeholder view
     */
    private final class PlaceHolder extends ViewHolder {
        final Button loadBtn;
        final ProgressBar loadCircle;

        PlaceHolder(@NonNull View v, GlobalSettings settings) {
            super(v);
            CardView background = (CardView) v;
            loadBtn = v.findViewById(R.id.placeholder_button);
            loadCircle = v.findViewById(R.id.placeholder_loading);

            background.setCardBackgroundColor(settings.getCardColor());
            AppStyles.setProgressColor(loadCircle, settings.getHighlightColor());
        }
    }

    /**
     * Listener for tweet click
     */
    public interface TweetClickListener {

        /**
         * handle click action
         *
         * @param tweet clicked tweet
         */
        void onTweetClick(Tweet tweet);

        /**
         * called on placeholder click
         *
         * @param sinceId the tweet ID of the tweet below the holder
         * @param maxId   the tweet ID of the tweet over the holder
         * @param pos     position of the holder
         */
        void onHolderClick(long sinceId, long maxId, int pos);
    }
}