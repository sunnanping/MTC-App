package cn.trae.mtc;

import android.graphics.Color;
import android.os.Bundle;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.getcapacitor.BridgeActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BridgeActivity {

    private static final String UA_REAL = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    
    private static final String FINGERPRINT_JS = "(function(){Object.defineProperty(screen,'width',{value:390});Object.defineProperty(screen,'height',{value:844});const canvas=document.createElement('canvas');const ctx=canvas.getContext('2d');ctx.fillText('abcdefghijklmnopqrstuvwxyz',2,2);const gl=canvas.getContext('webgl');if(gl){const e=gl.getExtension('WEBGL_debug_renderer_info');e&&gl.getParameter(e.UNMASKED_RENDERER_WEBGL)}})();";

    private static final String EXTRACT_CONTENT_JS = "(function(){" +
        "function extractText(el){" +
        "  if(!el) return '';" +
        "  var texts = [];" +
        "  var walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null, false);" +
        "  var node;" +
        "  while(node = walker.nextNode()){" +
        "    var t = node.textContent.trim();" +
        "    if(t.length > 10) texts.push(t);" +
        "  }" +
        "  return texts.join('\\n');" +
        "}" +
        "var content = extractText(document.body);" +
        "window.ContentBridge.onContentExtracted(content);" +
        "})();";

    private LinearLayout navContainer;
    private FrameLayout webViewContainer;
    private com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton btnExtractContent;

    private List<SiteInfo> sites = new ArrayList<>();
    private Map<String, WebView> webViewMap = new HashMap<>();
    private WebView activeWebView;
    private String activeSiteId;

    private WebViewClient originalWebViewClient;
    private WebChromeClient originalWebChromeClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        initNativeUI();
        initSites();
        renderNavIcons();
        configureCapacitorWebView();
    }

    private void initNativeUI() {
        try {
            View rootView = findViewById(android.R.id.content);
            if (rootView != null) {
                navContainer = rootView.findViewWithTag("navContainer");
                webViewContainer = rootView.findViewWithTag("webViewContainer");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initSites() {
        sites.add(new SiteInfo("1", "TRAE SOLO", "https://solo.trae.cn", "T", "#5C61FF"));
        sites.add(new SiteInfo("2", "DeepSeek", "https://chat.deepseek.com/sign_in", "D", "#00D4AA"));
        sites.add(new SiteInfo("3", "豆包", "https://www.doubao.com/chat", "豆", "#1890FF"));
        sites.add(new SiteInfo("4", "Kimi", "https://www.kimi.com", "K", "#FF6B6B"));
        sites.add(new SiteInfo("5", "NotebookLM", "https://notebooklm.google.com", "N", "#4285F4"));
    }

    private void renderNavIcons() {
        if (bridge == null || bridge.getWebView() == null) return;
        
        WebView webView = bridge.getWebView();
        ViewGroup parent = (ViewGroup) webView.getParent();
        if (parent == null) return;

        if (navContainer == null) {
            navContainer = new LinearLayout(this);
            navContainer.setOrientation(LinearLayout.HORIZONTAL);
            navContainer.setPadding(24, 24, 24, 24);
            navContainer.setBackgroundColor(Color.parseColor("#1A1A1A"));
            navContainer.setTag("navContainer");
            
            LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            
            if (parent instanceof FrameLayout) {
                FrameLayout frameParent = (FrameLayout) parent;
                webViewContainer = new FrameLayout(this);
                webViewContainer.setTag("webViewContainer");
                webViewContainer.setBackgroundColor(Color.WHITE);
                
                FrameLayout.LayoutParams containerParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                );
                
                frameParent.removeAllViews();
                frameParent.addView(navContainer, navParams);
                frameParent.addView(webViewContainer, containerParams);
                
                FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                );
                webViewContainer.addView(webView, webParams);
            }
        }

        navContainer.removeAllViews();

        for (SiteInfo site : sites) {
            View iconView = createSiteIconView(site);
            navContainer.addView(iconView);
        }

        View addBtn = createAddButton();
        navContainer.addView(addBtn);

        if (btnExtractContent == null) {
            btnExtractContent = new com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton(this);
            btnExtractContent.setText("提取内容");
            btnExtractContent.setBackgroundColor(Color.parseColor("#5C61FF"));
            btnExtractContent.setTextColor(Color.WHITE);
            btnExtractContent.setVisibility(View.GONE);
            btnExtractContent.setOnClickListener(v -> extractCurrentContent());
            
            FrameLayout.LayoutParams fabParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
            fabParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
            fabParams.bottomMargin = 100;
            
            if (webViewContainer != null) {
                webViewContainer.addView(btnExtractContent, fabParams);
            }
        }
    }

    private View createSiteIconView(SiteInfo site) {
        CardView cardView = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(96, 96);
        params.setMargins(0, 0, 24, 0);
        cardView.setLayoutParams(params);
        cardView.setRadius(48);
        cardView.setCardBackgroundColor(Color.parseColor(site.color));

        TextView textView = new TextView(this);
        textView.setText(site.icon);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(18);
        textView.setGravity(android.view.Gravity.CENTER);
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(96, 96);
        params.setMargins(0, 0, 24, 0);
        cardView.setLayoutParams(params);
        cardView.setRadius(48);
        cardView.setCardBackgroundColor(Color.parseColor("#333333"));

        TextView textView = new TextView(this);
        textView.setText("+");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(24);
        textView.setGravity(android.view.Gravity.CENTER);
        
        cardView.addView(textView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));

        cardView.setOnClickListener(v -> showAddSiteDialog());

        return cardView;
    }

    private void selectSite(SiteInfo site) {
        activeSiteId = site.id;
        
        if (webViewContainer != null) {
            webViewContainer.removeAllViews();
        }
        
        WebView webView = getOrCreateWebView(site);
        activeWebView = webView;
        
        if (webViewContainer != null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            webViewContainer.addView(webView, params);
        }
        
        btnExtractContent.setVisibility(View.VISIBLE);
        
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
        if (bridge == null) return;
        
        WebView webView = bridge.getWebView();
        if (webView == null) return;

        originalWebViewClient = webView.getWebViewClient();
        originalWebChromeClient = webView.getWebChromeClient();

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

        webView.addJavascriptInterface(new ContentBridge(), "ContentBridge");

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

        webView.addJavascriptInterface(new ContentBridge(), "ContentBridge");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(FINGERPRINT_JS, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
    }

    private void extractCurrentContent() {
        if (activeWebView == null) {
            Toast.makeText(this, "请先选择一个网站", Toast.LENGTH_SHORT).show();
            return;
        }

        activeWebView.evaluateJavascript(EXTRACT_CONTENT_JS, null);
        Toast.makeText(this, "正在提取内容...", Toast.LENGTH_SHORT).show();
    }

    public class ContentBridge {
        @JavascriptInterface
        public void onContentExtracted(String content) {
            runOnUiThread(() -> {
                if (content != null && !content.isEmpty()) {
                    showExtractedContent(content);
                } else {
                    Toast.makeText(MainActivity.this, "未提取到内容", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showExtractedContent(String content) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("提取的内容 (" + activeSiteId + ")");
        
        final TextView textView = new TextView(this);
        textView.setText(content.length() > 2000 ? content.substring(0, 2000) + "..." : content);
        textView.setPadding(32, 32, 32, 32);
        textView.setTextSize(14);
        
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        scrollView.addView(textView);
        
        builder.setView(scrollView);
        builder.setPositiveButton("关闭", null);
        builder.setNeutralButton("复制", (dialog, which) -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("content", content);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
        });
        builder.show();
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
        layout.setPadding(50, 40, 50, 10);

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
        urlHint.setPadding(0, 20, 0, 0);
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
        layout.setPadding(50, 40, 50, 10);

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
