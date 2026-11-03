#include <sys/types.h>
#include "zygisk.hpp"
#include <android/log.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>
#include <cerrno>
#include <cctype>
#include <cstring>
#include <cstdint>
#include <string>
#include <vector>

namespace {
constexpr const char* MOD="/data/adb/modules/gkd_ksu_sukisu";
constexpr const char* POLICY="/data/adb/gkd_node_bridge/protected.v1";
void log(const char* message){__android_log_print(ANDROID_LOG_INFO,"GKD-GlobalBridge","%s",message);}
bool clear(JNIEnv* e){if(!e->ExceptionCheck())return true;e->ExceptionClear();return false;}
bool transfer(int fd,void* bytes,size_t size,bool send){
 auto* p=static_cast<char*>(bytes);
 while(size){ssize_t n=send ? ::send(fd,p,size,MSG_NOSIGNAL) : ::read(fd,p,size);if(n<0&&errno==EINTR)continue;if(n<=0)return false;p+=n;size-=n;}return true;
}
bool exists(const std::string& p){return access(p.c_str(),F_OK)==0;}
std::vector<char> readFile(const std::string& path,size_t limit){
 int fd=open(path.c_str(),O_RDONLY|O_CLOEXEC|O_NOFOLLOW);if(fd<0)return {};
 struct stat st{};if(fstat(fd,&st)!=0||!S_ISREG(st.st_mode)||st.st_uid!=0||st.st_size<=0||static_cast<uint64_t>(st.st_size)>limit){close(fd);return {};}
 std::vector<char> bytes;
 try{bytes.resize(static_cast<size_t>(st.st_size));}catch(...){close(fd);return {};}
 size_t done=0;while(done<bytes.size()){ssize_t n=read(fd,bytes.data()+done,bytes.size()-done);if(n<0&&errno==EINTR)continue;if(n<=0){bytes.clear();break;}done+=n;}
 close(fd);return bytes;
}
bool protectedStatic(const std::string& p){
 for(const char* s:{"android","li.songe.gkd","li.songe.gkd.debug","com.android.systemui","com.android.settings","com.android.shell","org.lsposed.manager","org.frknkrc44.hma_oss","com.sukisu.ultra","me.weishu.kernelsu","com.topjohnwu.magisk","com.zhenxi.hunter","com.chunqiunativecheck"})if(p==s)return true;
 return false;
}
bool canLoad(const std::string& pkg){
 if(pkg.empty()||pkg.size()>255||protectedStatic(pkg)||exists(std::string(MOD)+"/disable")||exists(std::string(MOD)+"/remove"))return false;
 for(char c:pkg)if(!(isalnum(static_cast<unsigned char>(c))||c=='.'||c=='_'))return false;
 auto bytes=readFile(POLICY,1024*1024);std::string text(bytes.begin(),bytes.end());
 if(text.rfind("GKDGLOBAL1\n",0)!=0 || text.back()!='\n')return false;
 size_t count=0,start=11,end;
 while(start<text.size()){
  end=text.find('\n',start);if(end==std::string::npos||++count>4096||end-start>255)return false;
  if(text.compare(start,end-start,pkg)==0)return false;start=end+1;
 }
 return true;
}
void companion(int fd){
 timeval timeout{1,0};setsockopt(fd,SOL_SOCKET,SO_RCVTIMEO,&timeout,sizeof(timeout));setsockopt(fd,SOL_SOCKET,SO_SNDTIMEO,&timeout,sizeof(timeout));
 uint32_t size=0;if(!transfer(fd,&size,sizeof(size),false)||size==0||size>255)return;
 std::string pkg(size,'\0');if(!transfer(fd,pkg.data(),size,false))return;
 std::vector<char> dex;
 if(canLoad(pkg))dex=readFile(std::string(MOD)+"/common/global_bridge.dex",1024*1024);
 size=static_cast<uint32_t>(dex.size());if(!transfer(fd,&size,sizeof(size),true)||!size)return;
 transfer(fd,dex.data(),dex.size(),true);
}

struct Cache {
 jclass wm=nullptr,root=nullptr,activityThread=nullptr,collection=nullptr,handlerClass=nullptr;
 jmethodID wmInstance=nullptr,currentApp=nullptr,toArray=nullptr,ensure=nullptr,remove=nullptr,windowAttributes=nullptr;
 jfieldID wmRoots=nullptr,wmLock=nullptr,attach=nullptr,view=nullptr,handler=nullptr,manager=nullptr,windowId=nullptr;
 bool ready=false;
} cache;
struct Frame {
 JNIEnv* e;bool active;
 explicit Frame(JNIEnv* env):e(env),active(env->PushLocalFrame(128)==JNI_OK){clear(e);}
 ~Frame(){if(active)e->PopLocalFrame(nullptr);}
 jobject keep(jobject result){if(!active)return nullptr;active=false;return e->PopLocalFrame(result);}
};
jclass globalClass(JNIEnv* e,const char* name){jclass c=e->FindClass(name);bool ok=clear(e);if(!ok||!c)return nullptr;auto r=(jclass)e->NewGlobalRef(c);ok=clear(e);e->DeleteLocalRef(c);return ok?r:nullptr;}
jobject reflectedField(JNIEnv* e,jclass type,const char* name){
 if(!type)return nullptr;
 jclass cls=e->FindClass("java/lang/Class");bool ok=clear(e);if(!ok||!cls)return nullptr;
 auto get=e->GetMethodID(cls,"getDeclaredField","(Ljava/lang/String;)Ljava/lang/reflect/Field;");ok=clear(e);if(!ok||!get)return nullptr;
 jstring text=e->NewStringUTF(name);ok=clear(e);if(!ok||!text)return nullptr;
 jobject f=e->CallObjectMethod(type,get,text);ok=clear(e);return ok?f:nullptr;
}
jclass reflectedType(JNIEnv* e,jobject f){
 if(!f)return nullptr;jclass fc=e->GetObjectClass(f);bool ok=clear(e);if(!ok||!fc)return nullptr;
 auto m=e->GetMethodID(fc,"getType","()Ljava/lang/Class;");ok=clear(e);if(!ok||!m)return nullptr;
 auto result=(jclass)e->CallObjectMethod(f,m);ok=clear(e);return ok?result:nullptr;
}
bool primitive(JNIEnv* e,jclass type,bool expected){
 if(!type)return false;jclass c=e->FindClass("java/lang/Class");bool ok=clear(e);if(!ok||!c)return false;
 auto m=e->GetMethodID(c,"isPrimitive","()Z");ok=clear(e);if(!ok||!m)return false;
 bool value=e->CallBooleanMethod(type,m);ok=clear(e);return ok&&value==expected;
}
jfieldID field(JNIEnv* e,jclass type,const char* name,jclass* declared=nullptr,bool integer=false){
 jobject f=reflectedField(e,type,name);if(!f)return nullptr;jclass ft=reflectedType(e,f);if(!primitive(e,ft,integer))return nullptr;
 if(integer){
  jclass c=e->FindClass("java/lang/Class");bool ok=clear(e);if(!ok||!c)return nullptr;
  auto getName=e->GetMethodID(c,"getName","()Ljava/lang/String;");ok=clear(e);if(!ok||!getName)return nullptr;
  auto s=(jstring)e->CallObjectMethod(ft,getName);ok=clear(e);if(!ok||!s)return nullptr;
  const char* chars=e->GetStringUTFChars(s,nullptr);ok=clear(e);if(!ok||!chars)return nullptr;
  bool isInt=strcmp(chars,"int")==0;e->ReleaseStringUTFChars(s,chars);if(!isInt)return nullptr;
 }
 jfieldID result=e->FromReflectedField(f);bool ok=clear(e);if(declared)*declared=ft;return ok?result:nullptr;
}
bool initCache(JNIEnv* e){
 cache.ready=false;
 cache.wm=globalClass(e,"android/view/WindowManagerGlobal");cache.activityThread=globalClass(e,"android/app/ActivityThread");
 cache.root=globalClass(e,"android/view/ViewRootImpl");cache.collection=globalClass(e,"java/util/Collection");cache.handlerClass=globalClass(e,"android/os/Handler");
 if(!cache.wm||!cache.activityThread||!cache.root||!cache.collection||!cache.handlerClass)return false;
 cache.wmInstance=e->GetStaticMethodID(cache.wm,"getInstance","()Landroid/view/WindowManagerGlobal;");
 if(!clear(e)||!cache.wmInstance)return false;
 cache.currentApp=e->GetStaticMethodID(cache.activityThread,"currentApplication","()Landroid/app/Application;");
 if(!clear(e)||!cache.currentApp)return false;
 cache.toArray=e->GetMethodID(cache.collection,"toArray","()[Ljava/lang/Object;");if(!clear(e)||!cache.toArray)return false;
 cache.wmRoots=field(e,cache.wm,"mRoots");cache.wmLock=field(e,cache.wm,"mLock");
 jclass at=nullptr,manager=nullptr,handlerType=nullptr;
 cache.attach=field(e,cache.root,"mAttachInfo",&at);cache.view=field(e,cache.root,"mView");cache.handler=field(e,cache.root,"mHandler",&handlerType);cache.manager=field(e,cache.root,"mAccessibilityInteractionConnectionManager",&manager);
 if(!at||!manager||!handlerType)return false;
 jclass cls=e->FindClass("java/lang/Class");if(!clear(e)||!cls)return false;
 auto assignable=e->GetMethodID(cls,"isAssignableFrom","(Ljava/lang/Class;)Z");if(!clear(e)||!assignable)return false;
 bool handlerOk=e->CallBooleanMethod(cache.handlerClass,assignable,handlerType);if(!clear(e)||!handlerOk)return false;
 cache.windowId=field(e,at,"mAccessibilityWindowId",nullptr,true);
 cache.ensure=e->GetMethodID(manager,"ensureConnection","()V");if(!clear(e)||!cache.ensure)return false;
 cache.remove=e->GetMethodID(manager,"ensureNoConnection","()V");if(!clear(e)||!cache.remove)return false;
 cache.windowAttributes=e->GetMethodID(cache.root,"setAccessibilityWindowAttributesIfNeeded","()V");clear(e); // optional across platform releases
 cache.ready=clear(e)&&cache.wmInstance&&cache.currentApp&&cache.toArray&&cache.wmRoots&&cache.wmLock&&cache.attach&&cache.view&&cache.handler&&cache.manager&&cache.windowId&&cache.ensure&&cache.remove;
 return cache.ready;
}
jobject application(JNIEnv* e,jclass){if(!cache.ready)return nullptr;auto app=e->CallStaticObjectMethod(cache.activityThread,cache.currentApp);if(!clear(e))return nullptr;return app;}
jobjectArray roots(JNIEnv* e,jclass){
 Frame frame(e);if(!frame.active||!cache.ready)return nullptr;
 jobject wm=e->CallStaticObjectMethod(cache.wm,cache.wmInstance);bool ok=clear(e);if(!ok||!wm)return nullptr;
 jobject lock=e->GetObjectField(wm,cache.wmLock);ok=clear(e);if(!ok||!lock)return nullptr;
 if(e->MonitorEnter(lock)!=JNI_OK){clear(e);return nullptr;}
 jobject list=e->GetObjectField(wm,cache.wmRoots);ok=clear(e);jobjectArray result=nullptr;
 if(ok&&list&&e->IsInstanceOf(list,cache.collection)){result=(jobjectArray)e->CallObjectMethod(list,cache.toArray);ok=clear(e);}
 e->MonitorExit(lock);ok=clear(e)&&ok;return ok?(jobjectArray)frame.keep(result):nullptr;
}
jobject handler(JNIEnv* e,jclass,jobject root){if(!cache.ready||!root||!e->IsInstanceOf(root,cache.root))return nullptr;jobject h=e->GetObjectField(root,cache.handler);bool ok=clear(e);return ok&&h&&e->IsInstanceOf(h,cache.handlerClass)?h:nullptr;}
jint registerRoot(JNIEnv* e,jclass,jobject root){
 Frame frame(e);if(!frame.active||!cache.ready||!root||!e->IsInstanceOf(root,cache.root))return -1;
 jobject view=e->GetObjectField(root,cache.view);bool ok=clear(e);if(!ok||!view)return -1;
 jobject attach=e->GetObjectField(root,cache.attach);ok=clear(e);if(!ok||!attach)return -1;
 jint before=e->GetIntField(attach,cache.windowId);if(!clear(e))return -1;if(before!=-1)return -1;
 jobject manager=e->GetObjectField(root,cache.manager);ok=clear(e);if(!ok||!manager)return -1;
 e->CallVoidMethod(manager,cache.ensure);clear(e);
 // A failed transaction may already have created a window. Retain ownership for cleanup.
 jint after=e->GetIntField(attach,cache.windowId);if(!clear(e))return -1;
 if(after>=0&&cache.windowAttributes){e->CallVoidMethod(root,cache.windowAttributes);clear(e);}
 return after;
}
jboolean unregisterRoot(JNIEnv* e,jclass,jobject root,jint expected){
 Frame frame(e);if(!frame.active||!cache.ready||!root||!e->IsInstanceOf(root,cache.root))return false;
 jobject attach=e->GetObjectField(root,cache.attach);bool ok=clear(e);if(!ok)return false;if(!attach)return true;
 jint current=e->GetIntField(attach,cache.windowId);if(!clear(e))return false;if(current!=expected)return true;
 jobject manager=e->GetObjectField(root,cache.manager);ok=clear(e);if(!ok||!manager)return false;
 e->CallVoidMethod(manager,cache.remove);clear(e);
 current=e->GetIntField(attach,cache.windowId);return clear(e)&&current!=expected;
}
bool launch(JNIEnv* e,std::vector<char>& dex){
 Frame frame(e);if(!frame.active)return false;
 if(!initCache(e)){log("private API layout unavailable; host left untouched");return false;}
 jclass loader=e->FindClass("dalvik/system/InMemoryDexClassLoader");bool ok=clear(e);if(!ok||!loader)return false;
 jclass cl=e->FindClass("java/lang/ClassLoader");ok=clear(e);if(!ok||!cl)return false;
 auto parentMethod=e->GetStaticMethodID(cl,"getSystemClassLoader","()Ljava/lang/ClassLoader;");
 if(!clear(e)||!parentMethod)return false;
 auto parent=e->CallStaticObjectMethod(cl,parentMethod);ok=clear(e);if(!ok||!parent)return false;
 auto bytes=e->NewDirectByteBuffer(dex.data(),dex.size());ok=clear(e);if(!ok||!bytes)return false;
 auto constructor=e->GetMethodID(loader,"<init>","(Ljava/nio/ByteBuffer;Ljava/lang/ClassLoader;)V");
 if(!clear(e)||!constructor)return false;
 auto instance=e->NewObject(loader,constructor,bytes,parent);ok=clear(e);if(!ok||!instance)return false;
 auto load=e->GetMethodID(cl,"loadClass","(Ljava/lang/String;)Ljava/lang/Class;");ok=clear(e);if(!ok||!load)return false;
 jstring name=e->NewStringUTF("li.songe.gkd.global.GlobalBridge");ok=clear(e);if(!ok||!name)return false;
 auto type=(jclass)e->CallObjectMethod(instance,load,name);ok=clear(e);if(!ok||!type)return false;
 JNINativeMethod methods[]={
  {const_cast<char*>("application"),const_cast<char*>("()Landroid/app/Application;"),(void*)application},
  {const_cast<char*>("roots"),const_cast<char*>("()[Ljava/lang/Object;"),(void*)roots},
  {const_cast<char*>("rootHandler"),const_cast<char*>("(Ljava/lang/Object;)Landroid/os/Handler;"),(void*)handler},
  {const_cast<char*>("register"),const_cast<char*>("(Ljava/lang/Object;)I"),(void*)registerRoot},
  {const_cast<char*>("unregister"),const_cast<char*>("(Ljava/lang/Object;I)Z"),(void*)unregisterRoot},
 };
 int status=e->RegisterNatives(type,methods,5);ok=clear(e);if(!ok||status!=JNI_OK)return false;
 auto start=e->GetStaticMethodID(type,"start","()V");if(!clear(e)||!start)return false;e->CallStaticVoidMethod(type,start);ok=clear(e);
 e->DeleteLocalRef(type);e->DeleteLocalRef(name);e->DeleteLocalRef(instance);e->DeleteLocalRef(bytes);e->DeleteLocalRef(parent);e->DeleteLocalRef(loader);e->DeleteLocalRef(cl);return ok;
}
class GlobalModule : public zygisk::ModuleBase {
 zygisk::Api* api=nullptr;JNIEnv* env=nullptr;std::vector<char> dex;bool traceFixture=false;
public:
 void onLoad(zygisk::Api* a,JNIEnv* e) override {api=a;env=e;}
 void preAppSpecialize(zygisk::AppSpecializeArgs* args) override {try{pre(args);}catch(...){dex.clear();clear(env);log("pre-specialize stopped safely");}}
 void pre(zygisk::AppSpecializeArgs* args) {
  int uid=args->uid,user=uid/100000,id=uid%100000;
  if(args->nice_name){const char* name=env->GetStringUTFChars(args->nice_name,nullptr);bool valid=clear(env);if(valid&&name){traceFixture=strcmp(name,"li.songe.gkd.globalfixture")==0;env->ReleaseStringUTFChars(args->nice_name,name);}}
  // KernelSU's unmount/denylist advisory can include every non-root app. The host
  // already controls whether this module is delivered. Do not turn that advisory
  // into an extra target allowlist; use GKD's protected cache and live policy.
  if((user!=0&&user!=999)||id<10000||id>19999||!args->app_data_dir)return;
  const char* data=env->GetStringUTFChars(args->app_data_dir,nullptr);bool ok=clear(env);if(!ok||!data)return;
  std::string pkg;
  try{const char* slash=strrchr(data,'/');pkg=slash?slash+1:data;}catch(...){env->ReleaseStringUTFChars(args->app_data_dir,data);throw;}
  env->ReleaseStringUTFChars(args->app_data_dir,data);
  if(traceFixture)__android_log_print(ANDROID_LOG_INFO,"GKD-GlobalBridge","fixture pre uid=%d package=%s hostFlags=%u",uid,pkg.c_str(),api->getFlags());
  if(protectedStatic(pkg))return;
  int fd=api->connectCompanion();if(fd<0)return;
  timeval timeout{1,0};setsockopt(fd,SOL_SOCKET,SO_RCVTIMEO,&timeout,sizeof(timeout));setsockopt(fd,SOL_SOCKET,SO_SNDTIMEO,&timeout,sizeof(timeout));
  uint32_t length=pkg.size();
  if(transfer(fd,&length,sizeof(length),true)&&transfer(fd,pkg.data(),pkg.size(),true)&&transfer(fd,&length,sizeof(length),false)&&length>0&&length<=1024*1024){
   try{dex.resize(length);}catch(...){close(fd);throw;}if(!transfer(fd,dex.data(),length,false))dex.clear();
  }
  close(fd);
 }
 void postAppSpecialize(const zygisk::AppSpecializeArgs*) override {
  if(traceFixture)__android_log_print(ANDROID_LOG_INFO,"GKD-GlobalBridge","fixture helper bytes=%zu",dex.size());
  if(dex.empty()){api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);return;}
  // Keep JNI callback code mapped for the life of the process, even if Java startup fails.
  try{if(launch(env,dex))log("global helper loaded");else{clear(env);log("global helper did not start");}}
  catch(...){clear(env);log("post-specialize stopped safely");}
 }
 void postServerSpecialize(const zygisk::ServerSpecializeArgs*) override {api->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);}
};
} // namespace
REGISTER_ZYGISK_MODULE(GlobalModule)
static void companionGuard(int fd){try{companion(fd);}catch(...){log("companion stopped safely");}}
REGISTER_ZYGISK_COMPANION(companionGuard)

// Local app_process verification uses the identical loader/JNI paths, not a separate mock.
extern "C" JNIEXPORT jboolean JNICALL Java_GlobalLoaderCheck_load(JNIEnv* env,jclass,jbyteArray input){
 try{if(!input)return false;static std::vector<char> data;jsize n=env->GetArrayLength(input);if(!clear(env)||n<=0||n>1024*1024)return false;data.resize(n);env->GetByteArrayRegion(input,0,n,(jbyte*)data.data());if(!clear(env))return false;return launch(env,data);}catch(...){clear(env);return false;}
}
