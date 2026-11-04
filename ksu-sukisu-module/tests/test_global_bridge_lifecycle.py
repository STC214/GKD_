import pathlib,subprocess,json,os,argparse
parser=argparse.ArgumentParser();parser.add_argument('--java-home',default=os.environ.get('JAVA_HOME'));parser.add_argument('--repeat',type=int,default=30);options=parser.parse_args();assert options.java_home and 0<=options.repeat<=100
ROOT=pathlib.Path(__file__).resolve().parents[1];P=ROOT/'work/lifecycle-tests';P.mkdir(parents=True,exist_ok=True);JDK=pathlib.Path(options.java_home)/'bin'
stubs={
'android/app/Activity.java':'''package android.app; public class Activity {}''',
'android/app/Application.java':'''package android.app; import android.os.Bundle; public class Application { public interface ActivityLifecycleCallbacks {void onActivityResumed(Activity a);void onActivityPaused(Activity a);void onActivityCreated(Activity a,Bundle b);void onActivityStarted(Activity a);void onActivityStopped(Activity a);void onActivitySaveInstanceState(Activity a,Bundle b);void onActivityDestroyed(Activity a);} public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks x){} public String getPackageName(){return "fixture";} }''',
'android/net/LocalSocketAddress.java':'''package android.net;public class LocalSocketAddress {public LocalSocketAddress(String n){}}''',
'android/net/Credentials.java':'''package android.net;public class Credentials {public int getUid(){return 0;}}''',
'android/net/LocalSocket.java':'''package android.net;import java.io.*;public class LocalSocket implements AutoCloseable {public static volatile boolean block,entered;public void connect(LocalSocketAddress a)throws Exception{entered=true;while(block)Thread.sleep(10);}public void setSoTimeout(int n){}public Credentials getPeerCredentials(){return new Credentials();}public InputStream getInputStream(){return new ByteArrayInputStream(new byte[9]);}public void close(){} }''',
'android/os/Bundle.java':'''package android.os;public class Bundle {}''',
'android/os/Looper.java':'''package android.os;public class Looper {private static final Looper main=new Looper();public static Looper getMainLooper(){return main;}public static Looper myLooper(){return main;} }''',
'android/os/Handler.java':'''package android.os;import java.util.concurrent.ConcurrentLinkedQueue;public class Handler {public static final ConcurrentLinkedQueue<Runnable> queue=new ConcurrentLinkedQueue<>();private final Looper loop;public Handler(Looper l){loop=l;}public Looper getLooper(){return loop;}public boolean post(Runnable r){queue.add(r);return true;}public boolean postDelayed(Runnable r,long t){queue.add(r);return true;} }''',
'android/os/SystemClock.java':'''package android.os;public class SystemClock {public static long elapsedRealtime(){return 1000;} }''',
'android/os/Process.java':'''package android.os;public class Process {public static int myUid(){return 12345;} }''',
'android/util/Log.java':'''package android.util;public class Log {public static int i(String a,String b){return 0;}public static int w(String a,String b,Throwable t){return 0;} }''',
'GlobalBridgeAuditCheck.java':r'''import java.lang.reflect.*;import java.util.*;import android.app.Activity;import android.os.Handler;import android.net.LocalSocket;import li.songe.gkd.global.GlobalBridge;
public class GlobalBridgeAuditCheck {
 static Object get(Object o,String n)throws Exception{Field f=GlobalBridge.class.getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 static void set(Object o,String n,Object v)throws Exception{Field f=GlobalBridge.class.getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static void check(boolean value,String n){if(!value)throw new AssertionError(n);System.out.println("PASS "+n);}
 static class HostActivity extends Activity {public int hashCode(){throw new AssertionError("host hashCode invoked");}public boolean equals(Object o){throw new AssertionError("host equals invoked");}}
 public static void main(String[] args)throws Exception{
  GlobalBridge b=new GlobalBridge();((java.util.concurrent.atomic.AtomicBoolean)get(b,"workerStarted")).set(true);Activity a=new HostActivity(),c=new HostActivity();
  b.onActivityResumed(a);b.onActivityResumed(a);check((Integer)get(b,"resumed")==1 && !(Boolean)get(b,"faulted"),"duplicate resume identity tracking");
  b.onActivityResumed(c);b.onActivityPaused(a);check((Integer)get(b,"resumed")==1,"multi activity pause isolation");
  b.onActivityDestroyed(c);check((Integer)get(b,"resumed")==0,"destroyed activity count cleanup");
  LocalSocket.block=true;LocalSocket.entered=false;Handler.queue.clear();GlobalBridge q=new GlobalBridge();q.onActivityResumed(new HostActivity());
  long until=System.nanoTime()+1_000_000_000L;while(!LocalSocket.entered && System.nanoTime()<until)Thread.sleep(5);
  check(LocalSocket.entered && !Handler.queue.isEmpty(),"cleanup ticker independent of stalled IPC");LocalSocket.block=false;
  GlobalBridge f=new GlobalBridge();set(f,"resumed",1);set(f,"leaseUntil",100000L);Method tick=GlobalBridge.class.getDeclaredMethod("tick");tick.setAccessible(true);tick.invoke(f);
  check((Boolean)get(f,"faulted") && (Long)get(f,"leaseUntil")==0L,"native failure stays inside guarded ticker");
  GlobalBridge r=new GlobalBridge();Map<Object,Integer> owned=(Map<Object,Integer>)get(r,"owned");Object root=new Object();owned.put(root,42);tick.invoke(r);
  check(owned.get(root)==42,"failed native cleanup retains ownership for retry");
 }
}'''
}
for name,text in stubs.items():f=P/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text,encoding='utf8')
classes=P/'classes';classes.mkdir(exist_ok=True);sources=[str(P/n) for n in stubs]+[str(ROOT/'global/java/li/songe/gkd/global/GlobalBridge.java')];records=[]
for args in [[str(JDK/'javac.exe') if os.name=='nt' else str(JDK/'javac'),'--release','17','-d',str(classes),*sources],[str(JDK/'java.exe') if os.name=='nt' else str(JDK/'java'),'-cp',str(classes),'GlobalBridgeAuditCheck']]:
 r=subprocess.run(args,capture_output=True,text=True,encoding='utf8',errors='replace',timeout=30);records.append(dict(command=args,exit=r.returncode,stdout=r.stdout,stderr=r.stderr));assert r.returncode==0,records[-1]
assert records[-1]['stdout'].count('PASS ')==6
for _ in range(options.repeat):
 r=subprocess.run([str(JDK/'java.exe') if os.name=='nt' else str(JDK/'java'),'-cp',str(classes),'GlobalBridgeAuditCheck'],capture_output=True,text=True,encoding='utf8',errors='replace',timeout=30);records.append(dict(command=r.args,exit=r.returncode,stdout=r.stdout,stderr=r.stderr));assert r.returncode==0 and r.stdout.count('PASS ')==6,records[-1]
(P/'verification.json').write_text(json.dumps(dict(tests=6,repeatRuns=options.repeat+1,passed=True,fixtureRaceCorrected=True,productionChanged=False,commands=records),indent=2),encoding='utf8');print(records[-1]['stdout'])
