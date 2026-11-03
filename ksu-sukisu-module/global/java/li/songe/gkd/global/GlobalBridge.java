package li.songe.gkd.global;

import android.app.Activity;
import android.app.Application;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.io.DataInputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Injected process: observe/register nodes only. Matching and all actions stay in owner GKD. */
public final class GlobalBridge implements Application.ActivityLifecycleCallbacks {
    private static final String TAG="GKD-GlobalBridge";
    private static final AtomicBoolean started=new AtomicBoolean();
    private final Map<Object,Integer> owned=new WeakHashMap<>();
    private final Map<Object,Boolean> pending=new WeakHashMap<>();
    private volatile int resumed;
    private volatile long leaseUntil;
    private final AtomicBoolean workerStarted=new AtomicBoolean();
    private final Object wake=new Object();
    // Application callbacks run on main; compare by identity, never host-defined equals/hashCode.
    private final ArrayList<WeakReference<Activity>> activeActivities=new ArrayList<>();
    private boolean seenLifecycle,synthetic;
    private boolean ticking;
    private volatile boolean faulted;
    private final Handler main=new Handler(Looper.getMainLooper());
    private static native Application application();
    private static native Object[] roots();
    private static native Handler rootHandler(Object root);
    private static native int register(Object root);
    private static native boolean unregister(Object root,int expectedWindow);

    public static void start() {
        if(!started.compareAndSet(false,true))return;
        Thread init=new Thread(()->{
            long end=SystemClock.elapsedRealtime()+60_000;
            while(SystemClock.elapsedRealtime()<end) {
                try {
                    Application app=application();
                    if(app!=null && Looper.getMainLooper()!=null) {
                        new Handler(Looper.getMainLooper()).post(()->{
                            try {
                                GlobalBridge bridge=new GlobalBridge();
                                app.registerActivityLifecycleCallbacks(bridge);
                                Log.i(TAG,"ready package="+app.getPackageName()+" uid="+android.os.Process.myUid());
                                bridge.guarded(()->bridge.bootstrapRoots(10));
                            }catch(Throwable error){report("main init stopped",error);}
                        });return;
                    }
                    Thread.sleep(20);
                }catch(InterruptedException stop){Thread.currentThread().interrupt();return;}
                catch(Throwable error){report("init stopped",error);return;}
            }
        },"GkdGlobalInit");init.setDaemon(true);init.start();
    }

    private void startPolicyWorker() {
        if(!workerStarted.compareAndSet(false,true))return;
        Thread worker=new Thread(()->{
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    synchronized(wake){while(resumed==0 && !hasOwned())wake.wait();}
                    if(faulted && !hasOwned())return;
                    // A service/push-only process never performs socket polling or node scans.
                    if(resumed>0 && !faulted) pollPolicy();else leaseUntil=0;
                    Thread.sleep(1000);
                }catch(InterruptedException stop){Thread.currentThread().interrupt();}
                catch(Throwable error){leaseUntil=0;report("policy unavailable",error);}
            }
        },"GkdGlobalPolicy");worker.setDaemon(true);worker.start();
    }
    private void bootstrapRoots(int attempts) {
        if(seenLifecycle)return;
        Object[] windows=roots();
        if(windows!=null && windows.length>0){synthetic=true;resumed=1;startPolicyWorker();startTicker();return;}
        if(attempts>0)main.postDelayed(()->guarded(()->bootstrapRoots(attempts-1)),200);
    }
    private static void report(String message,Throwable error){try{Log.w(TAG,message,error);}catch(Throwable ignored){}}
    private void guarded(Runnable task){try{task.run();}catch(Throwable error){faulted=true;leaseUntil=0;report("host operation stopped",error);}}
    // Cleanup must remain live even if an IPC connect/read is delayed indefinitely.
    private void startTicker(){if(ticking)return;ticking=true;main.post(this::tick);}
    private void tick(){
        try {
            guarded(this::refresh);
            if((resumed>0 && !faulted)||hasOwned())main.postDelayed(this::tick,1000);
            else ticking=false;
        }catch(Throwable error){ticking=false;faulted=true;leaseUntil=0;report("cleanup ticker stopped",error);}
    }
    private boolean hasOwned(){synchronized(owned){return !owned.isEmpty();}}
    private boolean eligible(){return !faulted && resumed>0 && leaseUntil>SystemClock.elapsedRealtime();}
    private void pollPolicy() {
        long until=0;
        try(LocalSocket socket=new LocalSocket()) {
            socket.connect(new LocalSocketAddress("li.songe.gkd.node-policy.v1.owner0"));
            socket.setSoTimeout(250);
            if(socket.getPeerCredentials().getUid()!=0)throw new SecurityException("Policy peer is not root");
            DataInputStream input=new DataInputStream(socket.getInputStream());
            boolean allowed=input.readBoolean();long expires=input.readLong(),now=SystemClock.elapsedRealtime();
            if(allowed && expires>now)until=Math.min(expires,now+3000);
        }catch(Exception ignored) { }
        leaseUntil=until;
    }
    private void onRoot(Object root,Runnable task) {
        try {
            Handler handler=rootHandler(root);if(handler==null)return;
            Runnable safe=()->guarded(task);
            if(handler.getLooper()==Looper.myLooper())safe.run();else {
                synchronized(pending){if(pending.containsKey(root))return;pending.put(root,true);}
                boolean posted=false;
                try {
                    posted=handler.post(()->{try{safe.run();}finally{synchronized(pending){pending.remove(root);}}});
                }finally{if(!posted)synchronized(pending){pending.remove(root);}}
            }
        }catch(Throwable error){faulted=true;leaseUntil=0;report("root dispatch stopped",error);}
    }
    private void refresh() {
        if(eligible()) {
            Object[] windows=roots();if(windows==null)return;
            // One malformed/custom host window list cannot create an unbounded main task queue.
            for(int i=0;i<Math.min(windows.length,256);i++){Object root=windows[i];if(root!=null)onRoot(root,()->{
                if(!eligible())return;
                int id=register(root);
                if(id>=0){synchronized(owned){owned.put(root,id);}Log.i(TAG,"registered window="+id);}
            });}
        }else{
            Map<Object,Integer> snapshot;synchronized(owned){snapshot=new java.util.HashMap<>(owned);}
            for(Map.Entry<Object,Integer> entry:snapshot.entrySet())onRoot(entry.getKey(),()->{
                if(eligible())return;
                if(!unregister(entry.getKey(),entry.getValue()))return;
                synchronized(owned){
                    Integer current=owned.get(entry.getKey());
                    if(current!=null && current.intValue()==entry.getValue().intValue())owned.remove(entry.getKey());
                }
                Log.i(TAG,"released window="+entry.getValue());
            });
        }
    }
    private void updateActivity(Activity activity,boolean active){
        Iterator<WeakReference<Activity>> iterator=activeActivities.iterator();
        while(iterator.hasNext()){Activity value=iterator.next().get();if(value==null||value==activity)iterator.remove();}
        if(active)activeActivities.add(new WeakReference<>(activity));
        resumed=activeActivities.size();
    }
    public void onActivityResumed(Activity activity){guarded(()->{seenLifecycle=true;synthetic=false;updateActivity(activity,true);startPolicyWorker();synchronized(wake){wake.notifyAll();}startTicker();});}
    public void onActivityPaused(Activity activity){guarded(()->{seenLifecycle=true;synthetic=false;updateActivity(activity,false);startTicker();});}
    public void onActivityCreated(Activity activity,Bundle state){}
    public void onActivityStarted(Activity activity){}
    public void onActivityStopped(Activity activity){}
    public void onActivitySaveInstanceState(Activity activity,Bundle state){}
    public void onActivityDestroyed(Activity activity){guarded(()->{seenLifecycle=true;synthetic=false;updateActivity(activity,false);startTicker();});}
}
