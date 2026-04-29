package cn.trae.mtc;

import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private ClipboardManager clipboardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupClipboardListener();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (bridge != null) {
            WebView webView = bridge.getWebView();
            if (webView != null) {
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
            }
        }
    }

    private void setupClipboardListener() {
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(new OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                notifyCopyToJS();
            }
        });
    }

    private void notifyCopyToJS() {
        runOnUiThread(() -> {
            if (bridge != null && bridge.getWebView() != null) {
                bridge.getWebView().evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('nativecopy'));", null
                );
            }
        });
    }
}
