package com.example.admin.classifydemo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;


import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;


/**
 * Created by admin on 2017/11/21.
 */

public class Module implements IXposedHookLoadPackage {

    ContentResolver resolver;
    Uri uri = Uri.parse("content://com.example.admin.classifydemo.provider");


    Context applicationContext = null;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        if (loadPackageParam.packageName.equals("com.tencent.mm")){
            Log.i("xyzz","进来handleLoadPackage方法");

            // 获取到当前进程的上下文
            try{
                Class<?> ContextClass = findClass("android.content.ContextWrapper",loadPackageParam.classLoader);
                findAndHookMethod(ContextClass, "getApplicationContext", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        super.afterHookedMethod(param);
                        applicationContext = (Context) param.getResult();
                        XposedBridge.log("得到上下文");
                    }
                });
            }catch (Throwable throwable){
                XposedBridge.log("获取上下文失败 "+throwable);
            }


            XposedHelpers.findAndHookMethod(Activity.class, "getIntent", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Intent intent = (Intent) param.getResult();
                    if (intent != null) {
                        Bundle bundle = intent.getExtras();
                        if (bundle != null) {
                            Log.i("xyz","intent = "+intent.toString());
                            Log.i("xyz", "bundle = "+bundle.toString());
                            XposedBridge.log("intent = " + intent.toString());
                            XposedBridge.log("bundle = " + bundle.toString());

                            // I/set     ( 6354): bundle = Bundle[{Contact_NeedShowChangeRemarkButton=false,
                            // Contact_QuanPin=wxid39j4euy01u0g41, Contact_PyInitial=WXID39J4EUY01U0G41,
                            // Contact_RegionCode=CN_Guangdong_Guangzhou, Contact_BrandList=<brandlist count="0" ver="655896973"></brandlist>,
                            // Contact_KSnsBgId=0, add_more_friend_search_scene=2, Contact_Search_Mobile=18814143550, Contact_KWeiboNick=null,
                            // Contact_KSnsBgUrl=null, Contact_KSnsIFlag=0, Contact_KWeibo_flag=0, Contact_Signature=鬼知道我经历了什么？,
                            // Contact_BIZ_KF_WORKER_ID=null, Contact_VUser_Info=null, Contact_KHideExpose=true, Contact_Nick=╰ 别太矫情没人惯着伱。,
                            // Contact_User=v1_7ee62b1257dcc980da7561f3ab4ed1a44acace415a2c24b9a42017d5fee5444c0a917b82dab7b463e76a399ee2e0e74b@stranger,
                            // Contact_VUser_Info_Flag=0, Contact_Sex=2, Contact_Alias=li-123520xx, Contact_Scene=15, Contact_KWeibo=null,
                            // AntispamTicket=v2_66883ed899a403948c2c3d9f1d229bef11cc8734ad743dbc128c771d94aa73a90c39a8da879cc994f061102e3024b559@stranger,
                            // Contact_NeedShowChangeSnsPreButton=false}]
                            if (!TextUtils.isEmpty(bundle.getString("Contact_QuanPin"))){
//                                Log.i("xyz","Module pid = "+Process.myPid());


//                                //  路径名
//                                File file = new File("/sdcard/tmp/");
//                                if (!file.exists()){
//                                    file.mkdirs();  // 创建目录
//                                }
//
//                                File local = new File(file,"tmp.txt");
//                                // 文件不存在就创建
//                                if (!local.exists()){
//                                    local.createNewFile();
//                                    Log.i("xyz","module 创建文件");
//                                }
//
//                                // 如果文件存在且文件为空
//                                if (local.exists() && local.length() == 0){
//                                    String wxid = bundle.getString("Contact_QuanPin");  //  wxid
//                                    int sex =  bundle.getInt("Contact_Sex");         //  性别
//                                    String nickName = bundle.getString("Contact_Nick"); //  昵称
//
//                                    String s2 = wxid.substring(0,4);
//                                    if (s2.equals("wxid")){
//                                        String s3 = wxid.substring(4);
//                                        wxid = s2 + "_"+s3;
//                                    }
//
//                                    StringBuilder sb = new StringBuilder();
//                                    sb.append( wxid +"\n"+ sex +"\n"+ nickName+"\n");
//                                    FileOutputStream fos = new FileOutputStream(local,false); // 这里的第二个参数代表追加还是覆盖，true为追加，false为覆盖
//                                    fos.write(sb.toString().getBytes());
//                                    fos.close();
//                                }
                                Log.i("xyzz","进入获取bundle方法");

                                String wxid = bundle.getString("Contact_QuanPin");          //  wxid
                                int sex =  bundle.getInt("Contact_Sex");                    //  性别
                                String nickName = bundle.getString("Contact_Nick");         //  昵称
                                String signature = bundle.getString("Contact_Signature");  //  个性签名
                                String v1 = bundle.getString("Contact_User");               //  v1 值
                                String v2 = bundle.getString("AntispamTicket");             //  v2 值

                                String stmp = v1.substring(0,2);
                                if (!stmp.equals("v1")){  // 如果Contact_User不是v1，那就为空格
                                    v1 = " ";
                                }

                                String s2 = wxid.substring(0,4);
                                if (s2.equals("wxid")){
                                    String s3 = wxid.substring(4);
                                    wxid = s2 + "_"+s3;
                                }

                                if (nickName!=null){
                                    nickName = nickName.replaceAll("\r|\n","");
                                }

                                if (signature!=null){
                                    signature = signature.replaceAll("\r|\n","");
                                }

                                int flag = 1;

                                resolver = applicationContext.getContentResolver();
                                ContentValues values = new ContentValues();
                                values.put("wxid",wxid);
                                values.put("sex",sex);
                                values.put("nickName",nickName);
                                values.put("flag",flag);
                                values.put("signature",signature);
                                values.put("v1",v1);
                                values.put("v2",v2);
                                // 调用ContentProvider插入数据
                                resolver.insert(uri,values);

                                Log.i("xyzz","module插入数据");
                            }


                        }
                    }
                }
            });
        }
    }
}
