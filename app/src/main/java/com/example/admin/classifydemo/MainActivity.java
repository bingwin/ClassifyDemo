package com.example.admin.classifydemo;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
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

import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static android.widget.Toast.LENGTH_SHORT;
import static com.example.admin.classifydemo.Constant.*;
import static com.example.admin.classifydemo.MyService.getContext;


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


    ContentResolver resolver;
    Uri uri = Uri.parse("content://com.example.admin.classifydemo.provider");


    FileOutputStream fos1,fos2,fos3,fos4;

    TextView tvError;

    TextView tvNum; // 当前数
    TextView tvSum; // 总数

    Handler handler;

    TextView tvStartTime;
    TextView tvEndTime;

    int sum;

    private MyDatabaseHelper helper;
    private SQLiteDatabase db;

    Button btnStop;

    static volatile AtomicInteger sAtomicFlag = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSelect = findViewById(R.id.select);
        tvFilePath = findViewById(R.id.tv_hint);

        resolver = this.getContentResolver();


        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });

        Process.myPid();
        Log.i("xyz","MainActivity pid = "+Process.myPid());

        tvError = findViewById(R.id.error);
        tvNum = findViewById(R.id.num);
        tvSum = findViewById(R.id.sum);

        tvStartTime = findViewById(R.id.startTime);
        tvEndTime = findViewById(R.id.endTime);


        helper = DBManager.getInstance(this);


        btnStop = findViewById(R.id.stop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sAtomicFlag.set(1);
            }
        });

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what){
                    case 1: // 错误时的信息和时间
                        String  s = (String) msg.obj;
                        tvError.setText(s);
                        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ");
                        Date curDate = new Date(System.currentTimeMillis());
                        String date = format.format(curDate);
                        tvEndTime.setText(date);
                        break;
                    case 2: // 软件界面
                        int i = msg.arg1;
                        tvNum.setText(""+i);
                        String info = (String) msg.obj;
                        // 吐司
                        Toast.makeText(MainActivity.this,"翻译"+info+"完成，当前已翻译 "+i +"/"+sum,Toast.LENGTH_SHORT).show();
                        if (i == list.size()){
                            Toast.makeText(MainActivity.this,"翻译完成",Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 3: // 正常结束时间
                        String s1 = (String) msg.obj;
                        tvEndTime.setText(s1);
                        break;
                    case 4: // 上次翻译还有未完成的任务
                        Toast.makeText(MainActivity.this,"上次翻译还有未完成的任务,将继续执行任务",Toast.LENGTH_SHORT).show();
                        break;
                }
                super.handleMessage(msg);
            }
        };


        btnStart = findViewById(R.id.start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sAtomicFlag.set(0);
                db = helper.getReadableDatabase();

                // 初始化文件
                try {
                    initFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if(list == null){
                    Toast.makeText(MainActivity.this,"选择文件的列表为空,请检查路径是否正确",Toast.LENGTH_SHORT).show();
                }else {
                    if (isWxAvailable()){
                        sum = list.size();
                        Log.i("mdzz","list = "+ sum);
                        tvSum.setText(""+sum);

                        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ");
                        Date curDate = new Date(System.currentTimeMillis());
                        String date = format.format(curDate);
                        tvStartTime.setText(date);

                        // 一进来就清空表的数据
                        db.execSQL("delete from "+TABLE_NAME);

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Looper.prepare();
                                try {
                                    // 如果数据库中还有status为0的数据，说明上次的数据还处理完成
                                    if (hasStatusZero()){
                                        Message message = new Message();
                                        message.what = 4;
                                        handler.sendMessage(message);
                                        startClassify();  // 开始分类
                                    }else {
                                        // 插入数据
                                        insertData();
                                        startClassify();  // 开始分类
                                    }

                                } catch (MyTimeoutException e) {
                                    e.printStackTrace();
                                    // 更新界面，将错误信息输出到ui界面中

                                    Message message = new Message();
                                    message.what = 1;
                                    message.obj = e.getMessage();
                                    if (sAtomicFlag.get() ==1){
                                        message.obj = "用户点击停止";
                                    }
                                    handler.sendMessage(message);
                                }finally {
                                    db.close();
                                    try {
                                        fos1.close();
                                        fos2.close();
                                        fos3.close();
                                        fos4.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
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

    private void insertData() {
        // 利用事务每次先把数据插入数据库中
        db.beginTransaction();
        for (String info :list){
            String insert = "insert into "+ Constant.TABLE_NAME +" values("+info+",' ',0)";
            Log.i("sql",insert);
            db.execSQL(insert);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
        Log.i("sql","利用事务每次先把数据插入数据库完成");
    }

    private boolean hasStatusZero() {
        // 查出status状态为0的数据(即未处理的)
        String sql = "select * from "+Constant.TABLE_NAME+" where status = 0";
        Cursor cursor = DBManager.queryBySQL(db,sql,null);
        // 传统的做法就是把cursor转换为list，然后在listview中显示，会用到simpleAdapter
        List<Person> persons =  DBManager.cursorToPerson(cursor);
        if (persons.size() > 0){
            return true;
        }else {
            return false;
        }
    }

    private void initFile() throws IOException {
        //  路径名
        File file  = new File("/sdcard/tmp/");
        if (!file.exists()){
            file.mkdirs();  // 创建目录
        }

        File file1 = new File(file,"nonexistent.txt");
        if (!file1.exists()) {
            file1.createNewFile();
        }

        File file2 = new File(file,"abnormal.txt");
        if (!file2.exists()) {
            file2.createNewFile();
        }

        File file3 = new File(file,"frequent.txt");
        if (!file3.exists()) {
            file3.createNewFile();
        }


        File file4 = new File(file,"wxid.txt");
        if (!file4.exists()) {
            file4.createNewFile();
        }
        fos1 = new FileOutputStream(file1,true);// 这里的第二个参数代表追加还是覆盖，true为追加，false为覆盖
        fos2 = new FileOutputStream(file2,true);
        fos3 = new FileOutputStream(file3,true);
        fos4 = new FileOutputStream(file4,true);



    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void startClassify() throws MyTimeoutException {

        // 查出status状态为0的数据(即未处理的)
        String sql = "select * from "+Constant.TABLE_NAME+" where status = 0";
        Cursor cursor = DBManager.queryBySQL(db,sql,null);
        // 传统的做法就是把cursor转换为list，然后在listview中显示，会用到simpleAdapter
        List<Person> persons =  DBManager.cursorToPerson(cursor);
        for (int i = 0 ;i <persons.size(); i++){

            if (sAtomicFlag.get() == 1){
                throw new MyTimeoutException("用户点击停止");
            }

            String info = persons.get(i).getPhone();


            setCnt(0);
            gotoSearchUI();
            sleepRandom();
            waitFor(20000);
            if (getCnt() >= 1){
                Log.i("xyxz","输入手机号界面");
                // 找到输入框并填入手机号
                findEditAndInputInfo(info);
                sleepRandom();

                setCnt(0);
                // 按两下回车确定
                pushEnter();
                waitFor(20000);
                if (getCnt() >= 1 ){
                    Log.i("xyxz","具体界面");
                    int type = 0;
//                   // 获取当前界面信息
//                   if (isFrequent()){  // 操作频繁，用户存在
//                       type = 3;
//                       finishAndReturn();
//                       // 判断结果,输出到文本中
//                       try {
//                           writeToLocal(type,info);
//                       } catch (IOException e) {
//                           e.printStackTrace();
//                       }
//                   }else if (isAbnormal()){  // 用户状态异常
//                       type = 2;
//                       finishAndReturn();
//                       // 判断结果,输出到文本中
//                       try {
//                           writeToLocal(type,info);
//                       } catch (IOException e) {
//                           e.printStackTrace();
//                       }
//                   }else if (isNonexistent()){ // 用户不存在
//                       type = 1;
//                       finishAndReturn();
//                       // 判断结果,输出到文本中
//                       try {
//                           writeToLocal(type,info);
//                       } catch (IOException e) {
//                           e.printStackTrace();
//                       }
                    switch (hasAddBtn()){
                        case 1: // 用户不存在
                            type = 1;
                            if (sAtomicFlag.get() == 1){
                                throw new MyTimeoutException("用户点击停止");
                            }
                            // 判断结果,输出到文本中
                            try {
                                writeToLocal(type,info);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //  更新数据库的数据status
                            updateData(type,info);
                            finishAndReturn();
                            break;

                        case 2:// 用户状态异常
                            type = 2;
                            if (sAtomicFlag.get() == 1){
                                throw new MyTimeoutException("用户点击停止");
                            }
                            // 判断结果,输出到文本中
                            try {
                                writeToLocal(type,info);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            updateData(type,info);
                            finishAndReturn();
                            break;

                        case 3:// 操作频繁，用户存在
                            type = 3;
                            if (sAtomicFlag.get() == 1){
                                throw new MyTimeoutException("用户点击停止");
                            }
                            // 判断结果,输出到文本中
                            try {
                                writeToLocal(type,info);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            updateData(type,info);
                            finishAndReturn();
                            break;

                        case 4:// 界面中有添加按钮,用户存在，添加不频繁。第4 种情况 .
                            Log.i("xyxz","用户信息存在");

                            Cursor cursor1 = resolver.query(uri,null,null,null,null);
                            Log.i("xyzz","activity查询数据");
                            Bundle bundle = cursor1.getExtras();

                            // 等待hook那边创建文件
                            waitForHook(20000);
                            if (getFlag() == 1) {
                                Log.i("xyxz", "进入查询的代码");

                                // 查询数据并更新flag
                                String wxid = bundle.getString("wxid");
                                int sex = bundle.getInt("sex");
                                String nickName = bundle.getString("nickName");
                                String signature = bundle.getString("signature");  //   个性签名
                                String v1 = bundle.getString("v1");               // v1 值
                                String v2 = bundle.getString("v2");             // v2 值

                                // 获取到地区
                                CharSequence area = getArea();;

                                if (sAtomicFlag.get() == 1){
                                    throw new MyTimeoutException("用户点击停止");
                                }
                                try {
                                    writeToLocalScene2(info, wxid, area, sex, nickName, signature, v1, v2);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("wxid",wxid);
                                    jsonObject.put("sex",sex);
                                    jsonObject.put("nickName",nickName);
                                    jsonObject.put("signature",signature);
                                    jsonObject.put("v1",v1);
                                    jsonObject.put("v2",v2);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                String s = jsonObject.toString();
                                updateDataScene2(info,s);

                                // 重新置为0
                                ContentValues values = new ContentValues();
                                values.put("flag", 0);
                                resolver.update(uri, values, null, null);

                                finishAndReturn();
                                sleepRandom();
                                finishAndReturn();
                            }
                            break;

                    }

                }
            }

            Message message = new Message();
            message.what = 2;
            int j = i+1;
            message.arg1 = j;
            message.obj = info;
            handler.sendMessage(message);

        }

        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ");
        Date curDate = new Date(System.currentTimeMillis());
        String date = format.format(curDate);

        Message message = new Message();
        message.what = 3;
        message.obj = date;
        handler.sendMessage(message);

    }


    private int getFlag(){
        Cursor cursor =  resolver.query(uri,null,null,null,null);
        Bundle bundle = cursor.getExtras();
        return bundle.getInt("flag");
    }

    private void waitForHook(long overTime) throws MyTimeoutException {
        long before = System.currentTimeMillis();
        do{
            long now = System.currentTimeMillis();
            if (now - before >= overTime){
                Log.i("xyz","等待超时");
                throw new MyTimeoutException("等待hook方法超时");
            }
            SystemClock.sleep(200);
        }while ( getFlag() == 0 );
    }

    private void updateDataScene2(String phone,String info){
        String s = "update "+TABLE_NAME +" set info = '"+info +"' ,status = 4 where phone = "+phone;
        db.execSQL(s);
    }


    private void writeToLocalScene2(String info, String wxid, CharSequence area, int sex, String nickName,String signature, String v1, String v2) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(info +"-----" + wxid +"-----"+ area +"-----"+ sex +"----"+ nickName+ "----"+ signature +"----"+ v1 +"----"+ v2 +"\n");
        fos4.write(sb.toString().getBytes());

    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private CharSequence getArea() {
        AccessibilityNodeInfo root = getRoot();
        AccessibilityNodeInfo areaInfo = findArea(root);
        if (areaInfo!=null){
            String area = areaInfo.toString();
            Log.i("xyz","获取到地区： "+area);
            return areaInfo.getText();
        }else {
            return " ";
        }
    }

    private AccessibilityNodeInfo findArea(AccessibilityNodeInfo root) {
        if (root == null){
            return null;
        }
        AccessibilityNodeInfo res = null;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = root.getChild(i);
            if (nodeInfo!=null && nodeInfo.getClassName().equals("android.widget.TextView")) {
                Log.i("xyz","获取到TextView");
                Log.i("xyz","nodeInfo = "+nodeInfo);
                Rect rect = new Rect();
                nodeInfo.getBoundsInScreen(rect);
                int x = rect.centerX();
                int y = rect.centerY();
                if (84 < x  && x<186 &&197 < y && y < 215) {
                    res =  nodeInfo;
                    Log.i("xyz","找到地区值");
                    break; // 这里必须有这个break，表示找到返回键之后就会打破循环，将找到的值返回
                }
            }else {
                res = findArea(nodeInfo);
                if (res != null){
                    return res;
                }
            }
        }
        return res;
    }


    private void updateData(int type,String info){
        if (type == 1){         // 用户不存在
//            UPDATE Person SET Address = 'Zhongshan 23', City = 'Nanjing'
//            WHERE LastName = 'Wilson'
            String  s = "update "+TABLE_NAME+" set status = 1 where phone = "+info;
            db.execSQL(s);
        }else if (type == 2){   // 用户状态异常
            String  s = "update "+TABLE_NAME+" set status = 2 where phone = "+info;
            db.execSQL(s);
        }else if (type == 3){   // 用户存在，但是频繁
            String  s = "update "+TABLE_NAME+" set status = 3  where phone = "+info;
            db.execSQL(s);
        }
    }


    private void writeToLocal(int type,String info) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(info +"\n");

        if (type == 1){         // 用户不存在
            fos1.write(sb.toString().getBytes());
        }else if (type == 2){   // 用户状态异常
            fos2.write(sb.toString().getBytes());
        }else if (type == 3){   // 用户存在，但是频繁
            fos3.write(sb.toString().getBytes());
        }
    }


    AccessibilityNodeInfo returnInfo;
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void finishAndReturn(){

        Log.i("xyz","开始查找返回键");
        do {
            // 找到左上角的返回键
            AccessibilityNodeInfo root = getRoot();
            returnInfo = findReturn(root);
            SystemClock.sleep(200);
        }while (returnInfo == null);


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
            if (nodeInfo != null&&nodeInfo.getClassName().equals("android.widget.ImageView") ) {
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isAbnormal() {
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list;
        AccessibilityNodeInfo root = getRoot();
        list = root.findAccessibilityNodeInfosByText("异常");
        if (list.size() > 0){
            Log.i("xyz","找到异常");
            return true;
        }else {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isNonexistent() {
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list;
        AccessibilityNodeInfo root = getRoot();
        list = root.findAccessibilityNodeInfosByText("该用户不存在");
        if (list.size() > 0){
            Log.i("xyz","用户不存在");
            return true;
        }else {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isFrequent() {
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list;
        AccessibilityNodeInfo root = getRoot();
        list = root.findAccessibilityNodeInfosByText("操作过于频繁");
        if (list.size() > 0){
            Log.i("xyz","找到操作频繁");
            return true;
        }else {
            return false;
        }
    }


    //  改了，合并判断，判断是4种情景的哪一个
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private int hasAddBtn() throws MyTimeoutException {
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list;
        long aa = System.currentTimeMillis();
        do {
            AccessibilityNodeInfo root = getRoot();
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 13000){
                Log.e("xyz","sss");
                throw new MyTimeoutException("网络异常,没有获取到具体的场景模式");
            }
            if (isNonexistent()){
                return 1;
            }
            if (isAbnormal()){
                return 2;
            }
            if (isFrequent()){
                return 3;
            }

            list = root.findAccessibilityNodeInfosByText("添加");
            SystemClock.sleep(500);
        }while (list == null || list.size() == 0);

        if (list.size() > 0){
            Log.i("xyz","找到添加按钮");
            return 4;
        }else {
            throw new MyTimeoutException("网络异常,没有获取到具体的场景模式");
        }
    }

    private void pushEnter() {
        // 66 回车
        String adb = "adb shell input keyevent 66";
        for (int i = 0; i < 3; i++){ // 这里三次是为了有时网络卡，确保一定会跳转
            try {
                Runtime.getRuntime().exec(adb);
                Log.i("xyz","执行一次回车");
                SystemClock.sleep(400); // 睡眠是为了保证能够两次执行
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void findEditAndInputInfo(String info) {

        do {
            // 找到界面中的搜索输入框
            AccessibilityNodeInfo root = getRoot();
            editInfo = findEditText(root);
            SystemClock.sleep(200);
        }while (editInfo == null);

        // 填入手机号
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        String text = info;
        ClipData data = ClipData.newPlainText("text",text);
        manager.setPrimaryClip(data);
        editInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
    }

    // 找到输入框
    private AccessibilityNodeInfo findEditText(AccessibilityNodeInfo root) {
        if (root == null){
            return null;
        }
        AccessibilityNodeInfo res = null;

        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo nodeInfo = root.getChild(i);
            if (nodeInfo!=null && nodeInfo.getClassName().equals("android.widget.EditText")) {
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
    @android.support.annotation.RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private AccessibilityNodeInfo getRoot() {
        mService = (AccessibilityService) getContext();
        AccessibilityNodeInfo root;
        do {
            root = mService.getRootInActiveWindow();
            SystemClock.sleep(200);
        }while (root==null);
        root.refresh();
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
    public void waitFor(long overTime) throws MyTimeoutException {
        long before = System.currentTimeMillis();
        do{
            long now = System.currentTimeMillis();
            if (now - before >= overTime){
                Log.i("xyz","等待超时");
                throw new MyTimeoutException("等待辅助类方法超时");
            }
            SystemClock.sleep(300);
        }while (getCnt() == 0);
    }


    // hook类的等待
//    public void waitForHook(long overTime){
//        long before = System.currentTimeMillis();
//        do{
//            long now = System.currentTimeMillis();
//            if (now - before >= overTime){
//                Log.i("xyz","等待超时");
//                return;
//            }
//            SystemClock.sleep(500);
//        }while ();
//    }

//    private boolean isFileCreate() {
//        File local = new File(file,"tmp.txt");
//        if (local.exists()){
//            Log.i("xyz","文件已创建");
//            return true;
//        }else {
//            Log.i("xyz","文件未创建");
//            return false;
//        }
//    }

    // 随机睡眠 1 到 1.5 秒
    private void sleepRandom(){
        double ran = Math.random();
        long lon = (long) (400 + ran *300);
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
        list.clear(); // 每次记录数据数目前先清零
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
