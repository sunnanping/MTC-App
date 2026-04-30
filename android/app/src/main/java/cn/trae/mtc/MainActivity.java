package cn.trae.mtc;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends com.getcapacitor.BridgeActivity {

    private static final String PREFS_NAME = "MTC_Prefs";
    private static final String KEY_SITES = "saved_sites";
    private static final String KEY_LANG = "saved_language";
    private static final String KEY_SAVES = "recent_saves";
    private static final String KEY_MD5_CACHE = "md5_cache";
    private static final String KEY_PERMISSION = "file_permission";
    private static final String KEY_ALWAYS_REMIND = "always_remind";
    private static final String KEY_ACTIVE_SITE_URL = "active_site_url";
    private static final String KEY_SCROLL_X = "site_scroll_x";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final int REQUEST_STORAGE_PERMISSION = 1001;
    private static final int MAX_MD5_CACHE = 100;
    private static final int MAX_RECENT_SAVES = 10;
    private static final int MIN_CONTENT_LENGTH = 500;

    private List<Site> sites;
    private Site activeSite;
    private String currentLanguage = "EN";
    private List<SaveRecord> recentSaves = new ArrayList<>();
    private List<Md5CacheItem> md5Cache = new ArrayList<>();
    private Map<String, Integer> siteCounters = new HashMap<>();
    private String pendingContent = null;

    private LinearLayout languageSelector;
    private TextView languageText;
    private android.widget.HorizontalScrollView siteScroll;
    private LinearLayout siteContainer;
    private android.webkit.WebView webView;
    private ClipboardManager clipboardManager;
    private SharedPreferences prefs;
    private View addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Check if it's the first launch
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        if (isFirstLaunch) {
            // Use system language on first launch
            Locale systemLocale = Locale.getDefault();
            currentLanguage = getLanguageCodeFromSystemLocale(systemLocale);
            // Save the language and mark that first launch is done
            prefs.edit()
                .putString(KEY_LANG, currentLanguage)
                .putBoolean(KEY_FIRST_LAUNCH, false)
                .apply();
        } else {
            // Use saved language
            currentLanguage = prefs.getString(KEY_LANG, "EN");
        }
        
        setAppLocale(currentLanguage);
        
        setContentView(R.layout.activity_main);
        
        initPreferences();
        initViews();
        initWebView();
        initClipboard();
        initDefaultSites();
        renderSites();
        
        String activeSiteUrl = prefs.getString(KEY_ACTIVE_SITE_URL, null);
        if (activeSiteUrl != null) {
            Site savedSite = findSiteByUrl(activeSiteUrl);
            if (savedSite != null) {
                selectSite(savedSite);
                restoreScrollPosition();
                return;
            }
        }
        
        if (!sites.isEmpty()) {
            selectSite(sites.get(0));
        }
    }

    private void restoreScrollPosition() {
        if (siteScroll != null) {
            int scrollX = prefs.getInt(KEY_SCROLL_X, 0);
            siteScroll.post(() -> siteScroll.scrollTo(scrollX, 0));
        }
    }

    private Site findSiteByUrl(String url) {
        for (Site site : sites) {
            if (site.url.equals(url)) {
                return site;
            }
        }
        return null;
    }

    private void setAppLocale(String languageCode) {
        Locale locale = getLocaleFromLanguageCode(languageCode);
        Locale.setDefault(locale);
        
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getBaseContext().getResources().updateConfiguration(config, 
            getBaseContext().getResources().getDisplayMetrics());
    }

    private Locale getLocaleFromLanguageCode(String languageCode) {
        switch (languageCode) {
            case "ZH":
                return new Locale("zh", "CN");
            case "JA":
                return new Locale("ja", "JP");
            case "KO":
                return new Locale("ko", "KR");
            case "ES":
                return new Locale("es", "ES");
            case "FR":
                return new Locale("fr", "FR");
            case "DE":
                return new Locale("de", "DE");
            case "PT":
                return new Locale("pt", "PT");
            case "RU":
                return new Locale("ru", "RU");
            case "AR":
                return new Locale("ar", "SA");
            case "HI":
                return new Locale("hi", "IN");
            case "BN":
                return new Locale("bn", "BD");
            case "PA":
                return new Locale("pa", "IN");
            case "JV":
                return new Locale("jv", "ID");
            case "MR":
                return new Locale("mr", "IN");
            case "TR":
                return new Locale("tr", "TR");
            case "IT":
                return new Locale("it", "IT");
            case "PL":
                return new Locale("pl", "PL");
            case "UK":
                return new Locale("uk", "UA");
            case "NL":
                return new Locale("nl", "NL");
            default:
                return new Locale("en", "US");
        }
    }

    private String getUaLanguageCode(String languageCode) {
        switch (languageCode) {
            case "ZH":
                return "zh-CN";
            case "JA":
                return "ja-JP";
            case "KO":
                return "ko-KR";
            case "ES":
                return "es-ES";
            case "FR":
                return "fr-FR";
            case "DE":
                return "de-DE";
            case "PT":
                return "pt-PT";
            case "RU":
                return "ru-RU";
            case "AR":
                return "ar-SA";
            case "HI":
                return "hi-IN";
            case "BN":
                return "bn-BD";
            case "PA":
                return "pa-IN";
            case "JV":
                return "jv-ID";
            case "MR":
                return "mr-IN";
            case "TR":
                return "tr-TR";
            case "IT":
                return "it-IT";
            case "PL":
                return "pl-PL";
            case "UK":
                return "uk-UA";
            case "NL":
                return "nl-NL";
            default:
                return "en-US";
        }
    }

    private String getLanguageCodeFromSystemLocale(Locale systemLocale) {
        String language = systemLocale.getLanguage();
        String country = systemLocale.getCountry();
        
        switch (language) {
            case "zh":
                return "ZH";
            case "ja":
                return "JA";
            case "ko":
                return "KO";
            case "es":
                return "ES";
            case "fr":
                return "FR";
            case "de":
                return "DE";
            case "pt":
                return "PT";
            case "ru":
                return "RU";
            case "ar":
                return "AR";
            case "hi":
                return "HI";
            case "bn":
                return "BN";
            case "pa":
                return "PA";
            case "jv":
                return "JV";
            case "mr":
                return "MR";
            case "tr":
                return "TR";
            case "it":
                return "IT";
            case "pl":
                return "PL";
            case "uk":
                return "UK";
            case "nl":
                return "NL";
            default:
                return "EN";
        }
    }

    private void initPreferences() {
        String sitesJson = prefs.getString(KEY_SITES, null);
        if (sitesJson != null) {
            try {
                loadSitesFromJson(sitesJson);
            } catch (Exception e) {
                sites = getDefaultSites();
            }
        } else {
            sites = getDefaultSites();
        }
        
        loadRecentSaves();
        loadMd5Cache();
    }

    private void loadRecentSaves() {
        recentSaves.clear();
        String savesJson = prefs.getString(KEY_SAVES, "[]");
        try {
            JSONArray array = new JSONArray(savesJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                recentSaves.add(new SaveRecord(
                    obj.getString("filename"),
                    obj.getLong("timestamp")
                ));
            }
        } catch (Exception e) {
            recentSaves = new ArrayList<>();
        }
    }

    private void loadMd5Cache() {
        md5Cache.clear();
        String cacheJson = prefs.getString(KEY_MD5_CACHE, "[]");
        try {
            JSONArray array = new JSONArray(cacheJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                md5Cache.add(new Md5CacheItem(
                    obj.getString("hash"),
                    obj.getString("filename"),
                    obj.getLong("timestamp")
                ));
            }
        } catch (Exception e) {
            md5Cache = new ArrayList<>();
        }
    }

    private void initViews() {
        languageSelector = findViewById(R.id.languageSelector);
        languageText = findViewById(R.id.languageText);
        siteScroll = findViewById(R.id.siteScroll);
        siteContainer = findViewById(R.id.siteContainer);
        webView = findViewById(R.id.mainWebView);
        addButton = findViewById(R.id.addButton);

        String[] languageCodes = {"EN", "ZH", "JA", "KO", "ES", "FR", "DE", "PT", "RU", "AR", "HI", "BN", "PA", "JV", "MR", "TR", "IT", "PL", "UK", "NL"};
        String[] languages = {
            getString(R.string.language_en),
            getString(R.string.language_zh),
            getString(R.string.language_ja),
            getString(R.string.language_ko),
            getString(R.string.language_es),
            getString(R.string.language_fr),
            getString(R.string.language_de),
            getString(R.string.language_pt),
            getString(R.string.language_ru),
            getString(R.string.language_ar),
            getString(R.string.language_hi),
            getString(R.string.language_bn),
            getString(R.string.language_pa),
            getString(R.string.language_jv),
            getString(R.string.language_mr),
            getString(R.string.language_tr),
            getString(R.string.language_it),
            getString(R.string.language_pl),
            getString(R.string.language_uk),
            getString(R.string.language_nl)
        };
        
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLanguage)) {
                languageText.setText(languages[i]);
                break;
            }
        }
        
        languageSelector.setOnClickListener(v -> showLanguageDialog());
        addButton.setOnClickListener(v -> showAddSiteDialog());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            webView.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets safeInsets = insets.getInsets(WindowInsets.Type.systemBars());
                int topInset = safeInsets.top;
                int bottomInset = safeInsets.bottom;
                v.setPadding(0, topInset, 0, bottomInset);
                return insets;
            });
        }
    }

    private void initWebView() {
        android.webkit.WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        String langTag = getUaLanguageCode(currentLanguage);
        String customUA = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + 
            "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36 " +
            "Accept-Language: " + langTag;
        settings.setUserAgentString(customUA);
        
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        
        webView.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(android.webkit.WebView view, android.webkit.WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onReceivedSslError(android.webkit.WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                handler.proceed();
            }
        });
        
        webView.setWebChromeClient(new android.webkit.WebChromeClient());
    }

    private void initClipboard() {
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(() -> {
            if (activeSite != null && clipboardManager.hasPrimaryClip()) {try {
                CharSequence text = clipboardManager.getPrimaryClip().getItemAt(0).getText();
                if (text != null && text.length() > 0) {
                    handleAutoSave(text.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            }
        });
    }

    private List<Site> getDefaultSites() {
        List<Site> defaultSites = new ArrayList<>();
        defaultSites.add(new Site("TRAE SOLO", "https://solo.trae.cn", "TRAE\nSOLO", "#5C61FF", true));
        defaultSites.add(new Site("DeepSeek", "https://chat.deepseek.com/sign_in", "Deep\nSeek", "#00D4AA", true));
        defaultSites.add(new Site("豆包", "https://www.doubao.com/chat", "豆包", "#1890FF", false));
        defaultSites.add(new Site("Kimi", "https://www.kimi.com", "K", "#FF6B6B", false));
        defaultSites.add(new Site("NotebookLM", "https://notebooklm.google.com", "Note\nBook", "#4285F4", true));
        return defaultSites;
    }

    private void initDefaultSites() {
        if (sites == null || sites.isEmpty()) {
            sites = getDefaultSites();
        }
    }

    private void loadSitesFromJson(String json) throws Exception {
        sites = new ArrayList<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            boolean allowWrap = obj.optBoolean("allowWrap", false);
            sites.add(new Site(
                obj.getString("name"),
                obj.getString("url"),
                obj.getString("icon"),
                obj.getString("color"),
                allowWrap
            ));
        }
    }

    private void saveSitesToPrefs() {
        try {
            JSONArray array = new JSONArray();
            for (Site site : sites) {
                JSONObject obj = new JSONObject();
                obj.put("name", site.name);
                obj.put("url", site.url);
                obj.put("icon", site.icon);
                obj.put("color", site.color);
                obj.put("allowWrap", site.allowWrap);
                array.put(obj);
            }
            prefs.edit().putString(KEY_SITES, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void renderSites() {
        siteContainer.removeAllViews();
        
        for (int i = 0; i < sites.size(); i++) {
            Site site = sites.get(i);
            TextView iconView = createSiteIcon(site);
            siteContainer.addView(iconView);
        }
    }

    private static final int CIRCLE_SIZE_DP = 52;

    private TextView createSiteIcon(Site site) {
        TextView textView = new TextView(this);
        
        int circleSize = dpToPx(CIRCLE_SIZE_DP);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(circleSize, circleSize);
        params.setMargins(0, 0, dpToPx(12), 0);
        textView.setLayoutParams(params);
        
        textView.setGravity(Gravity.CENTER);
        textView.setText(site.icon);
        textView.setTextColor(Color.WHITE);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        updateIconTextSize(textView, site.icon, site.allowWrap);
        
        textView.setBackgroundResource(R.drawable.circle_bg);
        
        int color;
        try {
            color = Color.parseColor(site.color);
        } catch (Exception e) {
            color = Color.parseColor("#5C61FF");
        }
        
        boolean isActive = activeSite != null && activeSite.url.equals(site.url);
        
        if (isActive) {
            textView.setBackgroundTintList(ColorStateList.valueOf(color));
        } else {
            textView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        }
        
        textView.setOnClickListener(v -> selectSite(site));
        
        textView.setOnLongClickListener(v -> {
            showEditDeleteDialog(site);
            return true;
        });
        
        return textView;
    }

    private void updateIconTextSize(TextView textView, String text, boolean allowWrap) {
        if (text == null || text.isEmpty()) {
            textView.setTextSize(18);
            return;
        }

        int charCount = text.length();
        int maxLines = allowWrap ? 2 : 1;
        
        float fontSize;
        
        if (charCount <= 1) {
            fontSize = 24;
        } else if (charCount == 2) {
            fontSize = 18;
        } else if (charCount <= 4) {
            fontSize = allowWrap ? 14 : 12;
        } else {
            fontSize = allowWrap ? 12 : 10;
        }

        textView.setTextSize(fontSize);
        textView.setMaxLines(maxLines);
        textView.setEllipsize(null);
    }

    private int darkenColor(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor;
        return Color.HSVToColor(hsv);
    }

    private void selectSite(Site site) {
        activeSite = site;
        prefs.edit().putString(KEY_ACTIVE_SITE_URL, site.url).apply();
        renderSites();
        webView.loadUrl(site.url);
    }

    private void changeLanguage(String languageCode) {
        currentLanguage = languageCode;
        prefs.edit().putString(KEY_LANG, currentLanguage).apply();
        
        if (siteScroll != null) {
            prefs.edit().putInt(KEY_SCROLL_X, siteScroll.getScrollX()).apply();
        }
        
        finish();
        startActivity(getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        overridePendingTransition(0, 0);
    }

    private void showLanguageDialog() {
        final String[] languages = {
            getString(R.string.language_en),
            getString(R.string.language_zh),
            getString(R.string.language_ja),
            getString(R.string.language_ko),
            getString(R.string.language_es),
            getString(R.string.language_fr),
            getString(R.string.language_de),
            getString(R.string.language_pt),
            getString(R.string.language_ru),
            getString(R.string.language_ar),
            getString(R.string.language_hi),
            getString(R.string.language_bn),
            getString(R.string.language_pa),
            getString(R.string.language_jv),
            getString(R.string.language_mr),
            getString(R.string.language_tr),
            getString(R.string.language_it),
            getString(R.string.language_pl),
            getString(R.string.language_uk),
            getString(R.string.language_nl)
        };
        
        final String[] languageCodes = {"EN", "ZH", "JA", "KO", "ES", "FR", "DE", "PT", "RU", "AR", "HI", "BN", "PA", "JV", "MR", "TR", "IT", "PL", "UK", "NL"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LanguageDialogStyle);
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_language, null);
        
        LinearLayout languageList = dialogView.findViewById(R.id.languageList);
        android.widget.ImageButton closeButton = dialogView.findViewById(R.id.closeButton);
        
        for (int i = 0; i < languages.length; i++) {
            final int position = i;
            TextView languageItem = new TextView(this);
            languageItem.setText(languages[i]);
            languageItem.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
            languageItem.setTextSize(16);
            
            if (languageCodes[i].equals(currentLanguage)) {
                languageItem.setBackgroundColor(Color.parseColor("#F5F5F5"));
                languageItem.setTextColor(Color.parseColor("#9E9E9E"));
                languageItem.setEnabled(false);
            } else {
                languageItem.setBackgroundColor(Color.TRANSPARENT);
                languageItem.setTextColor(Color.parseColor("#333333"));
                languageItem.setOnClickListener(v -> {
                    changeLanguage(languageCodes[position]);
                });
                languageItem.setBackgroundResource(R.drawable.list_item_selector);
            }
            
            languageList.addView(languageItem);
        }
        
        final AlertDialog dialog = builder.create();
        dialog.setView(dialogView);
        
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            android.view.WindowManager.LayoutParams lp = window.getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.66);
            lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.60);
            lp.gravity = Gravity.CENTER;
            window.setAttributes(lp);
            window.setBackgroundDrawableResource(R.drawable.dialog_rounded_bg);
            
            // Make dialog draggable
            dialogView.setOnTouchListener(new View.OnTouchListener() {
                private float initialX, initialY;
                private float initialTouchX, initialTouchY;
                
                @Override
                public boolean onTouch(View v, android.view.MotionEvent event) {
                    switch (event.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            initialX = lp.x;
                            initialY = lp.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case android.view.MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - initialTouchX;
                            float dy = event.getRawY() - initialTouchY;
                            lp.x = (int) (initialX + dx);
                            lp.y = (int) (initialY + dy);
                            window.setAttributes(lp);
                            return true;
                        case android.view.MotionEvent.ACTION_UP:
                            return true;
                    }
                    return false;
                }
            });
        }
    }

    private void showAddSiteDialog() {
        showSiteDialog(null);
    }

    private void showEditDeleteDialog(Site site) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.action))
            .setItems(new String[]{getString(R.string.edit), getString(R.string.delete)}, (dialog, which) -> {
                if (which == 0) {
                    showSiteDialog(site);
                } else {
                    deleteSite(site);
                }
            })
            .show();
    }

    private void showSiteDialog(Site editSite) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(editSite == null ? getString(R.string.add_site) : getString(R.string.edit_site));
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), dpToPx(8));
        
        EditText nameInput = new EditText(this);
        nameInput.setHint(getString(R.string.site_name));
        if (editSite != null) nameInput.setText(editSite.name);
        layout.addView(nameInput);
        
        EditText urlInput = new EditText(this);
        urlInput.setHint(getString(R.string.site_url));
        urlInput.setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI);
        if (editSite != null) urlInput.setText(editSite.url);
        layout.addView(urlInput);
        
        TextView iconHint = new TextView(this);
        iconHint.setText(getString(R.string.site_short_name));
        iconHint.setTextSize(12);
        iconHint.setTextColor(Color.GRAY);
        iconHint.setPadding(0, dpToPx(16), 0, 0);
        layout.addView(iconHint);
        
        EditText iconInput = new EditText(this);
        iconInput.setHint(getString(R.string.site_short_name_hint));
        if (editSite != null) iconInput.setText(editSite.icon);
        layout.addView(iconInput);
        
        CheckBox wrapCheckBox = new CheckBox(this);
        wrapCheckBox.setText(getString(R.string.allow_wrap));
        wrapCheckBox.setPadding(0, dpToPx(16), 0, 0);
        if (editSite != null) wrapCheckBox.setChecked(editSite.allowWrap);
        layout.addView(wrapCheckBox);
        
        builder.setView(layout);
        
        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String url = urlInput.getText().toString().trim();
            String icon = iconInput.getText().toString().trim();
            boolean allowWrap = wrapCheckBox.isChecked();
            
            if (!name.isEmpty() && !url.isEmpty()) {
                if (icon.isEmpty()) {
                    icon = getIconFromName(name);
                }
                
                if (editSite != null) {
                    editSite.name = name;
                    editSite.url = url;
                    editSite.icon = icon;
                    editSite.allowWrap = allowWrap;
                } else {
                    sites.add(new Site(name, url, icon, "#5C61FF", allowWrap));
                }
                saveSitesToPrefs();
                renderSites();
                if (editSite == null && sites.size() == 1) {
                    selectSite(sites.get(0));
                }
            }
        });
        
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void deleteSite(Site site) {
        sites.remove(site);
        saveSitesToPrefs();
        renderSites();
        
        if (activeSite != null && activeSite.url.equals(site.url)) {
            if (!sites.isEmpty()) {
                selectSite(sites.get(0));
            } else {
                activeSite = null;
                prefs.edit().remove(KEY_ACTIVE_SITE_URL).apply();
                webView.loadUrl("about:blank");
            }
        }
    }

    private String getIconFromName(String name) {
        if (name == null || name.isEmpty()) return "?";
        
        for (char c : name.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                return String.valueOf(c);
            }
        }
        
        return name.substring(0, 1).toUpperCase();
    }

    private void handleAutoSave(String content) {
        if (activeSite == null || content == null || content.trim().isEmpty()) {
            return;
        }
        
        if (content.length() < MIN_CONTENT_LENGTH) {
            return;
        }
        
        pendingContent = content;
        
        if (checkPermission()) {
            processSave(content);
        } else {
            boolean alwaysRemind = prefs.getBoolean(KEY_ALWAYS_REMIND, true);
            if (!alwaysRemind) {
                return;
            }
            showPermissionDialog();
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.permission_title));
        builder.setMessage(getString(R.string.permission_message));
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(8));
        
        CheckBox checkBox = new CheckBox(this);
        checkBox.setText(getString(R.string.always_remind));
        checkBox.setChecked(prefs.getBoolean(KEY_ALWAYS_REMIND, true));
        layout.addView(checkBox);
        
        builder.setView(layout);
        
        builder.setPositiveButton(getString(R.string.allow), (dialog, which) -> {
            prefs.edit().putBoolean(KEY_ALWAYS_REMIND, checkBox.isChecked()).apply();
            prefs.edit().putString(KEY_PERMISSION, "granted").apply();
            requestPermission();
        });
        
        builder.setNegativeButton(getString(R.string.deny), (dialog, which) -> {
            prefs.edit().putBoolean(KEY_ALWAYS_REMIND, checkBox.isChecked()).apply();
            prefs.edit().putString(KEY_PERMISSION, "denied").apply();
        });
        
        builder.show();
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    android.net.Uri.parse("package:" + getPackageName())
                );
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(
                    android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                );
                startActivity(intent);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingContent != null) {
                    processSave(pendingContent);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (pendingContent != null && checkPermission()) {
            processSave(pendingContent);
            pendingContent = null;
        }
    }

    private void processSave(String content) {
        String md5Hash = calculateMd5(content);
        
        Md5CacheItem existingItem = findInMd5Cache(md5Hash);
        if (existingItem != null) {
            showDuplicateDialog(content, md5Hash, existingItem.filename);
            return;
        }
        
        performSave(content, md5Hash);
    }

    private String calculateMd5(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(content.hashCode());
        }
    }

    private Md5CacheItem findInMd5Cache(String hash) {
        for (Md5CacheItem item : md5Cache) {
            if (item.hash.equals(hash)) {
                return item;
            }
        }
        return null;
    }

    private void showDuplicateDialog(String content, String md5Hash, String existingFilename) {
        new AlertDialog.Builder(this)
            .setTitle(getString(R.string.duplicate_title))
            .setMessage(String.format(getString(R.string.duplicate_message), existingFilename))
            .setPositiveButton(getString(R.string.save), (dialog, which) -> {
                performSave(content, md5Hash);
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .show();
    }

    private void performSave(String content, String md5Hash) {
        String filename = generateFilename(activeSite.name);
        
        boolean success = saveToFile(filename, content);
        
        if (success) {
            addToMd5Cache(md5Hash, filename);
            addToRecentSaves(filename);
            showSaveToast(filename, true);
        } else {
            showSaveToast(filename, false);
        }
    }

    private boolean saveToFile(String filename, String content) {
        try {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "MTC-Saves");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            File file = new File(dir, filename);
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            writer.write(content);
            writer.close();
            fos.close();
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addToMd5Cache(String hash, String filename) {
        md5Cache.add(0, new Md5CacheItem(hash, filename, System.currentTimeMillis()));
        
        while (md5Cache.size() > MAX_MD5_CACHE) {
            md5Cache.remove(md5Cache.size() - 1);
        }
        
        saveMd5Cache();
    }

    private void addToRecentSaves(String filename) {
        recentSaves.add(0, new SaveRecord(filename, System.currentTimeMillis()));
        
        while (recentSaves.size() > MAX_RECENT_SAVES) {
            recentSaves.remove(recentSaves.size() - 1);
        }
        
        saveRecentSaves();
    }

    private void saveMd5Cache() {
        try {
            JSONArray array = new JSONArray();
            for (Md5CacheItem item : md5Cache) {
                JSONObject obj = new JSONObject();
                obj.put("hash", item.hash);
                obj.put("filename", item.filename);
                obj.put("timestamp", item.timestamp);
                array.put(obj);
            }
            prefs.edit().putString(KEY_MD5_CACHE, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveRecentSaves() {
        try {
            JSONArray array = new JSONArray();
            for (SaveRecord record : recentSaves) {
                JSONObject obj = new JSONObject();
                obj.put("filename", record.filename);
                obj.put("timestamp", record.timestamp);
                array.put(obj);
            }
            prefs.edit().putString(KEY_SAVES, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AlertDialog saveResultDialog = null;

    private void showSaveToast(String currentFilename, boolean success) {
        runOnUiThread(() -> {
            if (saveResultDialog != null && saveResultDialog.isShowing()) {
                saveResultDialog.dismiss();
            }
            
            LinearLayout dialogLayout = new LinearLayout(this);
            dialogLayout.setOrientation(LinearLayout.VERTICAL);
            dialogLayout.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(8));
            
            TextView headerView = new TextView(this);
            headerView.setText(success ? getString(R.string.auto_save_success) : getString(R.string.save_failed));
            headerView.setTextColor(success ? Color.parseColor("#5C61FF") : Color.parseColor("#FF6B6B"));
            headerView.setTextSize(18);
            headerView.setTypeface(null, android.graphics.Typeface.BOLD);
            headerView.setPadding(0, 0, 0, dpToPx(16));
            dialogLayout.addView(headerView);
            
            ScrollView scrollView = new ScrollView(this);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(200)));
            
            LinearLayout listLayout = new LinearLayout(this);
            listLayout.setOrientation(LinearLayout.VERTICAL);
            
            boolean isCurrent = true;
            for (SaveRecord record : recentSaves) {
                TextView itemView = new TextView(this);
                String prefix = isCurrent && success ? "[*] " : "[-] ";
                itemView.setText(prefix + record.filename);
                itemView.setTextSize(14);
                
                if (isCurrent && success) {
                    itemView.setTextColor(Color.parseColor("#5C61FF"));
                    itemView.setBackgroundColor(Color.parseColor("#1A1A3A"));
                    itemView.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    itemView.setTextColor(Color.parseColor("#888888"));
                }
                
                itemView.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
                listLayout.addView(itemView);
                
                isCurrent = false;
            }
            
            scrollView.addView(listLayout);
            dialogLayout.addView(scrollView);
            
            TextView timerView = new TextView(this);
            timerView.setText(String.format(getString(R.string.seconds_close), 3));
            timerView.setTextColor(Color.parseColor("#666666"));
            timerView.setTextSize(12);
            timerView.setGravity(Gravity.CENTER);
            timerView.setPadding(0, dpToPx(16), 0, 0);
            dialogLayout.addView(timerView);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog);
            builder.setView(dialogLayout);
            builder.setCancelable(true);
            
            saveResultDialog = builder.create();
            saveResultDialog.getWindow().setBackgroundDrawableResource(android.R.drawable.dialog_holo_dark_frame);
            saveResultDialog.show();
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (saveResultDialog != null && saveResultDialog.isShowing()) {
                    saveResultDialog.dismiss();
                }
            }, 3000);
        });
    }

    private String generateFilename(String siteName) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmm", Locale.getDefault());
        String timeStr = sdf.format(new Date());
        
        int counter = siteCounters.getOrDefault(siteName + "-" + timeStr, 1);
        siteCounters.put(siteName + "-" + timeStr, counter + 1);
        
        return siteName + "-" + timeStr + "-" + counter + ".md";
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    static class Site {
        String name;
        String url;
        String icon;
        String color;
        boolean allowWrap;

        Site(String name, String url, String icon, String color) {
            this(name, url, icon, color, false);
        }

        Site(String name, String url, String icon, String color, boolean allowWrap) {
            this.name = name;
            this.url = url;
            this.icon = icon;
            this.color = color;
            this.allowWrap = allowWrap;
        }
    }

    static class SaveRecord {
        String filename;
        long timestamp;

        SaveRecord(String filename, long timestamp) {
            this.filename = filename;
            this.timestamp = timestamp;
        }
    }

    static class Md5CacheItem {
        String hash;
        String filename;
        long timestamp;

        Md5CacheItem(String hash, String filename, long timestamp) {
            this.hash = hash;
            this.filename = filename;
            this.timestamp = timestamp;
        }
    }
}
