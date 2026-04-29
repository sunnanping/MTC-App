package cn.trae.mtc;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

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

    private List<Site> sites;
    private Site activeSite;
    private String currentLanguage = "EN";
    private List<String> recentSaves = new ArrayList<>();
    private Map<String, Integer> siteCounters = new HashMap<>();

    private LinearLayout languageSelector;
    private TextView languageText;
    private LinearLayout siteContainer;
    private WebView webView;
    private ClipboardManager clipboardManager;
    private SharedPreferences prefs;
    private View addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initPreferences();
        initViews();
        initWebView();
        initClipboard();
        initDefaultSites();
        renderSites();
        if (!sites.isEmpty()) {
            selectSite(sites.get(0));
        }
    }

    private void initPreferences() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentLanguage = prefs.getString(KEY_LANG, "EN");
        
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
        
        String savesJson = prefs.getString(KEY_SAVES, "[]");
        try {
            JSONArray array = new JSONArray(savesJson);
            for (int i = 0; i < array.length(); i++) {
                recentSaves.add(array.getString(i));
            }
        } catch (Exception e) {
            recentSaves = new ArrayList<>();
        }
    }

    private void initViews() {
        languageSelector = findViewById(R.id.languageSelector);
        languageText = findViewById(R.id.languageText);
        siteContainer = findViewById(R.id.siteContainer);
        webView = findViewById(R.id.mainWebView);
        addButton = findViewById(R.id.addButton);

        languageText.setText(currentLanguage);
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
        WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        String customUA = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + 
            "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36";
        settings.setUserAgentString(customUA);
        
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient());
    }

    private void initClipboard() {
        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(() -> {
            if (activeSite != null && clipboardManager.hasPrimaryClip()) {
                try {
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
        defaultSites.add(new Site("TRAE SOLO", "https://solo.trae.cn", "T", "#5C61FF"));
        defaultSites.add(new Site("DeepSeek", "https://chat.deepseek.com", "D", "#00D4AA"));
        defaultSites.add(new Site("豆包", "https://www.doubao.com/chat", "豆", "#1890FF"));
        defaultSites.add(new Site("Kimi", "https://www.kimi.com", "K", "#FF6B6B"));
        defaultSites.add(new Site("NotebookLM", "https://notebooklm.google.com", "N", "#4285F4"));
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
        
        try {
            int color = Color.parseColor(site.color);
            textView.setBackgroundTintList(ColorStateList.valueOf(color));
        } catch (Exception e) {
            textView.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#5C61FF")));
        }
        
        if (activeSite != null && activeSite.url.equals(site.url)) {
            textView.setScaleX(1.1f);
            textView.setScaleY(1.1f);
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

    private void selectSite(Site site) {
        activeSite = site;
        renderSites();
        webView.loadUrl(site.url);
    }

    private void showLanguageDialog() {
        String[] languages = {"EN", "中文", "日本語", "한국어", "Español", "Français", "Deutsch", "Português", "Русский", "العربية"};
        
        new AlertDialog.Builder(this)
            .setTitle("选择语言 / Select Language")
            .setItems(languages, (dialog, which) -> {
                currentLanguage = languages[which];
                languageText.setText(currentLanguage);
                prefs.edit().putString(KEY_LANG, currentLanguage).apply();
            })
            .show();
    }

    private void showAddSiteDialog() {
        showSiteDialog(null);
    }

    private void showEditDeleteDialog(Site site) {
        new AlertDialog.Builder(this)
            .setTitle("操作 / Action")
            .setItems(new String[]{"编辑 / Edit", "删除 / Delete"}, (dialog, which) -> {
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
        builder.setTitle(editSite == null ? "添加网站" : "编辑网站");
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(8), dpToPx(24), dpToPx(8));
        
        EditText nameInput = new EditText(this);
        nameInput.setHint("网站名称");
        if (editSite != null) nameInput.setText(editSite.name);
        layout.addView(nameInput);
        
        EditText urlInput = new EditText(this);
        urlInput.setHint("网址（https://...）");
        urlInput.setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI);
        if (editSite != null) urlInput.setText(editSite.url);
        layout.addView(urlInput);
        
        TextView iconHint = new TextView(this);
        iconHint.setText("网站简写（1-4个字符）");
        iconHint.setTextSize(12);
        iconHint.setTextColor(Color.GRAY);
        iconHint.setPadding(0, dpToPx(16), 0, 0);
        layout.addView(iconHint);
        
        EditText iconInput = new EditText(this);
        iconInput.setHint("例如：D 或 豆");
        iconInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});
        if (editSite != null) iconInput.setText(editSite.icon);
        layout.addView(iconInput);
        
        CheckBox wrapCheckBox = new CheckBox(this);
        wrapCheckBox.setText("允许换行显示");
        wrapCheckBox.setPadding(0, dpToPx(16), 0, 0);
        if (editSite != null) wrapCheckBox.setChecked(editSite.allowWrap);
        layout.addView(wrapCheckBox);
        
        builder.setView(layout);
        
        builder.setPositiveButton("保存", (dialog, which) -> {
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
        
        builder.setNegativeButton("取消", null);
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
        
        String filename = generateFilename(activeSite.name);
        
        downloadMarkdown(filename, content);
        
        recentSaves.add(0, filename);
        if (recentSaves.size() > 10) {
            recentSaves.remove(recentSaves.size() - 1);
        }
        saveRecentSaves();
        
        showToastNotification(filename, true);
    }

    private String generateFilename(String siteName) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmm", Locale.getDefault());
        String timeStr = sdf.format(new Date());
        
        int counter = siteCounters.getOrDefault(siteName + "-" + timeStr, 1);
        siteCounters.put(siteName + "-" + timeStr, counter + 1);
        
        return siteName + "-" + timeStr + "-" + counter + ".md";
    }

    private void downloadMarkdown(String filename, String content) {
        String html = "<html>" +
            "<head><title>Download</title></head>" +
            "<body>" +
            "<a id='downloadLink' href='data:text/markdown;charset=utf-8," + 
            content.replace("'", "\\'").replace("\"", "\\\"") + 
            "' download='" + filename + "'></a>" +
            "<script>document.getElementById('downloadLink').click();</script>" +
            "</body></html>";
        
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private void showToastNotification(String filename, boolean success) {
        runOnUiThread(() -> {
            android.widget.Toast.makeText(
                this,
                success ? "保存成功 / Saved: " + filename : "保存失败 / Save failed",
                android.widget.Toast.LENGTH_LONG
            ).show();
        });
    }

    private void saveRecentSaves() {
        try {
            JSONArray array = new JSONArray(recentSaves);
            prefs.edit().putString(KEY_SAVES, array.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
}
