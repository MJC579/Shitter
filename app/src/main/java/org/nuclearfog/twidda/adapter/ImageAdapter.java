package org.nuclearfog.twidda.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.nuclearfog.twidda.R;
import org.nuclearfog.twidda.backend.holder.ImageHolder;
import org.nuclearfog.twidda.backend.utils.AppStyles;
import org.nuclearfog.twidda.database.GlobalSettings;

import java.util.LinkedList;
import java.util.List;

import static android.view.View.VISIBLE;
import static androidx.recyclerview.widget.RecyclerView.NO_POSITION;

/**
 * Adapter class for image previews
 *
 * @author nuclearfog
 */
public class ImageAdapter extends Adapter<ViewHolder> {

    /**
     * View type for an image item
     */
    private static final int PICTURE = 0;

    /**
     * View type for a circle view
     */
    private static final int LOADING = 1;

    private OnImageClickListener itemClickListener;
    private GlobalSettings settings;

    private List<ImageHolder> images = new LinkedList<>();
    private boolean loading = false;
    private boolean saveImg = true;


    public ImageAdapter(GlobalSettings settings, OnImageClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
        this.settings = settings;
    }

    /**
     * add new image at the last position
     *
     * @param imageItem image to add
     */
    @MainThread
    public void addLast(@NonNull ImageHolder imageItem) {
        int imagePos = images.size();
        if (imagePos == 0)
            loading = true;
        images.add(imageItem);
        notifyItemInserted(imagePos);
    }

    /**
     * disable placeholder view
     */
    @MainThread
    public void disableLoading() {
        loading = false;
        int circlePos = images.size();
        notifyItemRemoved(circlePos);
    }

    /**
     * disable save button on images
     */
    public void disableSaveButton() {
        saveImg = false;
    }

    /**
     * check if image adapter is empty
     *
     * @return true if there isn't any image
     */
    public boolean isEmpty() {
        return images.isEmpty();
    }


    @Override
    public int getItemViewType(int position) {
        if (loading && position == images.size())
            return LOADING;
        return PICTURE;
    }


    @Override
    public int getItemCount() {
        if (loading)
            return images.size() + 1;
        return images.size();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == PICTURE) {
            View view = inflater.inflate(R.layout.item_image, parent, false);
            final ImageItem item = new ImageItem(view, settings);
            item.preview.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = item.getAdapterPosition();
                    if (pos != NO_POSITION) {
                        Bitmap img = images.get(pos).getMiddleSize();
                        itemClickListener.onImageClick(img);
                    }
                }
            });
            if (saveImg) {
                item.saveButton.setVisibility(VISIBLE);
                item.saveButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = item.getAdapterPosition();
                        if (pos != NO_POSITION) {
                            Bitmap img = images.get(pos).getOriginalImage();
                            itemClickListener.onImageSave(img, pos);
                        }
                    }
                });
            }
            return item;
        } else {
            View view = inflater.inflate(R.layout.item_image_load, parent, false);
            return new LoadItem(view, settings.getHighlightColor());
        }
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder vh, int index) {
        if (vh instanceof ImageItem) {
            ImageItem item = (ImageItem) vh;
            Bitmap image = images.get(index).getSmallSize();
            item.preview.setImageBitmap(image);
        }
    }

    /**
     * Holder for image
     */
    private final class ImageItem extends ViewHolder {
        final ImageView preview;
        final ImageButton saveButton;

        ImageItem(View view, GlobalSettings settings) {
            super(view);
            preview = view.findViewById(R.id.item_image_preview);
            saveButton = view.findViewById(R.id.item_image_save);
            saveButton.setImageResource(R.drawable.save);
            AppStyles.setButtonColor(saveButton, settings.getFontColor());
            AppStyles.setDrawableColor(saveButton, settings.getIconColor());
        }
    }

    /**
     * Holder for progress circle
     */
    private final class LoadItem extends ViewHolder {

        LoadItem(View v, int color) {
            super(v);
            ProgressBar progress = v.findViewById(R.id.imageitem_progress);
            AppStyles.setProgressColor(progress, color);
        }
    }


    public interface OnImageClickListener {

        /**
         * simple click on image_add
         *
         * @param image selected image_add bitmap
         */
        void onImageClick(Bitmap image);

        /**
         * long touch on image_add
         *
         * @param image selected image_add bitmap
         * @param index current image index
         */
        void onImageSave(Bitmap image, int index);
    }
}