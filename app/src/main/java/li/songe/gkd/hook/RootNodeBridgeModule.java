package li.songe.gkd.hook;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Process;
import android.os.SystemClock;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.DataInputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import io.github.libxposed.api.XposedModule;

/** Experimental, app-scoped connection repair. Never changes isEnabled/settings/service lists. */
public final class RootNodeBridgeModule extends XposedModule {
    private static final String TAG = "GKD-NodeBridge";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<Object, Integer> owned = new WeakHashMap<>();
    private final Map<Object, Boolean> observed = new WeakHashMap<>();
    private SharedPreferences policy;
    private String appId;
    private boolean installed;
    private volatile long brokerLeaseUntil;
    private int targetUserId;
    private String lastBrokerFailure;
    private boolean watchStarted;
    private volatile boolean hasRoots;

    @Override public void onPackageReady(PackageReadyParam param) {
        if (installed || !param.isFirstPackage()) return;
        appId = param.getPackageName();
        if (appId == null || "android".equals(appId) || "li.songe.gkd".equals(appId) ||
            "com.android.systemui".equals(appId) || "com.android.settings".equals(appId) ||
            "com.android.shell".equals(appId)) return;
        installed = true;
        try {
            targetUserId = Process.myUid() / 100000;
            if (targetUserId != 0 && targetUserId != 999) return;
            if (targetUserId == 0) {
                policy = getRemotePreferences("root_node_bridge");
                policy.registerOnSharedPreferenceChangeListener((p, key) -> main.post(this::refreshConnections));
            }
            Class<?> rootClass = Class.forName("android.view.ViewRootImpl", false, param.getClassLoader());
            for (Method method : rootClass.getDeclaredMethods()) {
                if (!method.getName().equals("setView")) continue;
                hook(method).intercept(chain -> {
                    Object result = chain.proceed();
                    Object root = chain.getThisObject();
                    observed.put(root, true);
                    hasRoots = true;
                    startWatching();
                    if (eligible()) register(root);
                    return result;
                });
            }
            log(Log.INFO, TAG, "installed app=" + appId + " user=" + targetUserId + " eligible=" + eligible());
        } catch (Throwable error) { log(Log.ERROR, TAG, "setup failed", error); }
    }

    /** Push/background-only processes never start a policy polling worker. */
    private void startWatching() {
        if (watchStarted) return;
        watchStarted = true;
        if (targetUserId == 999) {
            Thread brokerPoller = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    if (hasRoots) pollOwnerPolicy(); else brokerLeaseUntil = 0;
                    main.post(this::refreshConnections);
                    try { Thread.sleep(1000); }
                    catch (InterruptedException stop) { Thread.currentThread().interrupt(); }
                }
            }, "GkdClonePolicy");
            brokerPoller.setDaemon(true);
            brokerPoller.start();
        }
        main.postDelayed(new Runnable() {
            public void run() { refreshConnections(); main.postDelayed(this, 1000); }
        }, 1000);
    }

    private boolean eligible() {
        if (targetUserId == 999) return brokerLeaseUntil > SystemClock.elapsedRealtime();
        try {
            Set<String> excluded = policy.getStringSet("excluded", Collections.emptySet());
            Set<String> allowed = policy.getStringSet("allowed", Collections.emptySet());
            return policy.getBoolean("enabled", false) &&
                policy.getLong("leaseUntil", 0) > System.currentTimeMillis() &&
                allowed.contains(appId) && !excluded.contains(appId);
        } catch (Throwable ignored) { return false; }
    }

    private void pollOwnerPolicy() {
        long until = 0;
        try (LocalSocket socket = new LocalSocket()) {
            socket.connect(new LocalSocketAddress("li.songe.gkd.node-policy.v1.owner0"));
            socket.setSoTimeout(250);
            if (socket.getPeerCredentials().getUid() != 0) throw new SecurityException("Policy peer is not root");
            DataInputStream input = new DataInputStream(socket.getInputStream());
            boolean allowed = input.readBoolean();
            long expires = input.readLong();
            long now = SystemClock.elapsedRealtime();
            if (allowed && expires > now) until = Math.min(expires, now + 3000);
            lastBrokerFailure = null;
        } catch (Exception error) {
            String failure = error.getClass().getSimpleName() + ": " + error.getMessage();
            if (!failure.equals(lastBrokerFailure)) log(Log.WARN, TAG, "clone-policy unavailable: " + failure);
            lastBrokerFailure = failure;
        }
        boolean changed = (brokerLeaseUntil > SystemClock.elapsedRealtime()) != (until > 0);
        brokerLeaseUntil = until;
        if (changed) log(Log.INFO, TAG, "clone-policy user=" + targetUserId + " eligible=" + (until > 0));
    }

    private static Object field(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }

    private static void call(Object object, String name) throws Exception {
        Method method = object.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(object);
    }

    private void register(Object root) {
        try {
            Object attach = field(root, "mAttachInfo");
            int before = (Integer) field(attach, "mAccessibilityWindowId");
            if (before != -1 || !eligible()) return;
            Object manager = field(root, "mAccessibilityInteractionConnectionManager");
            call(manager, "ensureConnection");
            int after = (Integer) field(attach, "mAccessibilityWindowId");
            if (after == -1) { log(Log.WARN, TAG, "registration returned no window"); return; }
            owned.put(root, after);
            try { call(root, "setAccessibilityWindowAttributesIfNeeded"); } catch (Exception ignored) { }
            Context context = (Context) field(root, "mContext");
            AccessibilityManager am = context.getSystemService(AccessibilityManager.class);
            log(Log.INFO, TAG, "registered app=" + appId + " before=" + before + " after=" + after +
                " visibleEnabled=" + am.isEnabled() +
                " visibleServices=" + am.getEnabledAccessibilityServiceList(-1).size() +
                " visibleSecure=" + Settings.Secure.getInt(context.getContentResolver(), "accessibility_enabled", -1));
        } catch (Throwable error) { log(Log.ERROR, TAG, "registration failed", error); }
    }

    private void refreshConnections() {
        hasRoots = !observed.isEmpty();
        if (eligible()) {
            for (Object root : observed.keySet()) register(root);
            return;
        }
        for (Map.Entry<Object, Integer> entry : owned.entrySet()) {
            try {
                Object root = entry.getKey();
                int current = (Integer) field(field(root, "mAttachInfo"), "mAccessibilityWindowId");
                if (current == entry.getValue()) call(field(root, "mAccessibilityInteractionConnectionManager"), "ensureNoConnection");
            } catch (Throwable error) { log(Log.WARN, TAG, "cleanup failed", error); }
        }
        owned.clear();
    }
}
