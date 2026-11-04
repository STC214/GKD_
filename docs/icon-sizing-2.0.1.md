# 2.0.1 图标比例修正（本地验证版，未发布）

APK版本2.0.1／101，模块版本2.0.1／2026110403；当前公开Release仍为2.0.0，不替换旧附件或移动旧标签。本次仅调整资源留白和版本元数据，不修改规则、Root／Native、HMA或授权。

## 问题与修正

原自适应前景和主题单色层四边各内缩29%，图案只占前景画布宽高42%。改为16.67%，把方形原图映射到108画布的中心72视口；由系统形状遮罩裁切四角，而非把整张方图再缩到圆形安全区。图案相对旧版本线性放大约1.59倍；原始PNG不重采样、不改像素。

启动页不是自适应桌面视口：无独立图标背景时按288画布、192直径圆处理。启动页仅从28%改为27%，46%边长方形的对角线仍小于192/288，避免直接套用桌面16.67%导致方图裁切。通知／快捷设置使用原始单色向量，不随桌面前景增加或减少留白。

依据：[Android自适应图标](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)、[AdaptiveIconDrawable视口](https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable)、[SplashScreen尺寸](https://developer.android.com/reference/androidx/core/splashscreen/SplashScreen)。不同桌面主题包仍可能覆盖应用图标；不清除用户桌面数据。

## 验证与产物安排

新增6项资源比例／引用回归；需执行完整JVM与Lint、APK／模块字节一致校验、真机日夜间资源渲染、前后比例对照及覆盖部署。结果以本机`local-assets/icon-20261104-v101/verification.json`为准；本文不预先声称检查通过。

成品单独归档`dist/v2.0.1-iconfix/`，保留2.0.0基线与可执行回退。真机探针、渲染目录、安装缓存用后清理，不遗留测试应用。没有新的Native字节，不为图标调整重启手机；没有用户要求前不发布新Release。
