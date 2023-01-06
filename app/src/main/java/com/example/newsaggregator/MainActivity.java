package com.example.newsaggregator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    public static final String ACTION_MSG_TO_SERVICE = "ACTION_MSG_TO_SERVICE";
    public static final String ACTION_NEWS_STORY = "ACTION_NEWS_STORY";
    public static final String ARTICLE_LIST = "ARTICLE_LIST";
    public static final String SOURCE_ID = "SOURCE_ID";
    public static final String LANG_ID="LANG_ID";
    private static final String TAG = "MainActivity";
    private String newsSource;
    private String newsLanguage;
    Menu menu;

    private int currentSourcePointer;
    private boolean appState;
    private boolean serviceStatus = false;

    private List<String> sourceList;
    private List<String> languagesList;
    private List<NewsSource> sources;
    private List<String> categories;
    private List<String> languages;
    private List<String> countries;
    private List<NewsArticle> articles;
    private List<NewsFragment> newsFragments;
    private Map<String, NewsSource> sourceStore;

    private String submenu_category;
    private String submenu_language;
    private String submenu_country;

    private MainAcitvityReceiver receiver;
    private SourceAdapter adapter;

    private Menu categoryMenu;

    private List<Drawer> drawerList;
    private DrawerLayout drawerLayout;
    private ListView drawerListView;
    private ActionBarDrawerToggle drawerToggle;
    private PageAdapter pageAdapter;
    private ViewPager viewPager;

    private int[] topicColors;
    private Map<String, Integer> topicIntMap;

    private HashMap<String,String> codeLanguages;
    private HashMap<String,String> languagesCode;

    private HashMap<String,String> codeCountry;
    private HashMap<String,String> countriesCode;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sourceList = new ArrayList<>();
        sources = new ArrayList<>();
        categories = new ArrayList<>();
        countries = new ArrayList<>();
        languages = new ArrayList<>();
        articles = new ArrayList<>();

        drawerList = new ArrayList<>();
        newsFragments = new ArrayList<>();

        sourceStore = new HashMap<>();

        codeLanguages =  new HashMap<>();
        languagesCode = new HashMap<>();

        codeCountry = new HashMap<>();
        countriesCode = new HashMap<>();

        submenu_language = "";
        submenu_country = "";
        submenu_category = "";

        topicIntMap = new HashMap<>();
        topicColors = getResources().getIntArray(R.array.topicColors);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        receiver = new MainAcitvityReceiver(this);
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerListView = findViewById(R.id.drawerList);
        adapter = new SourceAdapter(this, drawerList);
        drawerListView.setAdapter(adapter);
        pageAdapter = new PageAdapter(getSupportFragmentManager(), newsFragments);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(pageAdapter);

        // Start service if not started
        if (savedInstanceState == null && !serviceStatus) {
            Log.d(TAG, "onCreate: Starting News Service");
            Intent intent = new Intent(MainActivity.this, NewsService.class);
            startService(intent);
            serviceStatus = true;
        }

        IntentFilter filter = new IntentFilter(MainActivity.ACTION_NEWS_STORY);
        registerReceiver(receiver, filter);

        // if no data is there to restore
        if (sourceStore.isEmpty() && savedInstanceState == null)
            new Thread(new SourceDownloader(this, "", "", "")).start();


        // add click listener to drawer list view
        drawerListView.setOnItemClickListener((parent, view, position, id) -> {
            viewPager.setBackgroundResource(0);
            currentSourcePointer = position;
            selectListItem(position);
        });

        // update the drawer toggle
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open_navigation_drawer, R.string.close_navigation_drawer);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: ");
//        getMenuInflater().inflate(R.menu.categories, menu);
//        categoryMenu = menu;
//        if (appState) {
//            SubMenu topicsubMenu = categoryMenu.addSubMenu("Topics");
//            SubMenu languagesubMenu = categoryMenu.addSubMenu("Languages");
//            SubMenu countrysubMenu = categoryMenu.addSubMenu("Countries");
//            for (String category : categories)
//                topicsubMenu.add(category);
//            for(String language : languages)
//                languagesubMenu.add(language);
//            for(String country: countries)
//                countrysubMenu.add(country);
//        }
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: ");
        if (drawerToggle.onOptionsItemSelected(item)) {  // <== Important!
            Log.d(TAG, "onOptionsItemSelected: drawerToggle " + item);
            return true;
        }

        if(item.hasSubMenu())
            return true;

        int group_id = item.getGroupId();
        int menuitem_id = item.getItemId();

        //itemid  = 0 -> Topics -> categories
        if(item.getGroupId() == 0){
            submenu_category = item.getTitle().toString();
            submenu_language="";
            submenu_country="";
            //submenu_language="";
        }

        //itemid = 1 -> Languages
        if(item.getGroupId()==1){
            submenu_language = languagesCode.get(item.getTitle().toString());
            submenu_country="";
        }
        //itemid = 2  -> Countries
        if(item.getGroupId()==2){
            submenu_country = countriesCode.get(item.getTitle().toString());
            submenu_language="";
        }
        Log.d(TAG, "onOptionsItemSelected: Starting Source Downloader thread");
        new Thread(new SourceDownloader(this, submenu_category, submenu_language.toLowerCase(),submenu_country.toLowerCase())).start();
        drawerLayout.openDrawer(drawerListView);

        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onPostCreate: ");
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged: ");
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "onSaveInstanceState: ");
        LayoutManager layoutRestore = new LayoutManager();

        Log.d(TAG, "categories: " + categories);
        layoutRestore.setCategories(categories);

        Log.d(TAG, "languages: " + languages);
        layoutRestore.setLanguages(languages);

        Log.d(TAG, "countries: " + countries);
        layoutRestore.setCountries(countries);

        Log.d(TAG, "sources: " + sources);
        layoutRestore.setSources(sources);

        layoutRestore.setArticle(viewPager.getCurrentItem());

        Log.d(TAG, "currentSourcePointer : " + currentSourcePointer);
        layoutRestore.setSource(currentSourcePointer);

        Log.d(TAG, "articles : " + articles);
        layoutRestore.setArticles(articles);

        outState.putSerializable("state", layoutRestore);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(TAG, "onRestoreInstanceState: ");
        super.onRestoreInstanceState(savedInstanceState);

        setTitle(R.string.app_name);
        LayoutManager layoutManager = (LayoutManager) savedInstanceState.getSerializable("state");
        appState = true;

        articles = layoutManager.getArticles();
        Log.d(TAG, "articles: " + articles);

        categories = layoutManager.getCategories();
        Log.d(TAG, "categories: " + categories);

        languages = layoutManager.getLanguages();
        Log.d(TAG, "languages: " + languages);

        countries = layoutManager.getCountries();
        Log.d(TAG, "countries: " + countries);

        sources = layoutManager.getSources();
        Log.d(TAG, "sources: " + sources);

        for (int i = 0; i < sources.size(); i++) {
            sourceList.add(sources.get(i).getName());
            sourceStore.put(sources.get(i).getName(), sources.get(i));
        }


        drawerListView.clearChoices();
        adapter.notifyDataSetChanged();
        drawerListView.setOnItemClickListener((parent, view, position, id) -> {
                    viewPager.setBackgroundResource(0);
                    currentSourcePointer = position;
                    selectListItem(position);
                }
        );
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: Stopping News Service");
        unregisterReceiver(receiver);
        Intent intent = new Intent(MainActivity.this, MainAcitvityReceiver.class);
        stopService(intent);
        super.onDestroy();
    }

    public void populateSourceAndCategory(List<String> newsCategories, List<NewsSource> newsSources, List<String> newsLanguages, List<String> newsCountries) {
        Log.d(TAG, "populateSourceAndCategory: ");
        Log.d(TAG, "newsSources size: " + newsSources.size() + ", newsCategories size: " + newsCategories.size());
        sourceStore.clear();
        sourceList.clear();
        sources.clear();
        drawerList.clear();
        sources.addAll(newsSources);

        // Sort and update category list in the options menu
        if (!menu.hasVisibleItems()) {
            categories.clear();
            menu.clear();
            SubMenu topicsubMenu = menu.addSubMenu("Topics");
            SubMenu languagesubMenu = menu.addSubMenu("Languages");
            SubMenu countrysubMenu = menu.addSubMenu("Countries");
            categories = newsCategories;
            topicsubMenu.add("all");
            languagesubMenu.add("all");
            countrysubMenu.add("all");
            Collections.sort(newsCategories);
            int i = 0;
            int l=0;
            for (String category : newsCategories) {
                SpannableString categoryString = new SpannableString(category);
                categoryString.setSpan(new ForegroundColorSpan(topicColors[i]), 0, categoryString.length(), 0);
                topicIntMap.put(category, topicColors[i++]);
                topicsubMenu.add(0,l++,l++,categoryString);
            }
            int j=0;
            //call language loading funcn -> hashmap
            language_load();
            for(String language : newsLanguages){
                // language_code -> open languages.json -> find languge_code-> retrieve language_name
                languagesubMenu.add(1,j++,j++,codeLanguages.get(language.toUpperCase()));
            }
            int k =0;
            //call country loading funcn -> hashmap
            country_load();
            for(String country : newsCountries){
                countrysubMenu.add(2,k++,k++,codeCountry.get(country.toUpperCase()));
            }
        }
        for (NewsSource source : newsSources) {
            if (topicIntMap.containsKey(source.getCategory())) {
                int color = topicIntMap.get(source.getCategory());
                SpannableString coloredString = new SpannableString(source.getName());
                coloredString.setSpan(new ForegroundColorSpan(color), 0, source.getName().length(), 0);
                source.setColoredName(coloredString);
                sourceList.add(source.getName());
                sourceStore.put(source.getName(), source);
            }
        }
        //Update the title of the app - NewsAggregator(Sources_Count)
        update_title();

        // Update the drawer
        for (NewsSource source : newsSources) {
            Drawer drawerContent = new Drawer();
            drawerContent.setItemName(source.getColoredName());
            drawerList.add(drawerContent);
        }
        adapter.notifyDataSetChanged();

        if(drawerList.isEmpty()){
            //alert dialog box
            Toast.makeText(MainActivity.this, "Empty sources", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder alertBuild = new AlertDialog.Builder(this);
            alertBuild.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    return;
                }
            });
            alertBuild.setMessage("No sources exits for this combination: Topic - "+submenu_category+
                    " Language - "+submenu_language+
                    " Country - "+submenu_country);
            AlertDialog alertDialog = alertBuild.create();
            alertDialog.show();
        }
    }

    public void updateFragments(List<NewsArticle> articles) {
        Log.d(TAG, "updateFragments: ");
        setTitle(newsSource);

        for (int i = 0; i < pageAdapter.getCount(); i++)
            pageAdapter.notifyChangeInPosition(i);

        newsFragments.clear();

        for (int article = 0; article < articles.size(); article++) {
            newsFragments.add(NewsFragment.newInstance(articles.get(article), article, articles.size()));
        }
        pageAdapter.notifyDataSetChanged();
        viewPager.setCurrentItem(0);
        this.articles = articles;
    }

    private void selectListItem(int position) {
        Log.d(TAG, "selectListItem => selected pos: " + position + ", sourceList size: " + sourceList.size());
        newsSource = sourceList.get(position);
        newsLanguage = submenu_language.toLowerCase();
        Intent intent = new Intent(MainActivity.ACTION_MSG_TO_SERVICE);
        intent.putExtra(SOURCE_ID, newsSource);
        intent.putExtra(LANG_ID,newsLanguage);
        sendBroadcast(intent);
        drawerLayout.closeDrawer(drawerListView);
    }


    public void update_title()
    {
        int total_numberSources = sourceList.size();
        if (total_numberSources != 0)
            setTitle(getString(R.string.app_name)+ " (" + total_numberSources + ")");
        else
            setTitle(getString(R.string.app_name));
    }

    public void  language_load(){
        String json=null;
        try {
            InputStream is = getResources().openRawResource(R.raw.language_codes);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer,"UTF-8");

            JSONObject jsonMainObject = new JSONObject(json);
            JSONArray jsonArrayLanguage = jsonMainObject.getJSONArray("languages");
            for (int i = 0; i < jsonArrayLanguage.length(); i++) {
                JSONObject jsonObject = jsonArrayLanguage.getJSONObject(i);
                String code = jsonObject.getString("code");
                String name = jsonObject.getString("name");
                codeLanguages.put(code,name);
                languagesCode.put(name,code);
            }
        }
        catch (FileNotFoundException e){
            Toast.makeText(this, " No JSON file found", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void  country_load(){
        String json=null;
        try {
            InputStream is = getResources().openRawResource(R.raw.country_codes);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer,"UTF-8");

            JSONObject jsonMainObject = new JSONObject(json);
            JSONArray jsonArrayCountry = jsonMainObject.getJSONArray("countries");
            for (int i = 0; i < jsonArrayCountry.length(); i++) {
                JSONObject jsonObjectC = jsonArrayCountry.getJSONObject(i);
                String country_code = jsonObjectC.getString("code");
                String country_name = jsonObjectC.getString("name");
                codeCountry.put(country_code,country_name);
                countriesCode.put(country_name,country_code);
            }
        }
        catch (FileNotFoundException e){
            Toast.makeText(this, " No JSON file found", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


}