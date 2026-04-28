package cn.trae.mtc;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebSettings;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    // 真实的 Android Chrome UA
    private static final String UA_REAL = "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36";
    
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

                // 重要：不修改 WebViewClient，保持 Capacitor 功能完整
                configureWebView(webView);
            }
        }
    }

    private void configureWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        
        // 一、设置真实浏览器 UA
        settings.setUserAgentString(UA_REAL);
        
        // 二、开启所有 Cookie 与存储
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        // 基本设置
        settings.setJavaScriptEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        // 三、指纹注入 - 通过 evaluateJavascript 在页面加载时注入
        // 注意：由于不能修改 WebViewClient，我们依赖 Capacitor 的机制或接受这个限制
        // 这里我们简单调用一次，但 Capacitor 应用可能需要更多的 JS 层处理
        webView.evaluateJavascript(FINGERPRINT_JS, null);
    }
}
