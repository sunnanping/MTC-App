package cn.trae.mtc;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

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
        
        // 加载保存的网站
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
        
        // 加载最近保存
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

        // Edge-to-Edge 适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            webView.setOnApplyWindowInsetsListener((v, insets) -> {
                WindowInsets safeInsets = insets.getInsets(WindowInsets.Type.systemBars());
                int topInset = safeInsets.top;
                int bottomInset = safeInsets.bottom;
                
                // 给 webView 添加上下内边距
                v.setPadding(0, topInset, 0, bottomInset);
                
                return insets;
            });
        }
    }

    private void initWebView() {
        WebSettings settings = webView.getSettings();
        
        // 基本设置
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        // 修改 User-Agent 模拟浏览器
        String originalUA = settings.getUserAgentString();
        String customUA = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + 
            "; " + Build.MODEL + ") AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36";
        settings.setUserAgentString(customUA);
        
        // Cookie 支持
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        
        // 混合内容
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        
        // WebViewClient 和 WebChromeClient
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
            sites.add(new Site(
                obj.getString("name"),
                obj.getString("url"),
                obj.getString("icon"),
                obj.getString("color")
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

    private TextView createSiteIcon(Site site) {
        TextView textView = new TextView(this);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            dpToPx(48),
            dpToPx(48)
        );
        params.setMargins(0, 0, dpToPx(12), 0);
        textView.setLayoutParams(params);
        
        textView.setGravity(Gravity.CENTER);
        textView.setText(site.icon);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(18);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);
        
        // 设置背景颜色
        try {
            textView.setBackgroundColor(Color.parseColor(site.color));
        } catch (Exception e) {
            textView.setBackgroundColor(Color.parseColor("#5C61FF"));
        }
        
        // 设置圆形背景
        textView.setBackgroundResource(R.drawable.circle_bg);
        // 动态修改颜色
        textView.setBackgroundTintList(
            androidx.core.content.res.ColorStateList.valueOf(Color.parseColor(site.color))
        );
        
        // 激活状态样式
        if (activeSite != null && activeSite.url.equals(site.url)) {
            textView.setScaleX(1.1f);
            textView.setScaleY(1.1f);
            textView.setBackgroundTintList(
                androidx.core.content.res.ColorStateList.valueOf(Color.parseColor(site.color))
            );
            // 添加白色边框
            textView.setStrokeWidth(2);
        }
        
        textView.setOnClickListener(v -> selectSite(site));
        
        // 长按编辑/删除
        textView.setOnLongClickListener(v -> {
            showEditDeleteDialog(site);
            return true;
        });
        
        return textView;
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
        
        EditText urlInput = new EditText(this);
        urlInput.setHint("网址（https://...）");
        urlInput.setInputType(EditorInfo.TYPE_TEXT_VARIATION_WEB_EMAIL);
        if (editSite != null) urlInput.setText(editSite.url);
        
        layout.addView(nameInput);
        layout.addView(urlInput);
        
        builder.setView(layout);
        
        builder.setPositiveButton("保存", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String url = urlInput.getText().toString().trim();
            
            if (!name.isEmpty() && !url.isEmpty()) {
                if (editSite != null) {
                    editSite.name = name;
                    editSite.url = url;
                    editSite.icon = getIconFromName(name);
                } else {
                    sites.add(new Site(name, url, getIconFromName(name), "#5C61FF"));
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
        
        // 检查是否有中文字符
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
        
        // 生成文件名
        String filename = generateFilename(activeSite.name);
        
        // 下载文件
        downloadMarkdown(filename, content);
        
        // 记录到最近保存
        recentSaves.add(0, filename);
        if (recentSaves.size() > 10) {
            recentSaves.remove(recentSaves.size() - 1);
        }
        saveRecentSaves();
        
        // 显示通知
        showToastNotification(filename, true);
    }

    private String generateFilename(String siteName) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmm", Locale.getDefault());
        String timeStr = sdf.format(new Date());
        
        // 获取该网站的计数器
        int counter = siteCounters.getOrDefault(siteName + "-" + timeStr, 1);
        siteCounters.put(siteName + "-" + timeStr, counter + 1);
        
        return siteName + "-" + timeStr + "-" + counter + ".md";
    }

    private void downloadMarkdown(String filename, String content) {
        // 创建一个临时网页用于下载
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
        // 创建一个简单的通知
        runOnUiThread(() -> {
            // 显示 Toast
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

        Site(String name, String url, String icon, String color) {
            this.name = name;
            this.url = url;
            this.icon = icon;
            this.color = color;
        }
    }
}
