package com.example.newsaggregator;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.List;

import static com.example.newsaggregator.MainActivity.ARTICLE_LIST;

import com.example.newsaggregator.MainActivity;
import com.example.newsaggregator.NewsArticle;

public class MainAcitvityReceiver extends BroadcastReceiver {

    private static final String TAG = "MainActivityReceiver";

    private final MainActivity mainActivity;

    public MainAcitvityReceiver(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: ");
        String action = intent.getAction();
        if (action == null)
            return;
        if (MainActivity.ACTION_NEWS_STORY.equals(action)) {
            List<NewsArticle> articles;
            if (intent.hasExtra(ARTICLE_LIST)) {
                articles = (List<NewsArticle>) intent.getSerializableExtra(ARTICLE_LIST);
                mainActivity.updateFragments(articles);
            }
        }
    }
}