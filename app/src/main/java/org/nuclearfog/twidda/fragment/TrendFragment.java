package org.nuclearfog.twidda.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;

import org.nuclearfog.twidda.activity.SearchPage;
import org.nuclearfog.twidda.adapter.FragmentAdapter.FragmentChangeObserver;
import org.nuclearfog.twidda.adapter.TrendAdapter;
import org.nuclearfog.twidda.adapter.TrendAdapter.TrendClickListener;
import org.nuclearfog.twidda.backend.TrendLoader;
import org.nuclearfog.twidda.backend.items.TwitterTrend;
import org.nuclearfog.twidda.database.GlobalSettings;

import static android.os.AsyncTask.Status.FINISHED;
import static android.os.AsyncTask.Status.RUNNING;
import static org.nuclearfog.twidda.activity.SearchPage.KEY_SEARCH_QUERY;


public class TrendFragment extends Fragment implements OnRefreshListener, TrendClickListener, FragmentChangeObserver {

    private TrendLoader trendTask;
    private SwipeRefreshLayout reload;
    private RecyclerView list;
    private TrendAdapter adapter;
    private GlobalSettings settings;
    private boolean notifyChange;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle param) {
        Context context = inflater.getContext();

        settings = GlobalSettings.getInstance(context);
        adapter = new TrendAdapter(this, settings);

        list = new RecyclerView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setHasFixedSize(true);
        list.setAdapter(adapter);

        reload = new SwipeRefreshLayout(context);
        reload.setOnRefreshListener(this);
        reload.addView(list);
        return reload;
    }


    @Override
    public void onStart() {
        super.onStart();
        if (trendTask == null)
            load();
        if (notifyChange) {
            adapter.notifyDataSetChanged();
            notifyChange = false;
            load();
        }
        reload.setProgressBackgroundColorSchemeColor(settings.getHighlightColor());
    }


    @Override
    public void onDestroy() {
        if (trendTask != null && trendTask.getStatus() == RUNNING)
            trendTask.cancel(true);
        super.onDestroy();
    }


    @Override
    public void onRefresh() {
        if (trendTask != null && trendTask.getStatus() != RUNNING)
            load();
    }


    @Override
    public void onTrendClick(TwitterTrend trend) {
        if (!reload.isRefreshing()) {
            Intent intent = new Intent(getContext(), SearchPage.class);
            intent.putExtra(KEY_SEARCH_QUERY, trend.getSearchString());
            startActivity(intent);
        }
    }


    @Override
    public void onSettingsChange() {
        notifyChange = true;
    }


    @Override
    public void onTabChange() {
        if (list != null)
            list.smoothScrollToPosition(0);
    }


    @Override
    public void onDataClear() {
        if (adapter != null)
            adapter.clear();
        load();
    }


    public TrendAdapter getAdapter() {
        return adapter;
    }


    public void setRefresh(boolean enable) {
        if (enable) {
            reload.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (trendTask.getStatus() != FINISHED && !reload.isRefreshing())
                        reload.setRefreshing(true);
                }
            }, 500);
        } else {
            reload.setRefreshing(false);
        }
    }


    private void load() {
        trendTask = new TrendLoader(this);
        trendTask.execute(settings.getTrendLocation().getWoeId());
    }
}