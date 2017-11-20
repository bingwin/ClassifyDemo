package com.example.admin.classifydemo;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;
import static com.example.admin.classifydemo.MyService.cnt;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "xyz";
    private Button btnSelect;
    private TextView tvFilePath;
    private static final int SELECT_CODE = 1;
    private String filePath;
    private List<String> list = new ArrayList<>();

    private Button btnStart;

    private AccessibilityService mService;

    AccessibilityNodeInfo editInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSelect = findViewById(R.id.select);
        tvFilePath = findViewById(R.id.tv_hint);

        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });

        btnStart = findViewById(R.id.start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(list == null){
                    Toast.makeText(MainActivity.this,"选择文件的列表为空,请检查路径是否正确",Toast.LENGTH_SHORT).show();
                }else {
                    if (isWxAvailable()){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Looper.prepare();
                                startClassify();  // 开始分类
                                Looper.loop();
                            }
                        }).start();
                    }else {
                        Toast.makeText(MainActivity.this,"微信尚未安装",Toast.LENGTH_SHORT).show();
                    }

                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void startClassify() {
       for (String info : list){
           sleepRandom();
           setCnt(0);
           gotoSearchUI();

           waitfor(20000);
           if (getCnt() >= 1){
               // 找到输入框并填入手机号
               findEditAndInputInfo(info);
               sleepRandom();
               setCnt(0);
               // 按两下回车确定
               pushEnter();
               waitfor(20000);
               if (getCnt() >= 1 ){
                   int type = 0;
                   // 获取当前界面信息
                    if (hasAddBtn()){  // 界面中有添加按钮,用户存在，添加不频繁。第4 种情况
                        sleepRandom();
                        finishAndReturn();
                        sleepRandom();
                        finishAndReturn();
                    }else if (isFrequent()){  // 操作频繁，用户存在
                        type = 3;
                        sleepRandom();
                        finishAndReturn();
                    }else if (isAbnormal()){  // 用户状态异常
                        type = 2;
                        sleepRandom();
                        finishAndReturn();
                    }else if (isNonexistent()){ // 用户不存在
                        type = 1;
                        sleepRandom();
                        finishAndReturn();
                    }
                   // 判断结果,输出到文本中
                   try {
                       writeToLocal(type,info);
                   } catch (IOException e) {
                       e.printStackTrace();
                   }
               }
           }
       }
    }

    private void writeToLocal(int type,String info) throws IOException {

        //  路径名
        File file = new File("/sdcard/tmp/");
        if (!file.exists()){
            file.mkdirs();  // 创建目录
        }
        File localFile = null;
        if (type == 1){         // 用户不存在
            // 具体文件名 , 路径 + 文件
            localFile = new File(file,"nonexistent.txt");
            if (!localFile.exists()) {
                localFile.createNewFile();
            }
        }else if (type == 2){   // 用户状态异常
            localFile = new File(file,"abnormal.txt");
            if (!localFile.exists()) {
                localFile.createNewFile();
            }
        }else if (type == 3){   // 用户存在，但是频繁
            localFile = new File(file,"frequent.txt");
            if (!localFile.exists()) {
                localFile.createNewFile();
            }
        }else if (type == 4){   // 用户存在，不会频繁，获取信息
            localFile = new File(file,"wxid.txt");
            if (!localFile.exists()) {
                localFile.createNewFile();
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append(info +"\n");
        FileOutputStream fos = new FileOutputStream(localFile,true); // 这里的第二个参数代表追加还是覆盖，true为追加，false为覆盖
        fos.write(sb.toString().getBytes());
        fos.close();

    }


    AccessibilityNodeInfo returnInfo;
    private void finishAndReturn(){

        Log.i("xyz","开始查找返回键");
        // 找到左上角的返回键
        AccessibilityNodeInfo root = getRoot();

        returnInfo = findReturn(root);

        sleepRandom();
        if (returnInfo == null){
            Log.i("xyz","找到的返回为null");
        }else {
            Log.i("xyz","找到的返回不为null");
            while (!returnInfo.isClickable()) {
                returnInfo = returnInfo.getParent();
            }
            // 点击返回
            returnInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    private AccessibilityNodeInfo findReturn(AccessibilityNodeInfo root) {
        if (root == null){
            return null;
        }
        AccessibilityNodeInfo res = null;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = root.getChild(i);
            if (nodeInfo.getClassName().equals("android.widget.ImageView")) {
                Log.i("xyz","获取到ImageView");
                Log.i("xyz","nodeInfo = "+nodeInfo);
                Rect rect = new Rect();
                nodeInfo.getBoundsInScreen(rect);
                int x = rect.centerX();
                int y = rect.centerY();
                if (5 < x && x < 35 && 13 < y && y < 43) {
                    res =  nodeInfo;
                    Log.i("xyz","找到返回键");
                    break; // 这里必须有这个break，表示找到返回键之后就会打破循环，将找到的值返回
                }
            }else {
                res = findReturn(nodeInfo);
                if (res != null){
                    return res;
                }
            }
        }
        return res;
    }

    private boolean isAbnormal() {
        AccessibilityNodeInfo root = getRoot();
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list;
        long aa = System.currentTimeMillis();
        do {
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 5000){
                Log.i("xyz","规定时间内找不到文本信息，直接返回false");
                return false;
            }
            list = root.findAccessibilityNodeInfosByText("异常");
            SystemClock.sleep(200);
        }while (list == null);

        if (list.size() > 0){
            Log.i("xyz","找到异常");
            return true;
        }else {
            return false;
        }
    }

    private boolean isNonexistent() {
        AccessibilityNodeInfo root = getRoot();
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list;
        long aa = System.currentTimeMillis();
        do {
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 5000){
                Log.i("xyz","规定时间内找不到文本信息，直接返回false");
                return false;
            }
            list = root.findAccessibilityNodeInfosByText("该用户不存在");
            SystemClock.sleep(200);
        }while (list == null);

        if (list.size() > 0){
            Log.i("xyz","用户不存在");
            return true;
        }else {
            return false;
        }
    }

    private boolean isFrequent() {
        AccessibilityNodeInfo root = getRoot();
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list;
        long aa = System.currentTimeMillis();
        do {
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 5000){
                Log.i("xyz","规定时间内找不到文本信息，直接返回false");
                return false;
            }
            list = root.findAccessibilityNodeInfosByText("操作过于频繁");
            SystemClock.sleep(200);
        }while (list == null);

        if (list.size() > 0){
            Log.i("xyz","找到操作频繁");
            return true;
        }else {
            return false;
        }
    }


    private boolean hasAddBtn() {
        AccessibilityNodeInfo root = getRoot();
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list;
        long aa = System.currentTimeMillis();
        do {
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 5000){
                Log.i("xyz","规定时间内找不到添加按钮，直接返回false");
                return false;
            }
            list = root.findAccessibilityNodeInfosByText("添加");
            SystemClock.sleep(200);
        }while (list == null);

        if (list.size() > 0){
            Log.i("xyz","找到添加按钮");
            return true;
        }else {
            return false;
        }
    }


    private void findEditAndInputInfo(String info) {
        // 找到界面中的搜索输入框
        AccessibilityNodeInfo root = getRoot();
        do {
            editInfo = findEditText(root);
            SystemClock.sleep(1000);
        }while (editInfo == null);
        // 填入手机号
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        String text = info;
        ClipData data = ClipData.newPlainText("text",text);
        manager.setPrimaryClip(data);
        editInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
    }

    private void pushEnter() {
        // 66 回车
        String adb = "adb shell input keyevent 66";
        for (int i = 0; i < 2; i++){
            try {
                Runtime.getRuntime().exec(adb);
                Log.i("xyz","执行一次回车");
                SystemClock.sleep(300); // 睡眠是为了保证能够两次执行
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



    // 找到输入框
    private AccessibilityNodeInfo findEditText(AccessibilityNodeInfo root) {
        if (root == null){
            return null;
        }
        AccessibilityNodeInfo res = null;

        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = root.getChild(i);
            if (nodeInfo.getClassName().equals("android.widget.EditText")) {
                Log.i("xyz","获取到editteXt");
                Log.i("xyz","nodeInfo = "+nodeInfo);
                Rect rect = new Rect();
                nodeInfo.getBoundsInScreen(rect);
                int x = rect.centerX();
                int y = rect.centerY();
                if (63 < x && x < 365 && 18 < y && y < 38) {
                    res =  nodeInfo;
                    break; // 这里必须有这个break，表示找到输入框之后就会打破循环，将找到的值返回
                }
            }else {
                res = findEditText(nodeInfo);
                if (res != null){
                    return res;
                }
            }
        }
        return res;
    }


    // 每次都等待200ms后获取root根节点信息
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private AccessibilityNodeInfo getRoot() {
        mService = (AccessibilityService) MyService.getContext();
        AccessibilityNodeInfo root;
        do {
            root = mService.getRootInActiveWindow();
            SystemClock.sleep(200);
        }while (root==null);
        return root;
    }


    private void gotoSearchUI(){
        //    com.tencent.mm/.plugin.search.ui.FTSAddFriendUI:
        String s =  "adb shell am start -n com.tencent.mm/com.tencent.mm.plugin.search.ui.FTSAddFriendUI "+
                "";
        try {
            Runtime.getRuntime().exec(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // 线程睡眠，让出cpu
    public void waitfor(long overTime){
        long before = System.currentTimeMillis();
        do{
            long now = System.currentTimeMillis();
            if (now - before >= overTime){
                Log.i("xyz","等待超时");
                return;
            }
            SystemClock.sleep(500);
        }while (getCnt() == 0);
    }

    // 随机睡眠 0.5 到 1.5 秒
    private void sleepRandom(){
        double ran = Math.random();
        long lon = (long) (500 + ran *1000);
        SystemClock.sleep(lon);
    }

    private int getCnt(){
        int i;
        synchronized (MyService.gs_lockObj){
            i = MyService.cnt;
            Log.i(TAG,"get cnt = "+ MyService.cnt);
        }
        return i;
    }

    private void setCnt(int i){
        synchronized (MyService.gs_lockObj){
            MyService.cnt = i;
            Log.i(TAG,"set cnt = "+ MyService.cnt);
        }
    }


    //  是否安装了微信
    private boolean isWxAvailable() {
        PackageManager pm = getPackageManager();
        List<PackageInfo> list = pm.getInstalledPackages(0);
        for (PackageInfo info : list){
            String pkg = info.packageName;
            if (pkg.equals("com.tencent.mm")){
                Log.i(TAG, "isWxAvailable: "+true);
                return true;
            }
        }
        Log.i(TAG, "isWxAvailable: "+false);
        return false;
    }

    private void selectFile() {
        // 打开系统文件浏览功能
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a file path"), SELECT_CODE);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == SELECT_CODE) {
            Uri uri = data.getData();
            if ("file".equalsIgnoreCase(uri.getScheme())) {  // 使用第三方应用打开
                filePath = uri.getPath();
                tvFilePath.setText(filePath);
                Log.i(TAG, "filePath = " + filePath);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {     // 4.4以后
                filePath = getPathKitKat(this, uri);
                tvFilePath.setText(filePath);
                Log.i(TAG, "filePath = " + filePath);
            } else {
                filePath = getPathUnderKitKat(uri);          // 4.4 以前
                tvFilePath.setText(filePath);
                Log.i(TAG, "filePath = " + filePath);
            }


            // 获取txt文本信息
            if (TextUtils.isEmpty(filePath)) {
                Toast.makeText(MainActivity.this, "请选择文件路径", LENGTH_SHORT).show();
            } else {
                getTxtInfo();
            }
        }
    }

    private String getPathUnderKitKat(Uri uri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, proj, null, null, null);
        if (null != cursor && cursor.moveToFirst()) {
            int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(index);
            cursor.close();
        }
        return res;
    }

    // 4.4 以后从uri获取文件的绝对路径
    @SuppressLint("NewApi")
    private String getPathKitKat(Context context, Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // 文件提供者 DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore （general）
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // file文件类型
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    // 返回从uri获取到的数据column ，这种只对MediaStore有效。其他的都是ContentProviders
    private String getDataColumn(Context context, Uri contentUri, String selection, String[] selectArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] proj = {column};
        cursor = context.getContentResolver().query(contentUri, proj, selection, selectArgs, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }


    // 是否是Media路径下的
    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    // 是否是下载路径下的
    private boolean isDownloadDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());

    }

    // 是否是ExternalStorage路径下的
    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }


    // 遍历文本中的每一行，将信息放入list中
    private void getTxtInfo() {
        File file = new File(filePath);
        String line = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
