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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sites.add(new SiteInfo("1", "TRAE SOLO", "https://solo.trae.cn", "T", "#5C61FF"));
        sites.add(new SiteInfo("2", "DeepSeek", "https://chat.deepseek.com/sign_in", "D", "#00D4AA"));
        sites.add(new SiteInfo("3", "豆包", "https://www.doubao.com/chat", "豆", "#1890FF"));
        sites.add(new SiteInfo("4", "Kimi", "https://www.kimi.com", "K", "#FF6B6B"));
        sites.add(new SiteInfo("5", "NotebookLM", "https://notebooklm.google.com", "N", "#4285F4"));
    }

    @Override
    protected void onStart() {
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

        navContainer = new LinearLayout(this);
        navContainer.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        navContainer.setOrientation(LinearLayout.HORIZONTAL);
        navContainer.setGravity(Gravity.CENTER_VERTICAL);
        navContainer.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));

        navBarContainer.addView(navContainer);
        rootContainer.addView(navBarContainer);

        renderNavIcons();

        parent.addView(rootContainer, index);
    }

    private void renderNavIcons() {
        if (navContainer == null) return;
        navContainer.removeAllViews();

        for (SiteInfo site : sites) {
            View iconView = createSiteIconView(site);
            navContainer.addView(iconView);
        }
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

        cardView.addView(textView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        cardView.setOnClickListener(v -> selectSite(site));

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

        settings.setUserAgentString(UA_REAL);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
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

        settings.setUserAgentString(UA_REAL);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
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
