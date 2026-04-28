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
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    // 真实的 Android Chrome UA
    private static final String UA_REAL = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    
    // 指纹注入 JS 代码
    private static final String FINGERPRINT_JS = "(function(){Object.defineProperty(screen,'width',{value:390});Object.defineProperty(screen,'height',{value:844});const canvas=document.createElement('canvas');const ctx=canvas.getContext('2d');ctx.fillText('abcdefghijklmnopqrstuvwxyz',2,2);const gl=canvas.getContext('webgl');if(gl){const e=gl.getExtension('WEBGL_debug_renderer_info');e&&gl.getParameter(e.UNMASKED_RENDERER_WEBGL)}})();";

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
                    bridge.getWebView().loadUrl(url);
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
                    bridge.getWebView().loadUrl("file:///android_asset/public/index.html");
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

        // 5. 支持弹窗登录
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false; // 让 WebView 处理所有 URL
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 注入指纹 JS
                view.evaluateJavascript(FINGERPRINT_JS, null);
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient());
    }
}
