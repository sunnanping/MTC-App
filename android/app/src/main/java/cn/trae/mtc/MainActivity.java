package cn.trae.mtc;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    // 真实的 Android Chrome UA
    private static final String UA_REAL = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    
    // 保存原始的 WebViewClient 和 WebChromeClient
    private WebViewClient originalWebViewClient;
    private WebChromeClient originalWebChromeClient;
    
    // 标记是否加载外部网站
    private boolean isExternalWebsite = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (bridge != null) {
            WebView webView = bridge.getWebView();
            if (webView != null) {
                // 保存原始的 WebViewClient 和 WebChromeClient
                originalWebViewClient = webView.getWebViewClient();
                originalWebChromeClient = webView.getWebChromeClient();
                
                // 设置 WindowInsets 监听器
                ViewCompat.setOnApplyWindowInsetsListener(webView, (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    if (mlp != null) {
                        mlp.topMargin = insets.top;
                        mlp.bottomMargin = insets.bottom;
                        v.setLayoutParams(mlp);
                    }
                    return windowInsets;
                });

                // 添加 JavaScript Interface
                webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

                configureWebView(webView);
            }
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void loadUrl(String url) {
            runOnUiThread(() -> {
                if (bridge != null && bridge.getWebView() != null) {
                    WebView webView = bridge.getWebView();
                    isExternalWebsite = true;
                    
                    // 切换到外部网站模式
                    webView.setWebViewClient(new WebViewClient() {
                        @Override
                        public boolean shouldOverrideUrlLoading(WebView view, String url) {
                            return false;
                        }
                    });
                    
                    webView.setWebChromeClient(new WebChromeClient());
                    
                    // 加载外部 URL
                    webView.loadUrl(url);
                }
            });
        }

        @JavascriptInterface
        public boolean canGoBack() {
            if (bridge != null && bridge.getWebView() != null) {
                return bridge.getWebView().canGoBack();
            }
            return false;
        }

        @JavascriptInterface
        public void goBack() {
            runOnUiThread(() -> {
                if (bridge != null && bridge.getWebView() != null) {
                    bridge.getWebView().goBack();
                }
            });
        }

        @JavascriptInterface
        public void returnToHome() {
            runOnUiThread(() -> {
                if (bridge != null && bridge.getWebView() != null) {
                    WebView webView = bridge.getWebView();
                    isExternalWebsite = false;
                    
                    // 恢复原始的 WebViewClient 和 WebChromeClient
                    if (originalWebViewClient != null) {
                        webView.setWebViewClient(originalWebViewClient);
                    }
                    if (originalWebChromeClient != null) {
                        webView.setWebChromeClient(originalWebChromeClient);
                    }
                    
                    // 重新加载首页
                    webView.loadUrl("file:///android_asset/public/index.html");
                }
            });
        }
    }

    private void configureWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        
        // 1. 开启核心能力
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        // 2. 允许第三方 Cookie（必须）
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        CookieManager.getInstance().flush();

        // 3. 伪装成 Chrome（必须）
        settings.setUserAgentString(UA_REAL);

        // 4. 支持混合内容
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    }
}
