package cn.trae.mtc;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.Toast;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final String UA_REAL = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

    private WebView originalWebView;
    private WebView externalWebView;
    private boolean isShowingExternal = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            configureCapacitorWebView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configureCapacitorWebView() {
        if (bridge == null) return;
        
        WebView webView = bridge.getWebView();
        if (webView == null) return;

        originalWebView = webView;

        ViewCompat.setOnApplyWindowInsetsListener(webView, (v, windowInsets) -> {
            try {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                android.view.ViewGroup.MarginLayoutParams mlp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
                if (mlp != null) {
                    mlp.topMargin = insets.top;
                    mlp.bottomMargin = insets.bottom;
                    v.setLayoutParams(mlp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return windowInsets;
        });

        try {
            webView.addJavascriptInterface(new AppBridge(), "AppBridge");
        } catch (Exception e) {
            e.printStackTrace();
        }

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
    }

    public class AppBridge {
        @JavascriptInterface
        public void loadExternalSite(String siteId, String url) {
            runOnUiThread(() -> {
                try {
                    showExternalSite(url);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }

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

    private void showExternalSite(String url) {
        if (originalWebView == null) return;

        android.view.ViewGroup parent = (android.view.ViewGroup) originalWebView.getParent();
        if (parent == null) return;

        if (externalWebView == null) {
            externalWebView = new WebView(this);
            configureExternalWebView(externalWebView);
        }

        isShowingExternal = true;

        int index = parent.indexOfChild(originalWebView);
        parent.removeView(originalWebView);

        android.view.ViewGroup.LayoutParams params = originalWebView.getLayoutParams();
        parent.addView(externalWebView, index, params);

        externalWebView.loadUrl(url);
        Toast.makeText(this, "正在加载: " + url, Toast.LENGTH_SHORT).show();
    }

    private void showOriginalWebView() {
        if (originalWebView == null || !isShowingExternal) return;

        android.view.ViewGroup parent = (android.view.ViewGroup) originalWebView.getParent();
        if (parent == null && externalWebView != null) {
            parent = (android.view.ViewGroup) externalWebView.getParent();
        }

        if (parent == null) return;

        isShowingExternal = false;

        if (externalWebView != null && parent.indexOfChild(externalWebView) != -1) {
            parent.removeView(externalWebView);
        }

        android.view.ViewGroup.LayoutParams params = originalWebView.getLayoutParams();
        parent.addView(originalWebView, params);

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
}
