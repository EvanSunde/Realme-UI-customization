package np.evan.lockscreen;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import rikka.shizuku.Shizuku;

public final class MainActivity extends Activity {
 private ThemeBuilder.Design design=new ThemeBuilder.Design();
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private SharedPreferences prefs;private WebView web;private IBinder shell;private boolean busy=false,ready=false,binding=false;
 private String photoName="wallpaper-source",subjectName="",message="Connect Shizuku to apply your design.";
 private String selectedIconPack="",pendingData="";private int selectedIconShape=0;private JSONObject deletedScene=null;
 private int delay=10,revision=0;private volatile byte[] backgroundPreview=new byte[0],subjectPreview=new byte[0];
 private static final String ORIGIN="https://appassets.androidplatform.net";
 private final Shizuku.UserServiceArgs serviceArgs=new Shizuku.UserServiceArgs(new ComponentName("np.evan.lockscreen",ShellService.class.getName())).daemon(true).processNameSuffix("theme_shell").version(12);
 private final ServiceConnection connection=new ServiceConnection(){
  public void onServiceConnected(ComponentName n,IBinder binder){shell=binder;binding=false;status("Shizuku connected. Your studio is ready.",false);if(ready)web.evaluateJavascript("if(typeof UI!=='undefined'){if(UI.tab==='aod')readAod();if(UI.tab==='shade')native('shadeRead');}",null);}
  public void onServiceDisconnected(ComponentName n){shell=null;binding=false;status("Shizuku disconnected. Start it and reconnect.",true);}
 };
 // Shizuku remembers the grant, so the studio reconnects by itself whenever Shizuku is running.
 private final Shizuku.OnBinderReceivedListener received=()->runOnUiThread(()->{if(!autoConnect())status("Shizuku is running. Tap Connect once to authorize the studio.",false);});
 private final Shizuku.OnBinderDeadListener dead=()->runOnUiThread(()->{shell=null;binding=false;status("Shizuku stopped. Start it again; the studio reconnects automatically.",true);});
 private final Shizuku.OnRequestPermissionResultListener permission=(request,result)->runOnUiThread(()->{if(result==PackageManager.PERMISSION_GRANTED)connect();else status("Shizuku access was denied.",true);});
 @Override public void onCreate(Bundle state){
  super.onCreate(state);getWindow().setStatusBarColor(0xff0b0d14);getWindow().setNavigationBarColor(0xff0b0d14);prefs=getSharedPreferences("design",MODE_PRIVATE);try{StudioFonts.init(this);}catch(Exception e){message="Typeface loading failed: "+e.getMessage();}
  try{
   if(prefs.contains("studioState")){JSONObject j=new JSONObject(prefs.getString("studioState","{}"));design=DesignIO.parse(j.getJSONObject("design"));photoName=j.optString("photo","wallpaper-source");subjectName=j.optString("subject","");}
   else{JSONObject j=new JSONObject();for(String key:new String[]{"size","vertical","horizontal","color","font"})if(prefs.contains(key))j.put(key,prefs.getInt(key,0));for(String key:new String[]{"stacked","twelve","date"})if(prefs.contains(key))j.put(key,prefs.getBoolean(key,false));design=DesignIO.parse(j);if(design.font==1)design.font=2;else if(design.font==2)design.font=3;}
  }catch(Exception ignored){}
  delay=Math.max(5,Math.min(30,prefs.getInt("activationDelay",10)));
  selectedIconPack=prefs.getString("selectedIconPack","");selectedIconShape=Math.max(0,Math.min(IconThemeBuilder.SHAPES.length-1,prefs.getInt("selectedIconShape",0)));
  android.util.DisplayMetrics dm=new android.util.DisplayMetrics();getWindowManager().getDefaultDisplay().getRealMetrics(dm);design.height=Math.max(1800,Math.min(2700,Math.round(1080f*Math.max(dm.widthPixels,dm.heightPixels)/Math.min(dm.widthPixels,dm.heightPixels))));
  web=new WebView(this);web.setBackgroundColor(0xff10131b);web.setOverScrollMode(android.view.View.OVER_SCROLL_NEVER);WebSettings settings=web.getSettings();settings.setTextZoom(100);settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(false);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);web.addJavascriptInterface(new Bridge(),"StudioNative");
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest req){
    Uri uri=req.getUrl();if(!"https".equals(uri.getScheme())||!"appassets.androidplatform.net".equals(uri.getHost()))return empty();
    String path=uri.getPath();
    try{
     if("/index.html".equals(path))return new WebResourceResponse("text/html","UTF-8",getAssets().open("studio.html"));
     if(path!=null&&path.startsWith("/fonts/")&&path.substring(7).matches("[a-zA-Z0-9._-]+"))return new WebResourceResponse(path.endsWith(".json")?"application/json":"font/ttf",null,getAssets().open("fonts/"+path.substring(7)));
     if("/preview/background".equals(path))return new WebResourceResponse("image/jpeg",null,new ByteArrayInputStream(backgroundPreview));
     if("/preview/subject".equals(path))return new WebResourceResponse("image/png",null,new ByteArrayInputStream(subjectPreview));
    }catch(IOException ignored){}return empty();
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){return true;}
   @Override public void onPageFinished(WebView view,String url){ready=true;sendUiMetrics();sendState();refreshPreviews();}
  });setContentView(web);web.loadUrl(ORIGIN+"/index.html");
  Shizuku.addBinderReceivedListenerSticky(received);Shizuku.addBinderDeadListener(dead);Shizuku.addRequestPermissionResultListener(permission);
 }
 @Override public void onConfigurationChanged(android.content.res.Configuration config){super.onConfigurationChanged(config);sendUiMetrics();}
 private void sendUiMetrics(){try{android.content.res.Configuration config=getResources().getConfiguration();emit("uiMetrics",new JSONObject().put("fontScale",config.fontScale).put("widthDp",config.screenWidthDp).put("heightDp",config.screenHeightDp).put("density",getResources().getDisplayMetrics().density));}catch(Exception ignored){}}
 private WebResourceResponse empty(){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
 private File file(String name){if(name==null||name.isEmpty()||!name.matches("[a-zA-Z0-9._-]+"))return null;File f=new File(getFilesDir(),name);return f.isFile()?f:null;}
 private JSONObject current()throws Exception{return new JSONObject().put("design",DesignIO.json(design)).put("photo",photoName).put("subject",subjectName);}
 private void save(){try{prefs.edit().putString("studioState",current().toString()).putInt("activationDelay",delay).apply();}catch(Exception e){status(e.getMessage(),true);}}
 private JSONArray presets(){try{return new JSONArray(prefs.getString("studioPresets","[]"));}catch(Exception e){return new JSONArray();}}
 private void emit(String type,JSONObject data){if(ready&&!isFinishing())web.evaluateJavascript("window.receive("+JSONObject.quote(type)+","+data.toString()+")",null);}
 private void status(String text,boolean error){message=text;try{emit("status",new JSONObject().put("text",text).put("error",error).put("busy",busy).put("connected",shell!=null&&shell.isBinderAlive()));}catch(Exception ignored){}}
 private void sendState(){try{
  JSONObject j=current();JSONArray names=new JSONArray(),all=presets();for(int i=0;i<all.length();i++)names.put(all.getJSONObject(i));
  j.put("shadeChoices",new JSONObject(prefs.getString("shadeChoices",ShadeJson.json(new ShadeOptions()).toString()))).put("shadePresets",new JSONArray(prefs.getString("shadePresets","[]"))).put("shadeDirty",prefs.getBoolean("shadeDirty",false));
  j.put("fresh",!prefs.contains("studioState")&&!prefs.contains("size")).put("presets",names).put("delay",delay).put("subjectReady",file(subjectName)!=null).put("photoReady",file(photoName)!=null).put("nextAlarm",alarmText()).put("battery",batteryPercent()).put("iconPack",selectedIconPack).put("iconShape",selectedIconShape).put("revision",revision).put("status",message).put("busy",busy).put("connected",shell!=null&&shell.isBinderAlive());emit("state",j);
 }catch(Exception e){status(e.getMessage(),true);}}
 private void refreshPreviews(){final File photo=file(photoName),subject=file(subjectName);final int height=design.height;
  worker.execute(()->{try{
   Bitmap bg=ThemeBuilder.wallpaper(photo,height),small=Bitmap.createScaledBitmap(bg,540,height/2,true);ByteArrayOutputStream bytes=new ByteArrayOutputStream();small.compress(Bitmap.CompressFormat.JPEG,85,bytes);backgroundPreview=bytes.toByteArray();if(small!=bg)small.recycle();bg.recycle();
   subjectPreview=new byte[0];if(subject!=null){Bitmap b=ThemeBuilder.subject(subject,height),s=Bitmap.createScaledBitmap(b,540,height/2,true);bytes.reset();s.compress(Bitmap.CompressFormat.PNG,100,bytes);subjectPreview=bytes.toByteArray();if(s!=b)s.recycle();b.recycle();}
   runOnUiThread(()->{revision++;sendState();});
  }catch(Exception e){runOnUiThread(()->status("Preview: "+e.getMessage(),true));}});
 }
 private final class Bridge {
  @JavascriptInterface public void action(String action,String raw){runOnUiThread(()->dispatch(action,raw));}
 }
 private void dispatch(String action,String raw){try{
  JSONObject j=new JSONObject(raw==null?"{}":raw);
  if(action.equals("shadeChange")){prefs.edit().putString("shadeChoices",ShadeJson.json(ShadeJson.parse(j)).toString()).putBoolean("shadeDirty",j.optBoolean("_draft",true)).apply();return;}
  if(action.equals("shadeSavePreset")){JSONArray a=new JSONArray(prefs.getString("shadePresets","[]"));if(a.length()>=12)throw new IOException("Delete a shade preset before adding another (12 maximum)");String name=j.optString("name","My shade").trim();if(name.isEmpty())name="My shade";a.put(new JSONObject().put("id",UUID.randomUUID().toString()).put("name",name.substring(0,Math.min(40,name.length()))).put("settings",ShadeJson.json(ShadeJson.parse(j.getJSONObject("settings")))));prefs.edit().putString("shadePresets",a.toString()).apply();emit("shadePresets",new JSONObject().put("presets",a));return;}
  if(action.equals("shadeDeletePreset")){JSONArray old=new JSONArray(prefs.getString("shadePresets","[]")),a=new JSONArray();for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).optString("id").equals(j.optString("id")))a.put(old.getJSONObject(i));prefs.edit().putString("shadePresets",a.toString()).apply();emit("shadePresets",new JSONObject().put("presets",a));return;}
  if(action.equals("shadeBlur")){shadeAction(18,j.toString());return;}
  if(action.equals("aodDisplayRead")||action.equals("aodDisplayApply")||action.equals("aodDisplayRestore")){displayAction(action.equals("aodDisplayApply")?20:action.equals("aodDisplayRestore")?21:19,j.toString());return;}
  if(action.equals("shadeRead")||action.equals("shadeApply")||action.equals("shadeRestore")){if(action.equals("shadeApply")){ShadeOptions options=ShadeJson.parse(j);prefs.edit().putString("shadeChoices",ShadeJson.json(options).toString()).putBoolean("shadeDirty",true).apply();}shadeAction(action.equals("shadeApply")?16:action.equals("shadeRestore")?17:15,j.toString());return;}
  if(action.equals("change")){design=DesignIO.parse(j.getJSONObject("design"));delay=Math.max(5,Math.min(30,j.optInt("delay",delay)));save();return;}
  if(action.equals("connect")){connect();return;}
  if(action.equals("copy")){((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Studio scene",j.optString("text","")));return;}
  if(action.equals("exportData")){pendingData=j.getJSONObject("data").toString(2);Intent out=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("application/json").putExtra(Intent.EXTRA_TITLE,"lockscreen-studio.json");startActivityForResult(out,20);return;}
  if(action.equals("importData")){startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"),21);return;}
  if(action.equals("importScenes")){if(j.has("design")){design=DesignIO.parse(j.getJSONObject("design"));save();}JSONArray in=j.getJSONArray("scenes"),all=presets();Set<String> ids=new HashSet<>();for(int i=0;i<all.length();i++)ids.add(all.getJSONObject(i).optString("id"));for(int i=0;i<in.length()&&all.length()<20;i++){JSONObject scene=in.getJSONObject(i);ThemeBuilder.Design imported=DesignIO.parse(scene.getJSONObject("design"));String id=scene.optString("id","");if(!id.matches("[a-zA-Z0-9_-]{1,80}"))id=UUID.randomUUID().toString();if(!ids.add(id))continue;String name=scene.optString("name","Imported scene");if(name.length()>40)name=name.substring(0,40);all.put(new JSONObject().put("id",id).put("name",name).put("design",DesignIO.json(imported)).put("photo",photoName).put("subject",subjectName));}prefs.edit().putString("studioPresets",all.toString()).apply();sendState();return;}
  if(action.equals("undoSceneDelete")){if(deletedScene!=null){JSONArray all=presets();if(all.length()<20)all.put(deletedScene);deletedScene=null;prefs.edit().putString("studioPresets",all.toString()).apply();sendState();}return;}
  if(action.equals("keep")||action.equals("revert")||action.equals("recoveryStatus")){engineAction(action.equals("keep")?6:action.equals("revert")?7:8);return;}
  if(action.equals("apply")||action.equals("export")){design=DesignIO.parse(j.getJSONObject("design"));delay=Math.max(5,Math.min(30,j.optInt("delay",delay)));save();generate(action.equals("apply"));return;}
  if(action.equals("photo")||action.equals("subject")){Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT);pick.addCategory(Intent.CATEGORY_OPENABLE);pick.setType(action.equals("subject")?"image/png":"image/*");startActivityForResult(pick,action.equals("subject")?11:10);return;}
  if(action.equals("autoSubject")){autoSubject();return;}
  if(action.equals("resetSubject")){AutoSubject.reset(this);status("Subject model safety state reset. You can try separation again.",false);return;}
  if(action.equals("removeSubject")){subjectName="";design.depth=false;updateImageState(false,false);save();cleanupImages();refreshPreviews();return;}
  if(action.equals("presetSave")){if(busy)return;design=DesignIO.parse(j.getJSONObject("design"));JSONArray all=presets();if(all.length()>=20)throw new IOException("You can save up to 20 scenes. Delete one first.");String name=j.optString("name","My scene").trim();if(name.isEmpty())name="My scene";if(name.length()>40)name=name.substring(0,40);all.put(current().put("name",name).put("id",UUID.randomUUID().toString()));prefs.edit().putString("studioPresets",all.toString()).apply();save();sendState();status("Scene saved. Photo files are shared between scenes, not duplicated.",false);return;}
  if(action.equals("presetLoad")||action.equals("presetDelete")){if(busy)return;JSONArray all=presets(),keep=new JSONArray();String id=j.getString("id");for(int i=0;i<all.length();i++){JSONObject scene=all.getJSONObject(i);if(!id.equals(scene.getString("id"))){keep.put(scene);continue;}if(action.equals("presetLoad")){design=DesignIO.parse(scene.getJSONObject("design"));photoName=scene.optString("photo","");subjectName=scene.optString("subject","");save();refreshPreviews();status("Scene loaded. Apply when ready.",false);return;}deletedScene=scene;}if(action.equals("presetDelete")){prefs.edit().putString("studioPresets",keep.toString()).apply();cleanupImages();sendState();}return;}
  if(action.equals("restore")){engineAction(2);return;}if(action.equals("activate")){engineAction(3);return;}
  if(action.equals("materialColor")){int id=android.R.color.system_accent1_100;int c=getResources().getColor(id,getTheme());emit("materialColor",new JSONObject().put("color",c));status("Material You accent sampled. Tap Apply to update the native clock.",false);return;}
  if(action.equals("iconPacks")){worker.execute(()->{try{JSONArray list=new JSONArray();for(String[] pack:IconThemeBuilder.packs(this))list.put(new JSONObject().put("package",pack[0]).put("name",pack[1]));runOnUiThread(()->{try{emit("iconPacks",new JSONObject().put("packs",list));}catch(Exception ignored){}});}catch(Exception e){runOnUiThread(()->status("Icon packs: "+e.getMessage(),true));}});return;}
  if(action.equals("applyIcons")||action.equals("exportIcons")){selectedIconPack=j.optString("package","");selectedIconShape=Math.max(0,Math.min(IconThemeBuilder.SHAPES.length-1,j.optInt("shape",0)));prefs.edit().putString("selectedIconPack",selectedIconPack).putInt("selectedIconShape",selectedIconShape).apply();icons(selectedIconPack,selectedIconShape,action.equals("applyIcons"));return;}
  if(action.equals("iconChoice")){selectedIconPack=j.optString("package","");selectedIconShape=Math.max(0,Math.min(IconThemeBuilder.SHAPES.length-1,j.optInt("shape",0)));prefs.edit().putString("selectedIconPack",selectedIconPack).putInt("selectedIconShape",selectedIconShape).apply();return;}
  if(action.equals("atmos")){for(String pkg:new String[]{"com.saad_khan_rind.atmosphere_effect","com.saad_khan_rind.atmosphere_effect.debug"}){Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);if(launch!=null){startActivity(launch);return;}}throw new IOException("Atmos was not found under its known package names. Open it from your launcher.");}
  if(action.equals("wallpaperPicker")){startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));return;}
  if(action.equals("themeStore")){Intent launch=getPackageManager().getLaunchIntentForPackage("com.oplus.themestore");if(launch!=null)startActivity(launch);return;}
  if(action.equals("appInfo")){startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));return;}
  if(action.equals("aodSettings")){startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));return;}
  if(action.equals("maps")){Intent launch=getPackageManager().getLaunchIntentForPackage("com.google.android.apps.maps");if(launch==null)throw new IOException("Google Maps is not installed or is unavailable in this Android user.");startActivity(launch);return;}
  if(action.equals("aodRead")||action.equals("aodTest")||action.equals("aodRestore")||action.equals("aodStatus")||action.equals("aodEnable")||action.equals("aodKeep")){engineAction(action.equals("aodRead")?9:action.equals("aodTest")?10:action.equals("aodRestore")?11:action.equals("aodEnable")?13:action.equals("aodKeep")?14:12,j.toString());return;}
  if(action.equals("details")){details();return;}if(action.equals("diagnostics")){engineAction(4);return;}
 }catch(Exception e){status(e.getMessage(),true);}}
 private void cleanupImages(){
  try{Set<String> retained=new HashSet<>();retained.add(photoName);retained.add(subjectName);if(deletedScene!=null){retained.add(deletedScene.optString("photo",""));retained.add(deletedScene.optString("subject",""));}JSONArray scenes=presets();for(int i=0;i<scenes.length();i++){retained.add(scenes.getJSONObject(i).optString("photo",""));retained.add(scenes.getJSONObject(i).optString("subject",""));}File[] files=getFilesDir().listFiles();if(files!=null)for(File image:files){String n=image.getName();if((n.startsWith("photo-")||n.startsWith("subject-")||n.equals("wallpaper-source"))&&!retained.contains(n))image.delete();}}catch(Exception ignored){}
 }
 private void icons(String pack,int shape,boolean apply){
  if(busy)return;if(apply&&(shell==null||!shell.isBinderAlive())){status("Connect Shizuku before applying icons.",true);return;}
  busy=true;status("Building icon module for your launcher apps…",false);
  worker.execute(()->{Uri saved=null;boolean complete=false;try{
   byte[] bytes=IconThemeBuilder.build(this,pack,shape);String filename="Evan-Icons-"+System.currentTimeMillis()+".theme";
   ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,filename);v.put(MediaStore.MediaColumns.MIME_TYPE,"application/octet-stream");v.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/EvanLockscreen");v.put(MediaStore.MediaColumns.IS_PENDING,1);
   saved=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(saved==null)throw new IOException("Cannot create icon export");
   try(OutputStream out=getContentResolver().openOutputStream(saved)){if(out==null)throw new IOException("Cannot write icon export");out.write(bytes);}
   v.clear();v.put(MediaStore.MediaColumns.IS_PENDING,0);getContentResolver().update(saved,v,null,null);complete=true;
   String result=apply?call(5,new File(Environment.getExternalStorageDirectory(),"Download/EvanLockscreen/"+filename).getPath(),0):"Icon-only theme exported: Download/EvanLockscreen/"+filename;
   runOnUiThread(()->{busy=false;status(result,result==null||result.startsWith("ERROR:"));});
  }catch(Exception e){if(saved!=null&&!complete)try{getContentResolver().delete(saved,null,null);}catch(Exception ignored){}runOnUiThread(()->{busy=false;status("Icons: "+e.getMessage(),true);});}});
 }
 private void autoSubject(){
  if(busy)return;final File photo=file(photoName);if(photo==null){status("Choose a still photo first.",true);return;}try{if(design.background==2||(!design.v2.isEmpty()&&!new JSONObject(design.v2).optString("bg","photo").equals("photo"))){status("Choose a photo background before separating its subject.",true);return;}}catch(Exception e){status("Choose a photo background first.",true);return;}
  busy=true;status("Finding the subject on your photo with ML Kit…",false);final File target=new File(getFilesDir(),"subject-"+UUID.randomUUID());final int height=design.height;
  worker.execute(()->{try{AutoSubject.extract(this,photo,height,target,worker,(cutout,error)->runOnUiThread(()->{
   busy=false;if(cutout!=null){subjectName=cutout.getName();design.depth=true;updateImageState(false,true);save();cleanupImages();refreshPreviews();status("Subject cutout created. Adjust its depth position, then Apply to the lock screen.",false);}
   else status(error,true);
  }));}catch(Exception e){target.delete();runOnUiThread(()->{busy=false;status("Subject separation: "+e.getMessage(),true);});}});
 }
 private boolean autoConnect(){try{if(shell!=null&&shell.isBinderAlive())return true;if(binding||!Shizuku.pingBinder()||Shizuku.isPreV11()||Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED)return binding;connect();return true;}catch(Exception e){return false;}}
 private void connect(){try{if(!Shizuku.pingBinder()){status("Start Shizuku, then tap Connect.",true);return;}if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED){Shizuku.requestPermission(1);return;}if(binding)return;binding=true;status("Connecting to Shizuku…",false);Shizuku.bindUserService(serviceArgs,connection);}catch(Exception e){binding=false;status("Connection: "+e.getMessage(),true);}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if((request==20||request==21)&&result==RESULT_OK&&data!=null&&data.getData()!=null){final Uri chosen=data.getData();worker.execute(()->{try{if(request==20){try(OutputStream out=getContentResolver().openOutputStream(chosen)){if(out==null)throw new IOException("Cannot open backup destination");out.write(pendingData.getBytes("UTF-8"));}runOnUiThread(()->status("Studio backup exported. Photos are not included.",false));}else{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(chosen)){if(in==null)throw new IOException("Cannot read backup");byte[] c=new byte[4096];int n;while((n=in.read(c))!=-1){if(bytes.size()+n>1024*1024)throw new IOException("Choose a backup smaller than 1 MB");bytes.write(c,0,n);}}JSONObject parsed=new JSONObject(bytes.toString("UTF-8"));runOnUiThread(()->emit("importData",parsed));}}catch(Exception e){runOnUiThread(()->status("Backup: "+e.getMessage(),true));}});return;}if((request!=10&&request!=11)||result!=RESULT_OK||data==null||data.getData()==null)return;if(busy){status("Wait for the current apply to finish before importing.",true);return;}
  busy=true;status("Importing image…",false);final Uri uri=data.getData();final boolean cutout=request==11;
  worker.execute(()->{File target=new File(getFilesDir(),(cutout?"subject-":"photo-")+UUID.randomUUID());try{
   try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(target)){if(in==null)throw new IOException("Cannot open image");byte[] b=new byte[8192];int n;long total=0;while((n=in.read(b))!=-1){total+=n;if(total>50L*1024*1024)throw new IOException("Choose an image smaller than 50 MB");out.write(b,0,n);}}
   BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(target.getPath(),options);if(options.outWidth<1||options.outHeight<1)throw new IOException("Unsupported image");
   if(cutout){options.inJustDecodeBounds=false;options.inSampleSize=Math.max(1,Math.max(options.outWidth,options.outHeight)/400);Bitmap b=BitmapFactory.decodeFile(target.getPath(),options);if(b==null)throw new IOException("Cannot decode subject");boolean transparent=false,visible=false;for(int y=0;y<b.getHeight();y+=Math.max(1,b.getHeight()/40))for(int x=0;x<b.getWidth();x+=Math.max(1,b.getWidth()/40)){int a=Color.alpha(b.getPixel(x,y));if(a<240)transparent=true;if(a>20)visible=true;}b.recycle();if(!transparent||!visible)throw new IOException("Import a transparent PNG containing a visible subject, not an opaque photo.");}
   runOnUiThread(()->{if(cutout){subjectName=target.getName();design.depth=true;}else{photoName=target.getName();subjectName="";design.depth=false;}updateImageState(!cutout,cutout);busy=false;save();cleanupImages();refreshPreviews();status(cutout?"Subject layer imported. Align it with the photo in Depth.":"Photo imported. Drag the clock to compose your scene.",false);});
  }catch(Exception e){target.delete();runOnUiThread(()->{busy=false;status("Import: "+e.getMessage(),true);});}});
 }
 private String call(int code,String path,int wait)throws Exception{
  IBinder remote=shell;if(remote==null||!remote.isBinderAlive())throw new IOException("Connect Shizuku first");Parcel data=Parcel.obtain(),reply=Parcel.obtain();
  try{data.writeInterfaceToken(ShellService.DESCRIPTOR);if(code==1||code==5||code==10||code==13||code==16||code==18||code==20)data.writeString(path);if(code==1)data.writeInt(wait);if(!remote.transact(code,data,reply,0))throw new IOException("Reconnect Shizuku after updating the app");reply.readException();return reply.readString();}finally{data.recycle();reply.recycle();}
 }
 private void shadeAction(int code,String payload){if(busy)return;if(shell==null||!shell.isBinderAlive()){status("Connect Shizuku before reading or applying the shade.",true);return;}busy=true;status(code==16?"Applying shade colours and preserving the clock…":code==17?"Restoring the shade backup…":code==18?"Changing blur…":"Reading native shade state…",false);worker.execute(()->{try{String result=call(code,payload,0);if(result==null||!result.startsWith("SHADE:"))throw new IOException(result==null?"No shade response":result);JSONObject state=new JSONObject(result.substring(6));runOnUiThread(()->{busy=false;if(code==16||code==17)prefs.edit().putBoolean("shadeDirty",state.optBoolean("error")).apply();emit("shade",state);status(state.optString("note"),state.optBoolean("error"));});}catch(Exception e){runOnUiThread(()->{busy=false;status("Shade: "+e.getMessage(),true);});}});}
 private void displayAction(int code,String payload){if(busy)return;if(shell==null||!shell.isBinderAlive()){status("Connect Shizuku before changing Always-On Display.",true);return;}busy=true;status(code==20?"Updating Always-On Display…":code==21?"Restoring Always-On Display settings…":"Reading Always-On Display settings…",false);worker.execute(()->{try{String result=call(code,payload,0);if(result==null||!result.startsWith("AODDISPLAY:"))throw new IOException(result==null?"No AOD response":result);JSONObject state=new JSONObject(result.substring(11));runOnUiThread(()->{busy=false;emit("aodDisplay",state);status(state.optString("note"),state.optBoolean("error"));});}catch(Exception e){runOnUiThread(()->{busy=false;status("AOD: "+e.getMessage(),true);});}});}
 private void generate(boolean apply){if(busy)return;if(apply&&(shell==null||!shell.isBinderAlive())){status("Connect Shizuku before applying. Export works without it.",true);return;}if(design.depth&&design.background!=2&&file(subjectName)==null){status("Import a transparent subject PNG first.",true);return;}
  busy=true;status("Building your scene. Keep the studio open until Theme Store reports completion and the clock is selected.",false);final ThemeBuilder.Design snapshot=design.copy();final File photo=file(photoName),subject=file(subjectName);
  worker.execute(()->{Uri saved=null;boolean complete=false;try{
   boolean safe=false;if(!snapshot.v2.isEmpty()){JSONObject doc=new JSONObject(snapshot.v2);doc.put("nextAlarm",alarmText());JSONObject styles=doc.optJSONObject("scheduleStyles");if(styles!=null)for(String slot:new String[]{"day","eve","night"}){JSONObject style=styles.optJSONObject(slot);if(style!=null)style.put("nextAlarm",alarmText());}snapshot.v2=doc.toString();safe=doc.optBoolean("safeRevert",false);}
   byte[] bytes=ThemeBuilder.build(snapshot,photo,subject);String filename="Evan-Lockscreen-"+System.currentTimeMillis()+".theme";ContentValues v=new ContentValues();v.put(MediaStore.MediaColumns.DISPLAY_NAME,filename);v.put(MediaStore.MediaColumns.MIME_TYPE,"application/octet-stream");v.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/EvanLockscreen");v.put(MediaStore.MediaColumns.IS_PENDING,1);saved=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(saved==null)throw new IOException("Could not create export");try(OutputStream out=getContentResolver().openOutputStream(saved)){if(out==null)throw new IOException("Could not write export");out.write(bytes);}v.clear();v.put(MediaStore.MediaColumns.IS_PENDING,0);getContentResolver().update(saved,v,null,null);complete=true;
   String path=new File(Environment.getExternalStorageDirectory(),"Download/EvanLockscreen/"+filename).getPath();String result=apply?call(1,path,safe?1:0):"Exported to Download/EvanLockscreen/"+filename;runOnUiThread(()->{busy=false;if(result!=null&&result.startsWith("CONFIRM:")){try{emit("confirm",new JSONObject().put("until",Long.parseLong(result.split(":",3)[1])));}catch(Exception ignored){}status("Theme applied. Confirm Keep, otherwise it will be reverted.",false);}else status(result,result==null||result.startsWith("ERROR:"));});
  }catch(Exception e){if(saved!=null&&!complete)try{getContentResolver().delete(saved,null,null);}catch(Exception ignored){}runOnUiThread(()->{busy=false;status("Apply/export: "+e.getMessage(),true);});}});
 }
 private void engineAction(int code){engineAction(code,null);}
 private void engineAction(int code,String payload){if(busy)return;busy=true;status(code>=9?(code==10?"Selecting the temporary native AOD test…":code==11?"Restoring previous AOD settings…":code==13?"Enabling native AOD music…":code==14?"Keeping native AOD music enabled…":"Reading native AOD state…"):code==2?"Restoring the default lock screen…":code==4?"Reading local compatibility details…":code==6?"Keeping this lock screen…":code==7?"Reverting the theme…":code==8?"Checking theme recovery…":"Activating the clock now…",false);worker.execute(()->{try{String result=call(code,payload,0);runOnUiThread(()->{busy=false;if(code>=9&&result!=null&&result.startsWith("AOD:")){try{JSONObject info=new JSONObject(result.substring(4));emit("aod",info);status(info.optBoolean("error")?info.optString("note"):"Native AOD state updated.",info.optBoolean("error"));return;}catch(Exception ignored){}}if(code==4){try{emit("diagnostics",new JSONObject().put("text",result));}catch(Exception ignored){}}if(code==8||(result!=null&&result.startsWith("RECOVERY:"))){try{emit("recovery",new JSONObject().put("state",result));}catch(Exception ignored){}}status(code==4?"Compatibility details ready.":result,result==null||result.startsWith("ERROR:"));});}catch(Exception e){runOnUiThread(()->{busy=false;status(e.getMessage(),true);});}});}
 private void updateImageState(boolean newPhoto,boolean cutout){if(design.v2.isEmpty())return;try{JSONObject doc=new JSONObject(design.v2);if(newPhoto)doc.put("bg","photo");doc.put("subjectOn",cutout);design.v2=doc.toString();}catch(Exception ignored){}}
 private String alarmText(){try{AlarmManager.AlarmClockInfo info=((AlarmManager)getSystemService(ALARM_SERVICE)).getNextAlarmClock();return info==null?"None set":new java.text.SimpleDateFormat("EEE HH:mm",Locale.getDefault()).format(new Date(info.getTriggerTime()));}catch(Exception e){return "Unavailable";}}
 private int batteryPercent(){try{Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));return i==null?0:Math.round(100f*i.getIntExtra(BatteryManager.EXTRA_LEVEL,0)/Math.max(1,i.getIntExtra(BatteryManager.EXTRA_SCALE,100)));}catch(Exception e){return 0;}}
 private void details(){try{
  String theme="Not found",wallpaper="Static wallpaper or unavailable";try{theme=getPackageManager().getPackageInfo("com.oplus.themestore",0).versionName;}catch(Exception ignored){}try{android.app.WallpaperInfo info=WallpaperManager.getInstance(this).getWallpaperInfo();if(info!=null)wallpaper=info.getPackageName()+" / "+info.getServiceName();}catch(Exception ignored){}
  emit("details",new JSONObject().put("version","0.7.0").put("device",Build.MANUFACTURER+" "+Build.MODEL).put("android",Build.VERSION.RELEASE).put("themeStore",theme).put("wallpaper",wallpaper).put("exports","Download/EvanLockscreen").put("security","Native keyguard; PIN and fingerprint stay managed by Android.").put("privacy","No Internet permission. Photos, presets and compatibility details stay on this device."));
 }catch(Exception e){status(e.getMessage(),true);}}
 @Override public void onBackPressed(){if(web!=null&&ready)web.evaluateJavascript("window.handleBack?window.handleBack():false",result->{if(!"true".equals(result))MainActivity.super.onBackPressed();});else super.onBackPressed();}
 @Override protected void onPause(){if(web!=null)web.onPause();super.onPause();}
 @Override protected void onResume(){super.onResume();autoConnect();if(web!=null){web.onResume();if(ready)web.evaluateJavascript("if(typeof UI!=='undefined'&&UI.tab==='aod'&&UI.shizuku==='connected')readAod()",null);}}
 @Override protected void onDestroy(){Shizuku.removeBinderReceivedListener(received);Shizuku.removeBinderDeadListener(dead);Shizuku.removeRequestPermissionResultListener(permission);try{Shizuku.unbindUserService(serviceArgs,connection,false);}catch(Exception ignored){}worker.shutdown();ready=false;if(web!=null){web.removeJavascriptInterface("StudioNative");web.destroy();}super.onDestroy();}
}
