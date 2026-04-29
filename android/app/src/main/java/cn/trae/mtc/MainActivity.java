package cn.trae.mtc;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.cardview.widget.CardView;

import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BridgeActivity {

    private static final String UA_REAL = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    private WebView originalWebView;
    private FrameLayout rootContainer;
    private FrameLayout navBarContainer;
    private LinearLayout navContainer;
    private FrameLayout contentContainer;
    private Map<String, WebView> webViewMap = new HashMap<>();
    private WebView activeWebView;
    private String activeSiteId;
    private boolean isShowingExternal = false;

    private List<SiteInfo> sites = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sites.add(new SiteInfo("1", "TRAE SOLO", "https://solo.trae.cn", "T", "#5C61FF"));
        sites.add(new SiteInfo("2", "DeepSeek", "https://chat.deepseek.com/sign_in", "D", "#00D4AA"));
        sites.add(new SiteInfo("3", "豆包", "https://www.doubao.com/chat", "豆", "#1890FF"));
        sites.add(new SiteInfo("4", "Kimi", "https://www.kimi.com", "K", "#FF6B6B"));
        sites.add(new SiteInfo("5", "NotebookLM", "https://notebooklm.google.com", "N", "#4285F4"));
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            setupNativeNavBar();
            configureCapacitorWebView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupNativeNavBar() {
        if (bridge == null) return;
        
        WebView webView = bridge.getWebView();
        if (webView == null) return;

        originalWebView = webView;

        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent == null) return;

        int index = parent.indexOfChild(webView);
        parent.removeView(webView);

        rootContainer = new FrameLayout(this);
        rootContainer.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
        rootContainer.setBackgroundColor(Color.parseColor("#1A1A1A"));

        contentContainer = new FrameLayout(this);
        contentContainer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        contentContainer.addView(webView);
        rootContainer.addView(contentContainer);

        navBarContainer = new FrameLayout(this);
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        navParams.gravity = Gravity.TOP;
        navBarContainer.setLayoutParams(navParams);
        navBarContainer.setBackgroundColor(Color.parseColor("#1A1A1A"));

        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        navContainer = new LinearLayout(this);
        navContainer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        navContainer.setOrientation(LinearLayout.HORIZONTAL);
        navContainer.setGravity(Gravity.CENTER_VERTICAL);

        scrollView.addView(navContainer);
        navBarContainer.addView(scrollView);
        rootContainer.addView(navBarContainer);

        renderNavIcons();

        parent.addView(rootContainer, index);

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, windowInsets) -> {
            try {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) navBarContainer.getLayoutParams();
                lp.topMargin = insets.top;
                navBarContainer.setLayoutParams(lp);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return windowInsets;
        });
    }

    private void renderNavIcons() {
        if (navContainer == null) return;
        navContainer.removeAllViews();

        for (SiteInfo site : sites) {
            View iconView = createSiteIconView(site);
            navContainer.addView(iconView);
        }

        View addBtn = createAddButton();
        navContainer.addView(addBtn);
    }

    private View createSiteIconView(SiteInfo site) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        params.setMargins(0, 0, dpToPx(12), 0);
        cardView.setLayoutParams(params);
        cardView.setRadius(dpToPx(24));
        cardView.setCardBackgroundColor(Color.parseColor(site.color));
        cardView.setCardElevation(0);

        TextView textView = new TextView(this);
        textView.setText(site.icon);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(18);
        textView.setGravity(Gravity.CENTER);
        textView.setTypeface(null, android.graphics.Typeface.BOLD);

        cardView.addView(textView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        cardView.setOnClickListener(v -> selectSite(site));

        cardView.setOnLongClickListener(v -> {
            showSiteOptions(site);
            return true;
        });

        return cardView;
    }

    private View createAddButton() {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        cardView.setLayoutParams(params);
        cardView.setRadius(dpToPx(24));
        cardView.setCardBackgroundColor(Color.parseColor("#333333"));
        cardView.setCardElevation(0);

        TextView textView = new TextView(this);
        textView.setText("+");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(24);
        textView.setGravity(Gravity.CENTER);

        cardView.addView(textView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        cardView.setOnClickListener(v -> showAddSiteDialog());

        return cardView;
    }

    private void selectSite(SiteInfo site) {
        activeSiteId = site.id;
        isShowingExternal = true;

        if (contentContainer != null) {
            contentContainer.removeAllViews();
        }

        WebView webView = getOrCreateWebView(site);
        activeWebView = webView;

        if (contentContainer != null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            params.topMargin = dpToPx(72);
            contentContainer.addView(webView, params);
        }

        Toast.makeText(this, "已打开: " + site.name, Toast.LENGTH_SHORT).show();
    }

    private WebView getOrCreateWebView(SiteInfo site) {
        if (webViewMap.containsKey(site.id)) {
            return webViewMap.get(site.id);
        }

        WebView webView = new WebView(this);
        configureExternalWebView(webView);
        webView.loadUrl(site.url);
        webViewMap.put(site.id, webView);

        return webView;
    }

    private void showSiteOptions(SiteInfo site) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(site.name);
        builder.setItems(new CharSequence[]{"编辑", "删除"}, (dialog, which) -> {
            if (which == 0) {
                showEditSiteDialog(site);
            } else {
                showDeleteConfirm(site);
            }
        });
        builder.show();
    }

    private void showAddSiteDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("添加网站");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(10));

        final TextView nameHint = new TextView(this);
        nameHint.setText("网站名称");
        nameHint.setTextSize(12);
        nameHint.setTextColor(Color.GRAY);
        layout.addView(nameHint);

        final android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setHint("例如: DeepSeek");
        layout.addView(nameInput);

        final TextView urlHint = new TextView(this);
        urlHint.setText("网站地址");
        urlHint.setTextSize(12);
        urlHint.setTextColor(Color.GRAY);
        urlHint.setPadding(0, dpToPx(16), 0, 0);
        layout.addView(urlHint);

        final android.widget.EditText urlInput = new android.widget.EditText(this);
        urlInput.setHint("https://example.com");
        layout.addView(urlInput);

        builder.setView(layout);

        builder.setPositiveButton("添加", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String url = urlInput.getText().toString().trim();

            if (!name.isEmpty() && !url.isEmpty()) {
                String icon = name.substring(0, 1).toUpperCase();
                String color = String.format("#%06X", (0xFFFFFF & ((int)(Math.random() * 0xFFFFFF))));

                SiteInfo newSite = new SiteInfo(
                    String.valueOf(System.currentTimeMillis()),
                    name,
                    url,
                    icon,
                    color
                );
                sites.add(newSite);
                renderNavIcons();
                Toast.makeText(this, "已添加: " + name, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showEditSiteDialog(SiteInfo site) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("编辑网站");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(10));

        final android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setText(site.name);
        layout.addView(nameInput);

        final android.widget.EditText urlInput = new android.widget.EditText(this);
        urlInput.setText(site.url);
        layout.addView(urlInput);

        builder.setView(layout);

        builder.setPositiveButton("保存", (dialog, which) -> {
            site.name = nameInput.getText().toString().trim();
            site.url = urlInput.getText().toString().trim();
            if (!site.name.isEmpty()) {
                site.icon = site.name.substring(0, 1).toUpperCase();
            }
            renderNavIcons();
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showDeleteConfirm(SiteInfo site) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("确认删除");
        builder.setMessage("确定要删除 \"" + site.name + "\" 吗？");
        builder.setPositiveButton("删除", (dialog, which) -> {
            sites.remove(site);
            webViewMap.remove(site.id);
            renderNavIcons();
            Toast.makeText(this, "已删除: " + site.name, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void configureCapacitorWebView() {
        if (originalWebView == null) return;

        try {
            originalWebView.addJavascriptInterface(new AppBridge(), "AppBridge");
        } catch (Exception e) {
            e.printStackTrace();
        }

        WebSettings settings = originalWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(originalWebView, true);
        CookieManager.getInstance().flush();

        settings.setUserAgentString(UA_REAL);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }

    public class AppBridge {
        @JavascriptInterface
        public void returnToHome() {
            runOnUiThread(() -> {
                try {
                    showOriginalWebView();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        @JavascriptInterface
        public void extractContent() {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "内容提取功能开发中", Toast.LENGTH_SHORT).show();
            });
        }

        @JavascriptInterface
        public void showToast(String message) {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void showOriginalWebView() {
        if (originalWebView == null || !isShowingExternal) return;

        isShowingExternal = false;

        if (contentContainer != null) {
            contentContainer.removeAllViews();
            
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            params.topMargin = dpToPx(72);
            contentContainer.addView(originalWebView, params);
        }

        activeWebView = null;
        activeSiteId = null;

        Toast.makeText(this, "返回首页", Toast.LENGTH_SHORT).show();
    }

    private void configureExternalWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        CookieManager.getInstance().flush();

        settings.setUserAgentString(UA_REAL);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private static class SiteInfo {
        String id;
        String name;
        String url;
        String icon;
        String color;

        SiteInfo(String id, String name, String url, String icon, String color) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.icon = icon;
            this.color = color;
        }
    }
}
