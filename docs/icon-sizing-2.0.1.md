# 2.0.1 图标比例修正（本地验证版，未发布）

APK版本2.0.1／101，模块版本2.0.1／2026110403；当前公开Release仍为2.0.0，不替换旧附件或移动旧标签。本次仅调整资源留白和版本元数据，不修改规则、Root／Native、HMA或授权。

## 问题与修正

原自适应前景和主题单色层四边各内缩29%，图案只占前景画布宽高42%。改为16.67%，把方形原图映射到108画布的中心72视口；由系统形状遮罩裁切四角，而非把整张方图再缩到圆形安全区。图案相对旧版本线性放大约1.59倍；原始PNG不重采样、不改像素。

启动页不是自适应桌面视口：无独立图标背景时按288画布、192直径圆处理。启动页仅从28%改为27%，46%边长方形的对角线仍小于192/288，避免直接套用桌面16.67%导致方图裁切。通知／快捷设置使用原始单色向量，不随桌面前景增加或减少留白。

依据：[Android自适应图标](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)、[AdaptiveIconDrawable视口](https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable)、[SplashScreen尺寸](https://developer.android.com/reference/androidx/core/splashscreen/SplashScreen)。不同桌面主题包仍可能覆盖应用图标；不清除用户桌面数据。

## 验证与产物安排

新增6项资源比例／引用回归，完整261项JVM通过，Lint0错误／58警告；APK／模块字节一致、签名及编译资源内缩值校验通过，原PNG与包内像素未变。真机100→101→100→101覆盖更新和回退通过，最终为`2.0.1-ee343da / 101`；配置、规则、白名单、HMA及UID保持不变。实际桌面前后截图已目视确认：图案铺满原图标范围，不再套着大圈蓝色留白。

真机日夜间单色／启动页逐资源渲染未通过：Root及Shell探针被系统终止（exit137），临时验证应用安装被系统取消，未绕过限制。单色与启动页仅完成编译资源引用／比例及安全区几何检查，不冒充实际渲染通过。本轮7项远端临时文件／目录已清理；手机未安装iconprobe或globalfixture测试应用。完整字面输出与边界见本机`local-assets/icon-20261104-v101/verification.json`及引用记录。

成品已单独归档`dist/v2.0.1-iconfix/`，保留2.0.0基线与实际执行过的可运行回退。没有新的Native字节，本轮未重启手机；未推送或发布新Release。APK的干净构建源码为`ee343da`，后续仅文档提交不重新打包、不改变其源码标识。
