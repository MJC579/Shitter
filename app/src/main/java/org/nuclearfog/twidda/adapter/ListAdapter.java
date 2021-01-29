package org.nuclearfog.twidda.adapter;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
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
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.squareup.picasso.Picasso;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.holder.UserListList;
import org.nuclearfog.twidda.backend.items.User;
import org.nuclearfog.twidda.backend.items.UserList;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.database.GlobalSettings;
import org.nuclearfog.twidda.fragment.UserListFragment;

import java.text.NumberFormat;

import jp.wasabeef.picasso.transformations.RoundedCornersTransformation;

import static android.graphics.PorterDuff.Mode.SRC_IN;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;
import static org.nuclearfog.twidda.backend.utils.StringTools.getTimeString;

/**
 * Adapter class for user lists
 *
 * @author nuclearfog
 * @see UserListFragment
 */
public class ListAdapter extends Adapter<ViewHolder> {

    /**
     * indicator if there is no footer
     */
    private static final int NO_LOADING = -1;

    /**
     * View type for a footer
     */
    private static final int ITEM_FOOTER = 0;

    /**
     * View type for an userlist item
     */
    private static final int ITEM_LIST = 1;

    private ListClickListener listener;
    private GlobalSettings settings;
    private Drawable[] icons;

    private NumberFormat formatter = NumberFormat.getIntegerInstance();
    private UserListList data = new UserListList();
    private int loadingIndex = NO_LOADING;


    public ListAdapter(Context context, ListClickListener listener) {
        this.listener = listener;
        this.settings = GlobalSettings.getInstance(context);

        TypedArray drawables = context.getResources().obtainTypedArray(R.array.list_item_icons);
        icons = new Drawable[drawables.length()];
        for (int index = 0; index < drawables.length(); index++) {
            int resId = drawables.getResourceId(index, 0);
            icons[index] = AppCompatResources.getDrawable(context, resId);
        }
        drawables.recycle();
        colorIcons();
    }

    /**
     * adds new data to the list
     *
     * @param newData new list to add
     */
    @MainThread
    public void setData(UserListList newData) {
        if (newData.isEmpty()) {
            if (!data.isEmpty() && data.peekLast() == null) {
                // remove footer
                data.pollLast();
            }
        } else if (data.isEmpty() || !newData.hasPrevious()) {
            data.replace(newData);
            if (data.hasNext()) {
                // Add footer
                data.add(null);
            }
            notifyDataSetChanged();
        } else {
            int end = data.size() - 1;
            if (!data.hasNext()) {
                // remove footer
                data.remove(end);
                notifyItemRemoved(end);
            }
            data.addListAt(newData, end);
            notifyItemRangeInserted(end, newData.size());
        }
        disableLoading();
    }


    /**
     * update a single item
     *
     * @param list updated list
     */
    @MainThread
    public void updateItem(UserList list) {
        int index = data.indexOf(list);
        if (index >= 0) {
            data.set(index, list);
            notifyItemChanged(index);
        }
    }

    /**
     * remove userlist item from list
     *
     * @param itemId userlist id to remove
     */
    @MainThread
    public void removeItem(long itemId) {
        for (int index = 0; index < data.size(); index++) {
            if (data.get(index).getId() == itemId) {
                data.remove(index);
                notifyItemRemoved(index);
                break;
            }
        }
    }


    @Override
    public int getItemCount() {
        return data.size();
    }


    @Override
    public int getItemViewType(int position) {
        if (data.get(position) == null)
            return ITEM_FOOTER;
        return ITEM_LIST;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ITEM_LIST) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
            final ListHolder vh = new ListHolder(v, settings);
            vh.profile_img.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getLayoutPosition();
                    if (position != NO_POSITION) {
                        User user = data.get(position).getListOwner();
                        listener.onProfileClick(user);
                    }
                }
            });
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = vh.getLayoutPosition();
                    if (position != NO_POSITION) {
                        UserList list = data.get(position);
                        listener.onListClick(list);
                    }
                }
            });
            return vh;
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_placeholder, parent, false);
            final PlaceHolder ph = new PlaceHolder(v, settings);
            ph.loadBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = ph.getLayoutPosition();
                    if (position != NO_POSITION) {
                        listener.onFooterClick(data.getNext());
                        ph.loadCircle.setVisibility(VISIBLE);
                        ph.loadBtn.setVisibility(INVISIBLE);
                        loadingIndex = position;
                    }
                }
            });
            return ph;
        }
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int index) {
        if (holder instanceof ListHolder) {
            ListHolder vh = (ListHolder) holder;
            UserList item = data.get(index);
            User owner = item.getListOwner();
            vh.textViews[0].setText(item.getTitle());
            vh.textViews[2].setText(owner.getUsername());
            vh.textViews[3].setText(owner.getScreenname());
            vh.textViews[1].setText(item.getDescription());
            vh.textViews[4].setText(getTimeString(item.getCreatedAt()));
            vh.textViews[5].setText(formatter.format(item.getMemberCount()));
            vh.textViews[6].setText(formatter.format(item.getSubscriberCount()));
            if (settings.getImageLoad() && owner.hasProfileImage()) {
                String pbLink = owner.getImageLink();
                if (!owner.hasDefaultProfileImage())
                    pbLink += settings.getImageSuffix();
                Picasso.get().load(pbLink).transform(new RoundedCornersTransformation(3, 0))
                        .error(R.drawable.no_image).into(vh.profile_img);
            } else {
                vh.profile_img.setImageResource(0);
            }
            if (!item.isListOwner() && item.isFollowing()) {
                vh.textViews[7].setVisibility(VISIBLE);
            } else {
                vh.textViews[7].setVisibility(GONE);
            }
            if (owner.isVerified()) {
                setIcon(vh.textViews[2], icons[0]);
            } else {
                setIcon(vh.textViews[2], null);
            }
            if (owner.isLocked()) {
                setIcon(vh.textViews[3], icons[1]);
            } else {
                setIcon(vh.textViews[3], null);
            }
            if (item.isPrivate()) {
                setIcon(vh.textViews[0], icons[2]);
            } else {
                setIcon(vh.textViews[0], null);
            }
        } else if (holder instanceof PlaceHolder) {
            PlaceHolder placeHolder = (PlaceHolder) holder;
            if (loadingIndex != NO_LOADING) {
                placeHolder.loadCircle.setVisibility(VISIBLE);
                placeHolder.loadBtn.setVisibility(INVISIBLE);
            } else {
                placeHolder.loadCircle.setVisibility(INVISIBLE);
                placeHolder.loadBtn.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * disable loading animation in footer
     */
    public void disableLoading() {
        if (loadingIndex != NO_LOADING) {
            int oldIndex = loadingIndex;
            loadingIndex = NO_LOADING;
            notifyItemChanged(oldIndex);
        }
    }

    /**
     * sets the TextView icons
     *
     * @param tv   TextView to add an icon
     * @param icon icon drawable
     */
    private void setIcon(TextView tv, @Nullable Drawable icon) {
        if (icon != null)
            icon = icon.mutate();
        tv.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
    }

    private void colorIcons() {
        for (Drawable icon : icons) {
            icon.setColorFilter(settings.getIconColor(), SRC_IN);
        }
    }

    /**
     * view holder class for an user list item
     */
    private final class ListHolder extends ViewHolder {

        final ImageView profile_img, subscrIcon, memberIcon;
        final TextView[] textViews = new TextView[8];

        ListHolder(View v, GlobalSettings settings) {
            super(v);
            CardView background = (CardView) v;
            profile_img = v.findViewById(R.id.list_owner_profile);
            memberIcon = v.findViewById(R.id.list_member_icon);
            subscrIcon = v.findViewById(R.id.list_subscriber_icon);
            textViews[0] = v.findViewById(R.id.list_title);
            textViews[1] = v.findViewById(R.id.list_description);
            textViews[2] = v.findViewById(R.id.list_ownername);
            textViews[3] = v.findViewById(R.id.list_screenname);
            textViews[4] = v.findViewById(R.id.list_createdat);
            textViews[5] = v.findViewById(R.id.list_member);
            textViews[6] = v.findViewById(R.id.list_subscriber);
            textViews[7] = v.findViewById(R.id.list_action);

            for (TextView tv : textViews) {
                tv.setTextColor(settings.getFontColor());
                tv.setTypeface(settings.getFontFace());
            }
            background.setCardBackgroundColor(settings.getCardColor());
            textViews[4].setCompoundDrawablesWithIntrinsicBounds(icons[3], null, null, null);
            textViews[7].setCompoundDrawablesWithIntrinsicBounds(icons[4], null, null, null);
            memberIcon.setImageDrawable(icons[5]);
            subscrIcon.setImageDrawable(icons[6]);
        }
    }

    /**
     * view holder class for a footer view
     */
    private final class PlaceHolder extends ViewHolder {

        final ProgressBar loadCircle;
        final Button loadBtn;

        PlaceHolder(@NonNull View v, GlobalSettings settings) {
            super(v);
            CardView background = (CardView) v;
            loadCircle = v.findViewById(R.id.placeholder_loading);
            loadBtn = v.findViewById(R.id.placeholder_button);

            loadBtn.setTypeface(settings.getFontFace());
            loadBtn.setTextColor(settings.getFontColor());
            AppStyles.setProgressColor(loadCircle, settings.getHighlightColor());
            background.setCardBackgroundColor(settings.getCardColor());
        }
    }

    /**
     * Listener for an item
     */
    public interface ListClickListener {

        /**
         * called when an item is clicked
         *
         * @param listItem Item data and information
         */
        void onListClick(UserList listItem);

        /**
         * called when the profile image of the owner was clicked
         *
         * @param user user information
         */
        void onProfileClick(User user);

        /**
         * called when the footer is clicked
         *
         * @param cursor next cursor of the list
         */
        void onFooterClick(long cursor);
    }
}