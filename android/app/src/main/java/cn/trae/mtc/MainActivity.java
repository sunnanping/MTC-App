package cn.trae.mtc;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.webkit.JavascriptInterface;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.cardview.widget.CardView;

import com.getcapacitor.BridgeActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BridgeActivity {

    private static final String UA_REAL = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    
    private static final int MAX_WEBVIEW_COUNT = 5;
    private static final long CACHE_CLEANUP_INTERVAL = 5 * 60 * 1000;

    private WebView originalWebView;
    private FrameLayout rootContainer;
    private FrameLayout navBarContainer;
    private LinearLayout navContainer;
    private FrameLayout contentContainer;
    private FrameLayout loadingContainer;
    private ProgressBar loadingProgress;
    private TextView loadingText;
    
    private Map<String, WebView> webViewMap = new LinkedHashMap<>();
    private Map<String, WebViewState> webViewStateMap = new HashMap<>();
    private WebView activeWebView;
    private String activeSiteId;
    private boolean isShowingExternal = false;
    private boolean isLoading = false;

    private List<SiteInfo> sites = new ArrayList<>();
    private Handler cleanupHandler;
    private Runnable cleanupRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sites.add(new SiteInfo("1", getString(R.string.site_trae), "https://solo.trae.cn", "T", "#5C61FF"));
        sites.add(new SiteInfo("2", getString(R.string.site_deepseek), "https://chat.deepseek.com/sign_in", "D", "#00D4AA"));
        sites.add(new SiteInfo("3", getString(R.string.site_doubao), "https://www.doubao.com/chat", "豆", "#1890FF"));
        sites.add(new SiteInfo("4", getString(R.string.site_kimi), "https://www.kimi.com", "K", "#FF6B6B"));
        sites.add(new SiteInfo("5", getString(R.string.site_notebooklm), "https://notebooklm.google.com", "N", "#4285F4"));

        setupCacheCleanup();
    }

    private void setupCacheCleanup() {
        cleanupHandler = new Handler(Looper.getMainLooper());
        cleanupRunnable = new Runnable() {
            @Override
            public void run() {
                cleanupUnusedWebViews();
                cleanupHandler.postDelayed(this, CACHE_CLEANUP_INTERVAL);
            }
        };
    }

    private void cleanupUnusedWebViews() {
        synchronized (webViewMap) {
            while (webViewMap.size() > MAX_WEBVIEW_COUNT) {
                String oldestKey = webViewMap.keySet().iterator().next();
                if (!oldestKey.equals(activeSiteId)) {
                    WebView webView = webViewMap.remove(oldestKey);
                    if (webView != null) {
                        webView.stopLoading();
                        webView.destroy();
                    }
                    webViewStateMap.remove(oldestKey);
                } else {
                    break;
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            setupNativeNavBar();
            configureCapacitorWebView();
            cleanupHandler.postDelayed(cleanupRunnable, CACHE_CLEANUP_INTERVAL);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cleanupHandler != null && cleanupRunnable != null) {
            cleanupHandler.removeCallbacks(cleanupRunnable);
        }
        pauseInactiveWebViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyAllWebViews();
    }

    private void pauseInactiveWebViews() {
        for (Map.Entry<String, WebView> entry : webViewMap.entrySet()) {
            if (!entry.getKey().equals(activeSiteId)) {
                entry.getValue().onPause();
            }
        }
    }

    private void destroyAllWebViews() {
        for (WebView webView : webViewMap.values()) {
            webView.stopLoading();
            webView.destroy();
        }
        webViewMap.clear();
        webViewStateMap.clear();
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

        loadingContainer = new FrameLayout(this);
        loadingContainer.setLayoutParams(new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        loadingContainer.setBackgroundColor(Color.parseColor("#1A1A1A"));
        loadingContainer.setVisibility(View.GONE);

        loadingProgress = new ProgressBar(this);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
            dpToPx(48),
            dpToPx(48)
        );
        progressParams.gravity = Gravity.CENTER;
        loadingProgress.setLayoutParams(progressParams);
        loadingContainer.addView(loadingProgress);

        loadingText = new TextView(this);
        loadingText.setText(getString(R.string.loading));
        loadingText.setTextColor(Color.WHITE);
        loadingText.setTextSize(14);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER;
        textParams.topMargin = dpToPx(60);
        loadingText.setLayoutParams(textParams);
        loadingContainer.addView(loadingText);

        rootContainer.addView(loadingContainer);

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
        cardView.setClickable(true);

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
        if (isLoading) return;

        activeSiteId = site.id;
        isShowingExternal = true;

        showLoading();

        runOnUiThread(() -> {
            try {
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
                    
                    webView.setAlpha(0f);
                    contentContainer.addView(webView, params);

                    animateViewIn(webView);
                }

                restoreWebViewState(site.id);
            } catch (Exception e) {
                e.printStackTrace();
                hideLoading();
                Toast.makeText(MainActivity.this, getString(R.string.load_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void animateViewIn(View view) {
        AlphaAnimation anim = new AlphaAnimation(0f, 1f);
        anim.setDuration(300);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                hideLoading();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(anim);
    }

    private void showLoading() {
        if (loadingContainer != null) {
            isLoading = true;
            loadingContainer.setVisibility(View.VISIBLE);
            loadingContainer.setAlpha(1f);
        }
    }

    private void hideLoading() {
        if (loadingContainer != null) {
            AlphaAnimation anim = new AlphaAnimation(1f, 0f);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    loadingContainer.setVisibility(View.GONE);
                    isLoading = false;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
            loadingContainer.startAnimation(anim);
        }
    }

    private WebView getOrCreateWebView(SiteInfo site) {
        synchronized (webViewMap) {
            if (webViewMap.containsKey(site.id)) {
                WebView webView = webViewMap.remove(site.id);
                webViewMap.put(site.id, webView);
                webView.onResume();
                return webView;
            }

            cleanupUnusedWebViews();

            WebView webView = new WebView(this);
            configureExternalWebView(webView, site.id);
            webView.loadUrl(site.url);
            webViewMap.put(site.id, webView);

            return webView;
        }
    }

    private void saveWebViewState(String siteId) {
        WebView webView = webViewMap.get(siteId);
        if (webView != null) {
            WebViewState state = new WebViewState();
            state.url = webView.getUrl();
            state.scrollX = webView.getScrollX();
            state.scrollY = webView.getScrollY();
            webViewStateMap.put(siteId, state);
        }
    }

    private void restoreWebViewState(String siteId) {
        WebViewState state = webViewStateMap.get(siteId);
        if (state != null && activeWebView != null) {
            activeWebView.loadUrl(state.url);
        }
    }

    private void showSiteOptions(SiteInfo site) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(site.name);
        builder.setItems(new CharSequence[]{getString(R.string.edit), getString(R.string.delete)}, (dialog, which) -> {
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
        builder.setTitle(getString(R.string.add_site));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(20), dpToPx(24), dpToPx(10));

        final TextView nameHint = new TextView(this);
        nameHint.setText(getString(R.string.site_name));
        nameHint.setTextSize(12);
        nameHint.setTextColor(Color.GRAY);
        layout.addView(nameHint);

        final android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setHint(getString(R.string.site_name_hint));
        layout.addView(nameInput);

        final TextView urlHint = new TextView(this);
        urlHint.setText(getString(R.string.site_url));
        urlHint.setTextSize(12);
        urlHint.setTextColor(Color.GRAY);
        urlHint.setPadding(0, dpToPx(16), 0, 0);
        layout.addView(urlHint);

        final android.widget.EditText urlInput = new android.widget.EditText(this);
        urlInput.setHint(getString(R.string.site_url_hint));
        layout.addView(urlInput);

        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.add), (dialog, which) -> {
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
                Toast.makeText(this, getString(R.string.added) + ": " + name, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void showEditSiteDialog(SiteInfo site) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.edit_site));

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

        builder.setPositiveButton(getString(R.string.save), (dialog, which) -> {
            String oldUrl = site.url;
            site.name = nameInput.getText().toString().trim();
            site.url = urlInput.getText().toString().trim();
            if (!site.name.isEmpty()) {
                site.icon = site.name.substring(0, 1).toUpperCase();
            }
            
            if (!oldUrl.equals(site.url)) {
                WebView webView = webViewMap.remove(site.id);
                if (webView != null) {
                    webView.stopLoading();
                    webView.destroy();
                }
                webViewStateMap.remove(site.id);
            }
            
            renderNavIcons();
            Toast.makeText(this, getString(R.string.saved), Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.show();
    }

    private void showDeleteConfirm(SiteInfo site) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.confirm_delete));
        builder.setMessage(getString(R.string.delete_message) + " \"" + site.name + "\"?");
        builder.setPositiveButton(getString(R.string.delete), (dialog, which) -> {
            sites.remove(site);
            
            WebView webView = webViewMap.remove(site.id);
            if (webView != null) {
                webView.stopLoading();
                webView.destroy();
            }
            webViewStateMap.remove(site.id);
            
            if (site.id.equals(activeSiteId)) {
                showOriginalWebView();
            }
            
            renderNavIcons();
            Toast.makeText(this, getString(R.string.deleted) + ": " + site.name, Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton(getString(R.string.cancel), null);
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

    private void configureExternalWebView(WebView webView, String siteId) {
        WebSettings settings = webView.getSettings();
        
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        CookieManager.getInstance().flush();

        settings.setUserAgentString(UA_REAL);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.addJavascriptInterface(new AppBridge(), "AppBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideLoading();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                hideLoading();
                Toast.makeText(MainActivity.this, getString(R.string.page_error) + ": " + description, Toast.LENGTH_SHORT).show();
            }
        });

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
        public void extractContent() {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, getString(R.string.extract_content), Toast.LENGTH_SHORT).show();
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

        saveWebViewState(activeSiteId);
        
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

        Toast.makeText(this, getString(R.string.return_home), Toast.LENGTH_SHORT).show();
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

    private static class WebViewState {
        String url;
        int scrollX;
        int scrollY;
    }
}