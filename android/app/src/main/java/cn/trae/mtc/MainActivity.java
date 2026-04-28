package cn.trae.mtc;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    // 标准 Safari on iPhone UA
    private static final String UA_SAFARI = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Mobile/15E148 Safari/604.1";
    
    // 标准 Chrome on Android UA
    private static final String UA_CHROME = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";

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
                // 设置 WindowInsets 监听器（保持原有的功能）
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

                // 配置 WebView 设置
                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                
                // 获取现有的 WebViewClient
                final WebViewClient originalClient = webView.getWebViewClient();
                
                // 设置一个包装的 WebViewClient，保持原有的功能
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        updateUserAgent(view, url);
                        // 委托给原有的 Client 处理
                        if (originalClient != null) {
                            return originalClient.shouldOverrideUrlLoading(view, url);
                        }
                        // 如果没有原有的 Client，让 WebView 自己处理
                        return false;
                    }

                    @Override
                    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                        updateUserAgent(view, url);
                        // 委托给原有的 Client 处理
                        if (originalClient != null) {
                            originalClient.onPageStarted(view, url, favicon);
                        } else {
                            super.onPageStarted(view, url, favicon);
                        }
                    }

                    // 确保所有其他方法都委托给原有的 Client
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        if (originalClient != null) {
                            originalClient.onPageFinished(view, url);
                        } else {
                            super.onPageFinished(view, url);
                        }
                    }

                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        if (originalClient != null) {
                            originalClient.onReceivedError(view, errorCode, description, failingUrl);
                        } else {
                            super.onReceivedError(view, errorCode, description, failingUrl);
                        }
                    }

                    @Override
                    public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                        if (originalClient != null) {
                            originalClient.onReceivedSslError(view, handler, error);
                        } else {
                            super.onReceivedSslError(view, handler, error);
                        }
                    }
                });
            }
        }
    }

    /**
     * 根据访问的 URL 动态更新 User-Agent
     * 访问 Google 相关网站 → 伪装成 Safari
     * 其他网站 → 伪装成 Chrome
     */
    private void updateUserAgent(WebView webView, String url) {
        if (webView == null || url == null) {
            return;
        }

        WebSettings webSettings = webView.getSettings();
        String newUA;

        // 检查是否是 Google 相关网站
        if (url.contains("google.com") || 
            url.contains("googleapis.com") || 
            url.contains("googleusercontent.com") ||
            url.contains("gstatic.com") ||
            url.contains("notebooklm.google.com")) {
            // Google 相关网站 → 伪装成 iPhone Safari
            newUA = UA_SAFARI;
        } else {
            // 其他网站 → 伪装成 Android Chrome
            newUA = UA_CHROME;
        }

        // 仅在 UA 不同时更新，避免不必要的刷新
        String currentUA = webSettings.getUserAgentString();
        if (!newUA.equals(currentUA)) {
            webSettings.setUserAgentString(newUA);
        }
    }
}
