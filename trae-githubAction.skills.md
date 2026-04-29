# GitHub Actions 构建错误排查指南

本文档记录了 MTC-App 项目在 GitHub Actions 构建过程中遇到的所有错误及其解决方案，旨在帮助后续项目避免类似的开发陷阱。

---

## 一、Android 构建相关错误

### 1. AndroidX 依赖版本与 AGP 不兼容

**错误信息：**
```
Execution failed for task ':app:checkDebugAarMetadata'.
> 3 issues were found when checking AAR metadata:
    1. Dependency 'androidx.activity:activity:1.11.0' requires Android Gradle plugin 8.9.1 or higher.
       This build currently uses Android Gradle plugin 8.7.2.
    2. Dependency 'androidx.core:core-ktx:1.17.0' requires Android Gradle plugin 8.9.1 or higher.
    3. Dependency 'androidx.core:core:1.17.0' requires Android Gradle plugin 8.9.1 or higher.
```

**原因分析：**
- AndroidX 依赖库版本过高，要求更高版本的 Android Gradle Plugin (AGP)
- AGP 版本与 AndroidX 依赖版本存在兼容性要求

**解决方案：**
1. **方案一：升级 AGP 版本**
   ```groovy
   // android/build.gradle
   dependencies {
       classpath 'com.android.tools.build:gradle:8.9.1'
   }
   ```
   同时需要升级 Gradle wrapper 版本以匹配 AGP 要求：
   ```properties
   # android/gradle/wrapper/gradle-wrapper.properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-all.zip
   ```

2. **方案二：降级 AndroidX 依赖版本**
   ```groovy
   // android/variables.gradle
   ext {
       androidxActivityVersion = '1.10.0'  // 从 1.11.0 降级
       androidxCoreVersion = '1.15.0'      // 从 1.17.0 降级
   }
   ```

**预防措施：**
- 在更新 AndroidX 依赖前，查阅 [AGP 与 AndroidX 兼容性表](https://d.android.com/r/tools/api-level-support)
- 使用 Capacitor 官方推荐的依赖版本

---

### 2. Debug Keystore 文件缺失

**错误信息：**
```
Execution failed for task ':app:validateSigningDebug'.
> Keystore file '/home/runner/work/MTC-App/MTC-App/android/app/debug.keystore' not found for signing config 'debug'.
```

**原因分析：**
- `.gitignore` 文件中配置了 `*.keystore`，导致 debug.keystore 被忽略
- 本地存在的 keystore 文件没有被推送到 GitHub 仓库
- GitHub Actions 构建时找不到签名所需的 keystore 文件

**解决方案：**
1. **修改 .gitignore，允许 debug.keystore 提交**
   ```gitignore
   # Secrets
   *.jks
   # Keep debug.keystore for CI builds
   # *.keystore
   google-services.json
   ```

2. **生成标准的 debug.keystore**
   ```bash
   keytool -genkey -v -keystore debug.keystore \
     -storepass android \
     -alias androiddebugkey \
     -keypass android \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000 \
     -dname "CN=Android Debug,O=Android,C=US"
   ```

3. **确保 keystore 文件被提交到 Git**
   ```bash
   git add android/app/debug.keystore
   git commit -m "Add debug.keystore for CI builds"
   git push
   ```

**预防措施：**
- 区分 debug keystore 和 release keystore 的处理方式
- Debug keystore 可以公开提交，Release keystore 应使用 GitHub Secrets 管理
- 在项目初始化时就创建并提交 debug.keystore

---

### 3. Gradle Wrapper 版本不兼容

**错误信息：**
```
WARNING: We recommend using a newer Android Gradle plugin to use compileSdk = 36
This Android Gradle plugin (8.7.2) was tested up to compileSdk = 35.
```

**原因分析：**
- Gradle wrapper 版本与 AGP 版本不匹配
- compileSdk 版本超出了 AGP 支持范围

**解决方案：**
确保 Gradle、AGP 和 compileSdk 版本匹配：
```properties
# gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-all.zip
```

```groovy
// build.gradle
classpath 'com.android.tools.build:gradle:8.9.1'
```

```groovy
// variables.gradle
compileSdkVersion = 36
```

**版本兼容参考：**
| AGP 版本 | 最低 Gradle 版本 | 最高支持 compileSdk |
|---------|-----------------|-------------------|
| 8.9.x   | 8.11.1          | 36                |
| 8.7.x   | 8.9             | 35                |
| 8.5.x   | 8.7             | 34                |

---

## 二、Capacitor 配置相关错误

### 4. Capacitor webDir 配置错误

**错误信息：**
```
[error] The web assets directory (./www) must contain an index.html file.
        It will be the entry point for the web portion of the Capacitor app.
```

**原因分析：**
- `capacitor.config.json` 中配置的 `webDir` 与实际目录不一致
- Web 资源文件放置在错误的目录中

**解决方案：**
1. **检查并修正 capacitor.config.json**
   ```json
   {
     "appId": "cn.trae.mtc",
     "appName": "MTC App",
     "webDir": "www",  // 确保与实际目录一致
     ...
   }
   ```

2. **确保 webDir 目录包含 index.html**
   ```bash
   # 如果使用 Vite 构建
   vite build --outDir www
   
   # 或手动复制
   cp -r dist/* www/
   ```

**预防措施：**
- 项目初始化时确定好 webDir 目录
- 在 CI/CD 流程中确保构建输出到正确的目录
- 使用 `npx cap sync` 前检查 webDir 目录是否存在且包含必要文件

---

## 三、Java 代码相关错误

### 5. 方法访问修饰符不匹配

**错误信息：**
```
error: onStart() in MainActivity cannot override onStart() in BridgeActivity
  attempting to assign weaker access privileges; was public
```

**原因分析：**
- 子类重写父类方法时，访问修饰符不能比父类更严格
- BridgeActivity 中的 `onStart()` 是 public，子类不能用 protected

**错误代码：**
```java
@Override
protected void onStart() {  // 错误：protected 比 public 更严格
    super.onStart();
}
```

**正确代码：**
```java
@Override
public void onStart() {  // 正确：保持 public
    super.onStart();
}
```

**预防措施：**
- 重写方法时使用 `@Override` 注解，编译器会检查访问修饰符
- 参考 Capacitor 官方示例代码
- 使用 IDE 的代码提示功能自动生成正确的方法签名

---

## 四、Git 相关问题

### 6. Git 忽略文件导致 CI 构建失败

**问题场景：**
- 本地构建成功，但 GitHub Actions 构建失败
- 某些必要文件没有被推送到仓库

**常见被忽略的关键文件：**
1. `*.keystore` - 导致签名失败
2. `gradle-wrapper.jar` - 导致 Gradle 无法运行
3. `google-services.json` - 导致 Firebase 功能失效

**解决方案：**
1. **检查 .gitignore 配置**
   ```bash
   git check-ignore -v android/app/debug.keystore
   ```

2. **强制添加被忽略的文件**
   ```bash
   git add -f android/app/debug.keystore
   ```

3. **修改 .gitignore 规则**
   ```gitignore
   # 排除特定文件
   *.keystore
   !debug.keystore  # 允许 debug.keystore
   ```

**预防措施：**
- 在推送前检查 CI 所需文件是否都被提交
- 使用 `git ls-files` 检查已跟踪文件
- 区分开发环境和 CI 环境的需求差异

---

## 五、最佳实践清单

### 项目初始化检查清单

- [ ] 确认 AGP、Gradle、AndroidX 版本兼容性
- [ ] 创建并提交 debug.keystore
- [ ] 配置正确的 capacitor.config.json webDir
- [ ] 检查 .gitignore 是否排除了 CI 必需文件
- [ ] 验证 Java 方法访问修饰符正确性

### CI/CD 配置建议

```yaml
# .github/workflows/build.yml 关键配置
- name: Setup Java
  uses: actions/setup-java@v4
  with:
    distribution: 'zulu'
    java-version: '21'
    cache: 'gradle'

- name: Setup Node.js
  uses: actions/setup-node@v4
  with:
    node-version: '22'
    cache: 'npm'

- name: Sync Capacitor
  run: npx cap sync android

- name: Build APK
  working-directory: ./android
  run: |
    chmod +x gradlew
    ./gradlew assembleDebug --no-daemon
```

### 调试技巧

1. **查看 GitHub Actions 详细日志**
   ```bash
   curl -H "Authorization: token $TOKEN" \
     "https://api.github.com/repos/OWNER/REPO/actions/runs/RUN_ID/logs" -o logs.zip
   ```

2. **本地模拟 CI 环境**
   ```bash
   # 使用相同的 Java 和 Node.js 版本
   # 清理缓存后重新构建
   rm -rf node_modules android/.gradle android/app/build
   npm ci
   npx cap sync android
   cd android && ./gradlew assembleDebug
   ```

---

## 六、Android 资源文件相关错误

### 7. AAPT 资源编译失败 - 特殊字符问题

**错误信息：**
```
Execution failed for task ':app:mergeDebugResources'.
> A failure occurred while executing com.android.build.gradle.internal.res.ResourceCompilerRunnable
  > Resource compilation failed (aapt2-8.9.1-internal:process-resources).
    java.lang.IllegalStateException: Can not extract resource from com.android.aaptcompiler.ParsedResource@...
```

**原因分析：**
- Android 资源文件 (strings.xml) 中包含 AAPT 无法正确处理的特殊字符
- 常见问题字符：`ç`, `ï`, `·`, `ñ`, `é`, `è`, `à`, `ü`, `ö`, `ß` 等
- 文件编码问题：虽然声明了 UTF-8，但某些特殊字符仍可能导致编译失败
- AAPT2 对某些 Unicode 字符的处理存在限制

**问题示例：**
```xml
<!-- 错误示例 - 包含特殊字符 -->
<string name="loading">Carregant...</string>  <!-- Catalan: 可能导致问题 -->
<string name="greeting">Hola, com estàs?</string>  <!-- 含有 à -->
```

**解决方案：**

1. **使用 ASCII 安全字符替代**
   ```xml
   <!-- 正确示例 - 使用安全字符 -->
   <string name="loading">Carregant...</string>  <!-- 移除特殊字符 -->
   <string name="greeting">Hola, com estas?</string>  <!-- 用 a 替代 à -->
   ```

2. **对于必须保留特殊字符的语言**
   - 使用 HTML 实体编码：`&#224;` 代表 `à`
   - 或使用 Unicode 转义：`\u00E0`

3. **验证资源文件编码**
   ```bash
   # 检查文件编码
   file -i android/app/src/main/res/values-*/strings.xml
   
   # 确保使用 UTF-8 无 BOM
   iconv -f UTF-8 -t UTF-8 input.xml > output.xml
   ```

**预防措施：**
- 添加新语言资源时，优先使用基础拉丁字符
- 在 CI 环境中本地测试资源编译
- 避免在 strings.xml 中使用复杂的 Unicode 字符

**安全字符集参考：**
| 字符类型 | 安全字符 | 可能导致问题的字符 |
|---------|---------|------------------|
| 基础拉丁字母 | a-z, A-Z | - |
| 数字 | 0-9 | - |
| 基础标点 | . , ! ? - _ : ; | 其他特殊符号 |
| 空格 | 空格 | 不换行空格等 |
| 变音符号 | - | à, é, ü, ñ, ç 等 |

---

### 8. Java 代码中的非 ASCII 字符导致构建失败

**错误信息：**
```
error: unmappable character (0xE6) for encoding UTF-8
error: unmappable character (0x9D) for encoding UTF-8
```

**原因分析：**
- Java 源文件中包含非 ASCII 字符（如中文、日文、特殊符号）
- 编译器默认使用系统编码，可能与文件编码不匹配
- 即使文件声明为 UTF-8，某些编译环境仍可能出问题

**错误代码示例：**
```java
// 错误：包含中文字符
String message = "加载中...";  // 可能导致编译问题
Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
```

**解决方案：**

1. **将字符串移至资源文件**
   ```java
   // 正确：使用资源引用
   String message = getString(R.string.loading);
   Toast.makeText(this, getString(R.string.deleted), Toast.LENGTH_SHORT).show();
   ```

2. **确保 Java 文件使用纯 ASCII**
   ```java
   // 如果必须在代码中定义字符串，使用英文或转义
   String message = "Loading...";  // 安全
   ```

3. **配置 Gradle 编码**
   ```groovy
   // android/app/build.gradle
   android {
       compileOptions {
           encoding "UTF-8"
       }
   }
   
   tasks.withType(JavaCompile) {
       options.encoding = 'UTF-8'
   }
   ```

**预防措施：**
- 所有用户可见字符串都应放在 strings.xml 中
- Java 代码中避免直接使用非 ASCII 字符
- 使用 lint 检查非 ASCII 字符

---

## 七、npm/Node.js 相关错误

### 9. npm ci 失败 - package-lock.json 不匹配

**错误信息：**
```
npm ERR! `npm ci` could not install due to a lockfile version mismatch.
npm ERR! The package-lock.json was created with a different version of npm.
```

**原因分析：**
- `npm ci` 严格要求 package-lock.json 与 node_modules 完全匹配
- 本地 npm 版本与 CI 环境 npm 版本不一致
- package-lock.json 过期或与 package.json 不同步

**解决方案：**

1. **方案一：使用 npm install 替代 npm ci**
   ```yaml
   # .github/workflows/build.yml
   - name: Install dependencies
     run: npm install  # 而非 npm ci
   ```

2. **方案二：更新 package-lock.json**
   ```bash
   # 本地重新生成 lock 文件
   rm -rf node_modules package-lock.json
   npm install
   git add package-lock.json
   git commit -m "Update package-lock.json"
   ```

3. **方案三：固定 npm 版本**
   ```yaml
   - name: Setup Node.js
     uses: actions/setup-node@v4
     with:
       node-version: '22'  # 固定版本
   ```

**npm ci vs npm install 对比：**
| 特性 | npm ci | npm install |
|-----|--------|-------------|
| 速度 | 更快 | 较慢 |
| 严格性 | 严格匹配 lock 文件 | 可更新 lock 文件 |
| node_modules | 删除后全新安装 | 增量更新 |
| 适用场景 | CI/CD 生产环境 | 开发环境 |

**预防措施：**
- 定期更新 package-lock.json
- 确保 CI 环境与本地 npm 版本一致
- 在 CI 中优先使用 npm ci，失败时降级为 npm install

---

## 八、多语言支持最佳实践

### 10. Android 多语言资源配置

**目录结构：**
```
android/app/src/main/res/
├── values/              # 默认语言（英语）
│   └── strings.xml
├── values-zh/           # 中文
│   └── strings.xml
├── values-es/           # 西班牙语
│   └── strings.xml
├── values-ja/           # 日语
│   └── strings.xml
└── ...                  # 其他语言
```

**语言代码参考（Top 20）：**
| 语言 | ISO 639-1 代码 | 目录名 |
|-----|---------------|--------|
| 英语 | en | values (默认) |
| 中文 | zh | values-zh |
| 西班牙语 | es | values-es |
| 印地语 | hi | values-hi |
| 阿拉伯语 | ar | values-ar |
| 葡萄牙语 | pt | values-pt |
| 孟加拉语 | bn | values-bn |
| 俄语 | ru | values-ru |
| 日语 | ja | values-ja |
| 法语 | fr | values-fr |
| 德语 | de | values-de |
| 乌尔都语 | ur | values-ur |
| 旁遮普语 | pa | values-pa |
| 爪哇语 | jv | values-jv |
| 泰卢固语 | te | values-te |
| 马拉地语 | mr | values-mr |
| 泰米尔语 | ta | values-ta |
| 越南语 | vi | values-vi |
| 韩语 | ko | values-ko |
| 土耳其语 | tr | values-tr |

**strings.xml 模板：**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">MTC App</string>
    <string name="title_activity_main">MTC App</string>
    
    <!-- Site Names -->
    <string name="site_trae">TRAE SOLO</string>
    <string name="site_deepseek">DeepSeek</string>
    
    <!-- Common Strings -->
    <string name="loading">Loading...</string>
    <string name="load_failed">Load failed</string>
    <string name="cancel">Cancel</string>
    <string name="save">Save</string>
    <string name="delete">Delete</string>
</resources>
```

**安全字符处理原则：**
1. 优先使用基础拉丁字母 (a-z, A-Z)
2. 避免使用变音符号和特殊字符
3. 如需特殊字符，使用 HTML 实体或 Unicode 转义
4. 每个语言文件添加后进行编译测试

---

## 九、WebView 配置相关

### 11. WebView User-Agent 配置

**问题场景：**
- 某些网站（如 DeepSeek）在 iframe 中无法正常加载
- 需要使用原生 WebView 替代 iframe
- 需要模拟真实浏览器 User-Agent

**解决方案：**
```java
// MainActivity.java
private void configureWebView(WebView webView) {
    WebSettings settings = webView.getSettings();
    
    // 设置 User-Agent 模拟真实浏览器
    String userAgent = "Mozilla/5.0 (Linux; Android 13) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Mobile Safari/537.36";
    settings.setUserAgentString(userAgent);
    
    // 启用必要功能
    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);
    settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    
    // 启用 Cookie
    CookieManager cookieManager = CookieManager.getInstance();
    cookieManager.setAcceptCookie(true);
    cookieManager.setAcceptThirdPartyCookies(webView, true);
}
```

**关键配置项：**
| 配置项 | 作用 | 推荐值 |
|-------|------|--------|
| User-Agent | 浏览器标识 | 模拟 Chrome Mobile |
| JavaScript | 脚本执行 | true |
| DomStorage | 本地存储 | true |
| MixedContent | 混合内容 | ALWAYS_ALLOW |
| Cookies | Cookie 支持 | true |

---

## 十、参考资源

- [Android Gradle Plugin 版本说明](https://developer.android.com/build/releases/gradle-plugin)
- [Capacitor 官方文档](https://capacitorjs.com/docs)
- [GitHub Actions 文档](https://docs.github.com/en/actions)
- [AndroidX 版本兼容性](https://developer.android.com/jetpack/androidx/releases)
- [Android 本地化指南](https://developer.android.com/guide/topics/resources/localization)
- [ISO 639-1 语言代码](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)

---

## 十一、快速排查清单

### 构建失败快速诊断

```
□ 检查 Gradle 版本是否与 AGP 匹配
□ 检查 Java 版本是否正确 (推荐 Java 21)
□ 检查 npm install 是否成功
□ 检查 capacitor.config.json webDir 配置
□ 检查 strings.xml 是否有特殊字符
□ 检查 Java 代码是否有非 ASCII 字符
□ 检查 debug.keystore 是否存在
□ 检查 .gitignore 是否排除了必要文件
```

### 常见错误速查表

| 错误类型 | 关键词 | 快速解决方案 |
|---------|--------|-------------|
| AGP 不兼容 | checkDebugAarMetadata | 升级 AGP 或降级 AndroidX |
| Keystore 缺失 | validateSigningDebug | 添加 debug.keystore |
| Gradle 版本 | minimum supported Gradle | 更新 gradle-wrapper.properties |
| webDir 错误 | index.html file | 检查 capacitor.config.json |
| 访问修饰符 | weaker access privileges | 修改为 public |
| 资源编译 | ParsedResource | 检查特殊字符 |
| npm ci | lockfile mismatch | 改用 npm install |
| 编码错误 | unmappable character | 移除非 ASCII 字符 |

---

*文档版本: 2.0*
*最后更新: 2026-04-29*
*项目: MTC-App*
