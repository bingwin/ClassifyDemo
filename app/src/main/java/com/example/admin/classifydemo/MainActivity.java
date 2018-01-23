package com.example.admin.classifydemo;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
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
import android.os.PersistableBundle;
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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
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

//    private Button btnStart;

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

//    Button btnStop;

//    Button btnContinue;

    static volatile AtomicInteger sAtomicFlag = new AtomicInteger(0);

    WechatServerHelper wechatServerHelper;

//    Button btnService;


    private List<String> mList1 = new ArrayList<>();
    private List<String> mList2 = new ArrayList<>();
    private List<String> mList3 = new ArrayList<>();
    private List<String> mList4Phone = new ArrayList<>();
    private List<JSONObject> mList4Info = new ArrayList<>();

    ClipboardManager manager;


    private static final String SP_WXW = "/data/data/com.guru.Xwx_module";
    private static final String SP_XWX_PATH = "/data/data/com.guru.Xwx_module/shared_prefs/wechatInfo.xml";
    private int userId;

    // TODO spaceID , 过滤为 1 (客户 155)或 0 ，翻译为 -1,
    private static final int spaceId = 1;

    // TODO 翻译模式 1 为开, 0 为关，即过滤
    private static final int translate = 0;

    // TODO 每次获取列表的数量
    private static final int num = 50;

    private TextView tvCurrentPhone;

    private String mTempText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnSelect = findViewById(R.id.select);
        tvFilePath = findViewById(R.id.tv_hint);

        resolver = this.getContentResolver();

        // 获取userid
        initUserId();


        Log.i("xyz","onCreate");
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

        wechatServerHelper = WechatServerHelper.getInstance();


        manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        tvCurrentPhone = findViewById(R.id.currentPhone);


//        btnStop = findViewById(R.id.stop);
//        btnStop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                sAtomicFlag.set(1);
//            }
//        });

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
//                        // 吐司
//                        Toast.makeText(MainActivity.this,"翻译"+info+"完成，当前已翻译 "+i +"/"+sum,Toast.LENGTH_SHORT).show();

                        mTempText = "翻译"+info+"完成，当前已翻译 "+i +"/"+sum;
//                        tvCurrentPhone.setText("翻译"+info+"完成，当前已翻译 "+i +"/"+sum);
                        if (i == list.size()){
//                            Toast.makeText(MainActivity.this,"翻译完成",Toast.LENGTH_SHORT).show();
//                            tvCurrentPhone.setText("翻译完成");
                            mTempText = "翻译完成";
                        }
                        break;
                    case 3: // 正常结束时间
                        String s1 = (String) msg.obj;
                        tvEndTime.setText(s1);
                        break;
                    case 4: // 未完成的任务的数量

                        int j = msg.arg1;
                        tvSum.setText(""+j);
                        break;
                    case 5:
//                        Toast.makeText(MainActivity.this,"list列表为空" ,Toast.LENGTH_SHORT).show();
                        mTempText = "list列表为空";
                        break;

                    case 6:
                        tvError.setText("服务器返回数据列表为空，睡眠一段时间之后将再次重试");
                        SimpleDateFormat f = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ");
                        Date d = new Date(System.currentTimeMillis());
                        String ss = f.format(d);
                        tvEndTime.setText(ss);
                        break;
                    case 7:
//                        Toast.makeText(MainActivity.this,"上次翻译还有未完成的任务,将继续执行任务，可点击继续按钮或者选择新的任务",Toast.LENGTH_SHORT).show();
//                        tvCurrentPhone.setText("上次翻译还有未完成的任务,将继续执行任务，可点击继续按钮或者选择新的任务");
                        mTempText = "上次翻译还有未完成的任务,将继续执行任务，可点击继续按钮或者选择新的任务";
                        break;
                    case 71:
//                        Toast.makeText(MainActivity.this,"上次翻译还有未完成的翻译任务,将自动继续执行任务",Toast.LENGTH_SHORT).show();
//                        tvCurrentPhone.setText("上次翻译还有未完成的翻译任务,将自动继续执行任务");
                        mTempText = "上次翻译还有未完成的翻译任务,将自动继续执行任务";
                        break;
                    case 8:
//                        Toast.makeText(MainActivity.this,"数据库还有未上传到服务器的数据,将自动上传",Toast.LENGTH_SHORT).show();
//                        tvCurrentPhone.setText("数据库还有未上传到服务器的数据,将自动上传");
                        mTempText = "数据库还有未上传到服务器的数据,将自动上传";
                        break;
                    case 9:
//                        Toast.makeText(MainActivity.this,"数据库上传到服务器成功",Toast.LENGTH_SHORT).show();
//                        tvCurrentPhone.setText("数据库上传到服务器成功");
                        mTempText = "数据库上传到服务器成功";
                        tvError.setText("任务完成");
                        break;

                }
                super.handleMessage(msg);
            }
        };
//        btnContinue = findViewById(R.id.jixu);
//        btnContinue.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Looper.prepare();
//                        try {
//                            continueClassify();  // 继续分类
//                        }catch (MyTimeoutException e) {
//                            e.printStackTrace();
//                            Message message = new Message();
//                            message.what = 1;
//                            message.obj = e.getMessage();
//                            if (sAtomicFlag.get() ==1){
//                                message.obj = "用户点击停止";
//                            }
//                            handler.sendMessage(message);
//                            if ( e.m_type == 1){
//                                sleepMinRandom(120,150);
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        } finally {
//                            db.close();
//                        }
//                        Looper.loop();
//                    }
//                }).start();
//            }
//        });

//
//        btnStart = findViewById(R.id.start);
//        btnStart.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                sAtomicFlag.set(0);
//                db = helper.getReadableDatabase();
//
//                // 初始化文件
//                try {
//                    initFile();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                if(list == null){
//                    Toast.makeText(MainActivity.this,"选择文件的列表为空,请检查路径是否正确",Toast.LENGTH_SHORT).show();
//                }else {
//                    if (isWxAvailable()){
//                        sum = list.size();
//                        Log.i("mdzz","list = "+ sum);
//                        tvSum.setText(""+sum);
//
//                        SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ");
//                        Date curDate = new Date(System.currentTimeMillis());
//                        String date = format.format(curDate);
//                        tvStartTime.setText(date);
//
//                        // 一进来就清空表的数据
//                        db.execSQL("delete from "+TABLE_NAME);
//
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Looper.prepare();
//                                try {
//                                    // 插入数据
//                                    insertData();
//                                    startClassify();  // 开始分类
//
//                                } catch (MyTimeoutException e) {
//                                    e.printStackTrace();
//                                    // 更新界面，将错误信息输出到ui界面中
//
//                                    Message message = new Message();
//                                    message.what = 1;
//                                    message.obj = e.getMessage();
//                                    if (sAtomicFlag.get() ==1){
//                                        message.obj = "用户点击停止";
//                                    }
//                                    handler.sendMessage(message);
//
//
//                                    if ( e.m_type == 1){
//                                        sleepMinRandom(120,150);
//                                    }
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                } finally {
//                                    db.close();
//                                    try {
//                                        fos1.close();
//                                        fos2.close();
//                                        fos3.close();
//                                        fos4.close();
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                }
//                                Looper.loop();
//                            }
//                        }).start();
//                    }else {
//                        Toast.makeText(MainActivity.this,"微信尚未安装",Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//        });



//        btnService = findViewById(R.id.serviceBtn);
//        btnService.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                autoDoTask();
//            }
//        });


        Bundle bundle = getIntent().getExtras();
        if (bundle!=null){
            String s = bundle.getString("key");
            if (s.equals("service_start")){
                Log.i("xyz","bundle传入正确");

                SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ");
                Date curDate = new Date(System.currentTimeMillis());
                String date = format.format(curDate);
                tvStartTime.setText(date);

                autoDoTask();
            }
        }

        if(savedInstanceState!=null) {

            SharedPreferences sharedPreferences = this.getSharedPreferences("tmp", Context.MODE_WORLD_READABLE);
            String startTime = sharedPreferences.getString("startTime"," ");
            String endTime = sharedPreferences.getString("endTime"," ");
            String num = sharedPreferences.getString("num"," ");
            String sum = sharedPreferences.getString("sum"," ");
            String error = sharedPreferences.getString("error"," ");


            tvStartTime.setText(startTime);
            tvEndTime.setText(endTime);
            tvNum.setText(num);
            tvSum.setText(sum);
            tvError.setText(error);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvCurrentPhone.setText(mTempText);
    }

    private void initUserId() {
        // 没有root的要先赋予权限
        execCMD("chmod -R 777 " + SP_WXW);
        userId = getUserId();
        Log.i("userId", "initUserId: userId -------- "+ userId );

    }

    private void execCMD(String paramString) {
        try {
            java.lang.Process process = Runtime.getRuntime().exec("su");
            Object object = process.getOutputStream();
            DataOutputStream dos = new DataOutputStream((OutputStream) object);
            String s = String.valueOf(paramString);
            object = s +"\n";
            dos.writeBytes((String) object);
            dos.flush();
            dos.writeBytes("exit\n");
            dos.flush();
            process.waitFor();
            object = process.exitValue();
        } catch (IOException e) {
            Log.i("userId", "execCMD  IOException: "+e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.i("userId", "execCMD  InterruptedException: "+e.getMessage());
            e.printStackTrace();
        }
    }

    private int getUserId() {
        Log.i("userId", "进来了getUserId方法");
        File file = new File(SP_XWX_PATH);
        Log.i("userId", "1");
        Log.i("userId", "SP_XWX_PATH  --- "+  SP_XWX_PATH);
        try {
            FileInputStream fis = new FileInputStream(file);
            // 利用dom4j里面的类
            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(fis);
            Element root = document.getRootElement();
            List<Element> list = root.elements();
            Log.i("userId", "2");
            Log.i("userId", "list ---- "+list);
            for (Element element : list){
                if ("userid".equals(element.attributeValue("name"))){
                    Log.i("userId", "找到userid了  --- "+  element.attributeValue("name"));
                    String currentUin = element.attributeValue("value");

                    Log.i("userId", "它的值currentUin  --- "+currentUin);
                    return Integer.parseInt(currentUin);
                }
            }
        } catch (FileNotFoundException e) {
            Log.i("userId", "FileNotFoundException  e --- "+e.getMessage());
            e.printStackTrace();
        } catch (DocumentException e) {
            Log.i("userId", "DocumentException  e --- "+e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        String startTime = tvStartTime.getText().toString();
        String endTime = tvEndTime.getText().toString();
        String num = tvNum.getText().toString();
        String sum = tvSum.getText().toString();
        String error = tvError.getText().toString();

        Log.i("life","onSaveInstanceState");


        SharedPreferences sharedPreferences  = this.getSharedPreferences("tmp", Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("startTime",startTime);
        editor.putString("endTime",endTime);
        editor.putString("num",num);
        editor.putString("sum",sum);
        editor.putString("error",error);

        editor.commit();


    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.i("life","onRestoreInstanceState");

        SharedPreferences sharedPreferences = this.getSharedPreferences("tmp", Context.MODE_WORLD_READABLE);
        String startTime = sharedPreferences.getString("startTime"," ");
        String endTime = sharedPreferences.getString("endTime"," ");
        String num = sharedPreferences.getString("num"," ");
        String sum = sharedPreferences.getString("sum"," ");
        String error = sharedPreferences.getString("error"," ");


        tvStartTime.setText(startTime);
        tvEndTime.setText(endTime);
        tvNum.setText(num);
        tvSum.setText(sum);
        tvError.setText(error);
    }


    private void autoDoTask() {
        new Thread(new Runnable() {
            @Override
            public void run() {



                while (true) {
                    try {
                        clearAllList();
                        doTask();
//                        sleepMinRandom(1,2);
                        sleepSecondRandom(10,20);
                    } catch (MyTimeoutException e) {
                        e.printStackTrace();
                        Log.e("xyz","捕获到异常");
                        Message message = new Message();
                        message.what = 1;
                        message.obj = e.getMessage();
                        if (sAtomicFlag.get() == 1) {
                            message.obj = "用户点击停止";
                            break;
                        }
                        handler.sendMessage(message);

                        if ( e.m_type == 1){
                            sleepMinRandom(120,150);
                        }
//                        killWechat();
//
//                        sleepMinRandom(1,2);
                        // 下次任务时记得开启微信先
//                        startWechat();
                        // 开启成功，回到doTask
                        while(true){
                            killWechat();
//                            SystemClock.sleep(5000);
                            sleepSecondRandom(5,10);
                            startWechat();
                            sleepSecondRandom(15,20);
//                            SystemClock.sleep(20000);
                            if (startFinish()){
                                break;
                            }
                            sleepRandom();
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (db!=null){
                            db.close();
                            db = null;
                        }
                    }
                }
            }
        }).start();
    }

    private void clearAllList() {
        mList1.clear();
        mList2.clear();
        mList3.clear();
        mList4Phone.clear();
        mList4Info.clear();
    }


    private void killWechat() {
        // 异常时杀死微信，这样就回到主界面了。这样做还有一个好处，保证下次任务开始时微信一定在主界面，从而继续执行任务
        String s = "adb shell am force-stop com.tencent.mm ";
        try {
            Runtime.getRuntime().exec(s);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean startFinish() {
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list = null;
        long aa = System.currentTimeMillis();
        do {
            AccessibilityNodeInfo root = getRoot();
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 8000){
                Log.e("xyz","sss");
                return false;

            }

            if (root!=null) {
                list = root.findAccessibilityNodeInfosByText("微信");
            }
            SystemClock.sleep(500);
        }while (list == null || list.size() == 0);

        if (list!=null && list.size() > 0){
            Log.i("xyz","微信启动完成");
            return true;
        }else {
            return false;
        }
    }

    private void startWechat() {
        Intent intent = new Intent();
        ComponentName cmp=new ComponentName("com.tencent.mm","com.tencent.mm.ui.LauncherUI");
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setComponent(cmp);
        startActivity(intent);
    }


    private void doTask() throws MyTimeoutException, JSONException, IOException {
        // 如果数据库中还有status为0的数据，说明上次的数据还处理完成
        if (hasStatusZero()) { // kg
            handler.sendEmptyMessage(71);
            continueClassify(); //k
        }

        if (hasData()){ // kg
            handler.sendEmptyMessage(8);


            refreshList(1,mList1); //kg
            refreshList(2,mList2);  //kg
            refreshList(3,mList3);  //kg
            refreshList2(mList4Phone,mList4Info);  //kg
            uploadToService(); //kg
        }

        sAtomicFlag.set(0);
        db = helper.getReadableDatabase(); // k

        // 请求数据
        JSONObject phoneObject = null;
        JSONArray array  = null;
        while ( true ) {

            // TODO  后台获取的接口
            if (translate == 1){
                phoneObject = wechatServerHelper.addrlistForTranslateWxid(userId, spaceId, num,1);
            }else {
                phoneObject = wechatServerHelper.addrlistForTranslateWxid(userId, spaceId, num,0);
            }
            if (phoneObject != null){
                array = phoneObject.getJSONArray("phones");
            }


            if (phoneObject != null && array !=null && array.length() > 0 ){
                break;
            }
            // 睡眠一段时间，在主界面中显示
            handler.sendEmptyMessage(6);
            if (array !=null && array.length() == 0){ // 后台返回数据为空
                sleepMinRandom(1,2);
            }else {
                sleepSecondRandom(20,30); // 代码出错
            }

        }

        list.clear();
        if (array!=null && array.length() >0){
            for (int i = 0; i < array.length(); i++) {
                String s = array.getString(i);
                if ( s!=null && !s.equals("")){
                    list.add(array.getString(i));
                }
                Log.i("list data",array.getString(i));
            }
        }



        if (list == null){
            handler.sendEmptyMessage(5);
        }else {
            // 插入数据库
            insertData(); // b kg
            // 翻译
            continueClassify();  // k


            // 上传到服务器
            uploadToService();  // kg

            // 清空数据库中的表数据
            if(!hasData() && !hasStatusZero()){
                db = helper.getReadableDatabase();
                db.execSQL("delete from "+TABLE_NAME);
                db.close();
            }
        }

    }

    private void refreshList2(List<String> mList4Phone,List<JSONObject> mList4Info) throws MyTimeoutException, JSONException {
        db = helper.getReadableDatabase();

        // 查出status状态为未上传的数据
        String sql = "select * from " + Constant.TABLE_NAME + " where status = 4";
        Cursor cursor = DBManager.queryBySQL(db, sql, null);
        // 传统的做法就是把cursor转换为list，然后在listview中显示，会用到simpleAdapter
        List<Person> persons = DBManager.cursorToPerson(cursor);

        mList4Phone.clear();
        mList4Info.clear();
        for (int i = 0; i < persons.size(); i++) {

            if (sAtomicFlag.get() == 1) {
                throw new MyTimeoutException("用户点击停止");
            }
            String phone = persons.get(i).getPhone();
            String info = persons.get(i).getInfo();

            JSONObject object = new JSONObject(info);
            mList4Phone.add(phone);
            mList4Info.add(object);

        }
        if (db!=null){
            db.close();
            db = null;
        }
    }

    private void refreshList(int status,List<String >list) throws MyTimeoutException {
        db = helper.getReadableDatabase();

        // 查出status状态为未上传的数据
        String sql = "select * from " + Constant.TABLE_NAME + " where status = "+status;
        Cursor cursor = DBManager.queryBySQL(db, sql, null);
        // 传统的做法就是把cursor转换为list，然后在listview中显示，会用到simpleAdapter
        List<Person> persons = DBManager.cursorToPerson(cursor);


        list.clear();
        for (int i = 0; i < persons.size(); i++) {

            if (sAtomicFlag.get() == 1) {
                throw new MyTimeoutException("用户点击停止");
            }
            String info = persons.get(i).getPhone();

            list.add(info);

        }
        if (db!=null){
            db.close();
            db = null;
        }

    }

    private boolean hasData() {
        // 查出status状态为1,2,3,4的数据(即未上传的)
        db = helper.getReadableDatabase();
        String sql = "select * from "+Constant.TABLE_NAME+" where status = 1 or status = 2 or status = 3 or status = 4 ";
        Cursor cursor = DBManager.queryBySQL(db,sql,null);
        if(cursor == null){
            if (db!=null){
                db.close();
                db = null;
            }
            return false;
        }else {
            List<Person> persons =  DBManager.cursorToPerson(cursor);
            if (persons.size() > 0){
                if (db!=null){
                    db.close();
                    db = null;
                }
                return true;
            }else {
                if (db!=null){
                    db.close();
                    db = null;
                }
                return false;
            }
        }
    }

    // 睡眠分钟数。区间段
    private void sleepMinRandom(long startMin,long endMin) {
        double ran = Math.random();
        long interval = endMin - startMin;
        long lon = (long) (startMin * 60 * 1000 + ran * 1000 * interval * 60);
        SystemClock.sleep(lon);
    }

    // 睡眠秒数。区间段
    private void sleepSecondRandom(long startSecond,long endSecond) {
        double ran = Math.random();
        long interval = endSecond - startSecond;
        long time = (long) (startSecond  * 1000 + ran * 1000 * interval );
        SystemClock.sleep(time);
    }


    private void uploadToService() throws MyTimeoutException{

        AtomicInteger sAtomicFlag1 = new AtomicInteger(0);

        while (true){

            try {
                sAtomicFlag1.set(0);
                // 上传用户不存在
                uploadToServiceScene1(mList1,3);
            }catch (Exception e){
                sAtomicFlag1.set(1);
                Message msg = new Message();
                msg.what = 1;
                msg.obj = "上传用户不存在失败";
                handler.sendMessage(msg);
//                sleepSecondRandom(10,11);
                sleepMinRandom(1,3);
            }finally {
                if (sAtomicFlag1.get() != 1){
                    break;
                }
            }
        }

        while (true){
            sAtomicFlag1.set(0);
            try {
                // 上传用户异常
                uploadToServiceScene1(mList2,4);
            }catch (Exception e){
                sAtomicFlag1.set(1);
                Message msg = new Message();
                msg.what = 1;
                msg.obj = "上传用户异常失败";
                handler.sendMessage(msg);
                sleepMinRandom(1,3);
            }finally {
                if (sAtomicFlag1.get() != 1){
                    break;
                }
            }
        }

        while (true){
            sAtomicFlag1.set(0);
            try {
                // 上传用户存在（频繁搜索）
                uploadToServiceFrequently(mList3);
            }catch (Exception e){
                sAtomicFlag1.set(1);
                Message msg = new Message();
                msg.what = 1;
                msg.obj = "上传用户存在失败";
                handler.sendMessage(msg);
                sleepMinRandom(1,3);
            }finally {
                if (sAtomicFlag1.get() != 1){
                    break;
                }
            }
        }



        while (true){
            sAtomicFlag1.set(0);
            try {
                // 上传信息
                uploadInfoToService(mList4Phone,mList4Info);
            }catch (Exception e){
                sAtomicFlag1.set(1);
                Message msg = new Message();
                msg.what = 1;
                msg.obj = "上传信息失败";
                handler.sendMessage(msg);
                sleepMinRandom(1,3);
            }finally {
                if (sAtomicFlag1.get() != 1){
                    break;
                }
            }
        }

        handler.sendEmptyMessage(9);




    }

    private void uploadInfoToService(List<String> phoneList, List<JSONObject> infoList) throws JSONException, MyTimeoutException {


        JSONObject infos = new JSONObject();
        for (int i = 0; i < phoneList.size(); i++) {
            JSONObject infoObject = infoList.get(i);  // 详细信息
            String phone = phoneList.get(i);          // 手机号

            infos.put(phone,infoObject);        // 总的json数据
        }

        boolean bb = wechatServerHelper.translateWxidOk(userId,spaceId,infos);

        if (bb){
            Log.i("uploadToService","translateWxidOk 上传到服务器成功");


            try {
                db = helper.getReadableDatabase();
                db.beginTransaction();
                for (String phone: phoneList){
                    String s = "update "+TABLE_NAME +" set status = 14 where phone = '"+phone+"'";
                    db.execSQL(s);
                }
                db.setTransactionSuccessful();
                Log.i("uploadToService","translateWxidOk 数据库更新数据事务成功");
            } catch (Exception e) {
                throw new MyTimeoutException("数据库更新数据事务失败");
            } finally {
                db.endTransaction();
                if (db!=null){
                    db.close();
                    db = null;
                }
            }

            mList4Info.clear();
            mList4Phone.clear();
        }else {
            Log.i("uploadToService","translateWxidOk 上传到服务器失败");
            Message msg = new Message();
            msg.what = 1;
            msg.obj = "上传用户信息失败";
            handler.sendMessage(msg);
            throw new MyTimeoutException("上传用户信息失败");
        }



    }


    private void uploadToServiceFrequently(List<String> list) throws JSONException, MyTimeoutException {
        JSONArray jsonArray = new JSONArray();
        for (String phone:list){
            jsonArray.put(phone);
        }

        JSONObject object = wechatServerHelper.phoneExistenceBat(userId,spaceId,jsonArray);

        Log.i("post","手机号存在时的返回 JSONObject = "+object);

        if (object==null){
            Log.i("uploadToService","phoneExistenceBat 上传到服务器失败");
            Message msg = new Message();
            msg.what = 1;
            msg.obj = "上传用户存在失败";
            handler.sendMessage(msg);
            throw new MyTimeoutException("上传用户存在失败");
        }
        int i = object.getInt("result");

        if (i == 1){
            Log.i("uploadToService","phoneExistenceBat 上传到服务器成功");

            try {
                db = helper.getReadableDatabase();
                db.beginTransaction();
                for (String phone: list){
                    String s = "update "+TABLE_NAME +" set status = 13 where phone = '"+phone+"'";
                    db.execSQL(s);
                }
                db.setTransactionSuccessful();
                Log.i("uploadToService","phoneExistenceBat 数据库更新数据事务成功");
            } catch (Exception e) {
                throw new MyTimeoutException("数据库更新数据事务失败");
            } finally {
                db.endTransaction();
                if (db!=null){
                    db.close();
                    db = null;
                }
            }

            mList3.clear();
        }else if (i == 0){
            Log.i("uploadToService","phoneExistenceBat 上传到服务器失败");
            Message msg = new Message();
            msg.what = 1;
            msg.obj = "上传用户存在失败";
            handler.sendMessage(msg);
            throw new MyTimeoutException("上传用户存在失败");
        }
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void continueClassify() throws MyTimeoutException, IOException {

        db = helper.getReadableDatabase();

        // 查出status状态为0的数据(即未处理的)
        String sql = "select * from " + Constant.TABLE_NAME + " where status = 0";
        Cursor cursor = DBManager.queryBySQL(db, sql, null);
        // 传统的做法就是把cursor转换为list，然后在listview中显示，会用到simpleAdapter
        List<Person> persons = DBManager.cursorToPerson(cursor);
        Log.i("xyz","persons = "+persons.toString());
        sum = persons.size();
        Message msg = new Message();
        msg.what = 4;
        msg.arg1 = sum;
        handler.sendMessage(msg);

        for (int i = 0; i < persons.size(); i++) {

            if (sAtomicFlag.get() == 1) {
                throw new MyTimeoutException("用户点击停止");
            }

            String info = persons.get(i).getPhone();


            setCnt(0);
            gotoSearchUI();
            sleepRandom();
            waitFor(20001);
            if (getCnt() >= 1) {
                Log.i("xyxz", "输入手机号界面");
                // 找到输入框并填入手机号
                findEditAndInputInfo(info);
                sleepRandom();
                setCnt(0);
                // 按两下回车确定
                pushEnter2();
                sleepRandom();
                waitFor(20002);
                if (getCnt() >= 1) {
                    Log.i("xyxz", "具体界面");
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
                    switch (hasAddBtn()) {
                        case 1: // 用户不存在
                            type = 1;
                            if (sAtomicFlag.get() == 1) {
                                throw new MyTimeoutException("用户点击停止");
                            }
                            //  更新数据库的数据status
                            updateData(type, info);
                            finishAndReturn();

                            mList1.add(info);

                            break;

                        case 2:// 用户状态异常
                            type = 2;
                            if (sAtomicFlag.get() == 1) {
                                throw new MyTimeoutException("用户点击停止");
                            }
                            updateData(type, info);
                            finishAndReturn();


                            mList2.add(info);
                            break;

                        case 3:// 操作频繁，用户存在

                            if (translate == 1){
                                // jia toast
                                finishAndReturn();
                                throw new MyTimeoutException("当前为翻译模式，操作已频繁,将睡眠一段时间再重试",1);
                            }else {
                                type = 3;
                                if (sAtomicFlag.get() == 1) {
                                    throw new MyTimeoutException("用户点击停止");
                                }

                                updateData(type, info);
                                finishAndReturn();

                                mList3.add(info);
//                            JSONArray jsonArray = new JSONArray();
//                            try {
//                                jsonArray.put(info);
//                                wechatServerHelper.phoneExistenceBat(1,1,jsonArray);
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }
                                break;
                            }


                        case 4:// 界面中有添加按钮,用户存在，添加不频繁。第4 种情况 .
                            Log.i("xyxz", "用户信息存在");



                            // 等待hook那边创建文件
                            waitForHook(20000);

                            Cursor cursor1 = resolver.query(uri, null, null, null, null);
                            Log.i("xyzz", "activity查询数据");
                            Bundle bundle = cursor1.getExtras();


                            if (getFlag() == 1) {
                                Log.i("xyxz", "进入查询的代码");

                                // 查询数据并更新flag
                                String wxid = bundle.getString("wxid");
                                int sex = bundle.getInt("sex");
                                String nickName = bundle.getString("nick");
                                String signature = bundle.getString("signature");  //   个性签名
                                String v1 = bundle.getString("v1data");               // v1 值
                                String v2 = bundle.getString("v2data");             // v2 值

                                // 获取到地区
                                CharSequence area = getArea();

                                if (sAtomicFlag.get() == 1) {
                                    throw new MyTimeoutException("用户点击停止");
                                }

                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("wxid", wxid);
                                    jsonObject.put("sex", sex);
                                    jsonObject.put("nick", nickName);
                                    jsonObject.put("signature", signature);
                                    jsonObject.put("v1data", v1);
                                    jsonObject.put("v2data", v2);
                                    jsonObject.put("area", area);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                String s = jsonObject.toString();


//                                try {
//                                    JSONObject phone = new JSONObject();
//                                    phone.put("wxid",wxid);
//                                    phone.put("sex",sex);
//                                    phone.put("nick",nickName);
//                                    phone.put("signature",signature);
//                                    phone.put("v1",v1);
//                                    phone.put("v2",v2);
//                                    phone.put("area", area);
//
//                                    JSONObject phones = new JSONObject();
//                                    phones.put(info,phone.toString());
//                                    boolean bb = wechatServerHelper.translateWxidOk(1,1,phones);
//                                    Log.i("uploadToService","反馈wxid "+phones+" ："+bb);
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }



                                JSONObject details = new JSONObject();
                                try {
                                    details.put("wxid",wxid);
                                    details.put("sex",sex);
                                    details.put("nick",nickName);
                                    details.put("signature",signature);
                                    details.put("v1data",v1);
                                    details.put("v2data",v2);
                                    details.put("area", area);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                mList4Phone.add(info);
                                mList4Info.add(details);

                                // 重新置为0
                                ContentValues values = new ContentValues();
                                values.put("flag", 0);
                                resolver.update(uri, values, null, null);

                                updateDataScene2(info, s);


                                finishAndReturn();
//                                sleepRandom();
                                finishAndReturn();
                            }
                            break;

                    }

                }
            }

            Message message = new Message();
            message.what = 2;
            int j = i + 1;
            message.arg1 = j;
            message.obj = info;
            handler.sendMessage(message);
        }
    }

    private void insertData() {
        // 利用事务每次先把数据插入数据库中

        db.beginTransaction();
        try {
            for (String info :list){
                String insert = "insert into "+ Constant.TABLE_NAME +" values('"+info+"',' ',0)";
                Log.i("sql",insert);
                db.execSQL(insert);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        Log.i("sql","利用事务每次先把数据插入数据库完成");
    }

    private boolean hasStatusZero() {
        // 查出status状态为0的数据(即未处理的)
        db = helper.getReadableDatabase();
        String sql = "select * from "+Constant.TABLE_NAME+" where status = 0";
        Cursor cursor = DBManager.queryBySQL(db,sql,null);
        if(cursor == null){
            if (db!=null){
                db.close();
                db = null;
            }
            return false;
        }else {
            List<Person> persons =  DBManager.cursorToPerson(cursor);
            if (persons.size() > 0){
                if (db!=null){
                    db.close();
                    db = null;
                }
                return true;
            }else {
                if (db!=null){
                    db.close();
                    db = null;
                }
                return false;
            }
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
    private void startClassify() throws MyTimeoutException, IOException {

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
//            sleepRandom();
            waitFor(20000);
            if (getCnt() >= 1){
                Log.i("xyxz","输入手机号界面");
                // 找到输入框并填入手机号
                findEditAndInputInfo(info);
//                sleepRandom();

                setCnt(0);
                // 按两下回车确定
                pushEnter2();
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


                            //judgetype
//                            1:表示 通过通讯录数据库,"请求状态"超过5天,还没有转换成"发出添加好友状态" 的方式进行判断的
//                            2:表示 通过软件判断没有开通了微信号的（v1.1新增）
//                            3:表示 通过手机号搜索判断的 结果为"用户不存在"
//                            4:表示 通过手机号搜索判断的 结果为"用户状态异常" ,表示该手机号用户可能开通了微信号，然后被封了

                            // 上传到后台
                          //  uploadToService(info,3);


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


                            // 上传到后台
                       //     uploadToService(info,4);
                            break;

                        case 3:// 操作频繁，用户存在

                            if (translate == 1){
                                finishAndReturn();
                                throw new MyTimeoutException("当前为翻译模式，操作已频繁,将睡眠一段时间再重试",1);
                            }else {
                                type = 3;
                                if (sAtomicFlag.get() == 1) {
                                    throw new MyTimeoutException("用户点击停止");
                                }
                                // 判断结果,输出到文本中
                                try {
                                    writeToLocal(type, info);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                updateData(type, info);
                                finishAndReturn();


//                            JSONArray jsonArray = new JSONArray();
//                            try {
//                                jsonArray.put(info);
//                                wechatServerHelper.phoneExistenceBat(userId,0,jsonArray);
//                            } catch (JSONException e) {
//                                e.printStackTrace();
//                            }

                                break;
                            }

                        case 4:// 界面中有添加按钮,用户存在，添加不频繁。第4 种情况 .
                            Log.i("xyxz","用户信息存在");

                            // 等待hook那边创建文件
                            waitForHook(20000);

                            Cursor cursor1 = resolver.query(uri,null,null,null,null);
                            Log.i("xyzz","activity查询数据");
                            Bundle bundle = cursor1.getExtras();

                            if (getFlag() == 1) {
                                Log.i("xyxz", "进入查询的代码");

                                // 查询数据并更新flag
                                String wxid = bundle.getString("wxid");
                                int sex = bundle.getInt("sex");
                                String nickName = bundle.getString("nick");
                                String signature = bundle.getString("signature");  //   个性签名
                                String v1 = bundle.getString("v1data");               // v1 值
                                String v2 = bundle.getString("v2data");             // v2 值

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
                                    jsonObject.put("nick",nickName);
                                    jsonObject.put("signature",signature);
                                    jsonObject.put("v1data",v1);
                                    jsonObject.put("v2data",v2);
                                    jsonObject.put("area",area);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                String s = jsonObject.toString();
                                updateDataScene2(info,s);

                                try {
                                    JSONObject phone = new JSONObject();
                                    phone.put("wxid",wxid);
                                    phone.put("sex",sex);
                                    phone.put("nick",nickName);
                                    phone.put("signature",signature);
                                    phone.put("v1data",v1);
                                    phone.put("v2data",v2);
                                    phone.put("area",area);

                                    JSONObject phones = new JSONObject();
                                    phones.put(info,phone.toString());
//                                    boolean bb = wechatServerHelper.translateWxidOk(userId,0,phones);
//                                    Log.i("uploadToService","反馈wxid "+phones+" ："+bb);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                // 重新置为0
                                ContentValues values = new ContentValues();
                                values.put("flag", 0);
                                resolver.update(uri, values, null, null);

                                finishAndReturn();
//                                sleepRandom();
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

    private void uploadToServiceScene1(List<String> list, int i) throws JSONException, MyTimeoutException {
        int j = 0;
        if (i == 3){
            j = 11;
        }else if (i == 4){
            j = 12;
        }
        JSONArray array = new JSONArray();
        for (String phone : list){
            JSONObject object = new JSONObject();
            object.put(phone,i);
            Log.i("uploadToService","用户的 object = "+object.toString());
            array.put(object);
        }

        boolean zzz = wechatServerHelper.uploadPhoneBlacklistBySpaceid(userId,spaceId,array);
        Log.i("uploadToService","上传到服务器uploadPhoneBlacklistBySpaceid    : "+ zzz);

        if (zzz){
            Log.i("uploadToService","uploadPhoneBlacklistBySpaceid 上传到服务器成功");

            try {
                db = helper.getReadableDatabase();
                db.beginTransaction();
                for (String phone: list){
                    String s = "update "+TABLE_NAME +" set status = "+ j+" where phone = '"+phone+"'";
                    db.execSQL(s);
                }
                db.setTransactionSuccessful();

                Log.i("uploadToService","uploadPhoneBlacklistBySpaceid 数据库更新数据事务成功");
            } catch (Exception e) {
                throw new MyTimeoutException("数据库更新数据事务失败");
            } finally {
                db.endTransaction();
                if (db!=null){
                    db.close();
                    db = null;
                }
            }

            if (i == 3){
                mList1.clear();
            }else if (i ==4 ){
                mList2.clear();
            }
        }else {
            Log.i("uploadToService","uploadPhoneBlacklistBySpaceid 上传到服务器失败");
            Message msg = new Message();
            msg.what = 1;
            msg.obj = "上传用户不存在/异常失败";
            handler.sendMessage(msg);
            throw new MyTimeoutException("上传用户不存在/异常失败");
        }

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
        String s = "update "+TABLE_NAME +" set info = '"+info +"' ,status = 4 where phone = '"+phone+"'";
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
        AccessibilityNodeInfo areaInfo = null;
        if (root!=null){
             areaInfo = findArea(root);
        }
        if (areaInfo!=null){
            String area = areaInfo.toString();
            Log.i("xyz","获取到地区： "+area);
            return areaInfo.getText();
        }else {
            return " ";
        }
    }

    private AccessibilityNodeInfo findArea(AccessibilityNodeInfo root) {

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
        db = helper.getReadableDatabase();
        if (type == 1){         // 用户不存在
//            UPDATE Person SET Address = 'Zhongshan 23', City = 'Nanjing'
//            WHERE LastName = 'Wilson'
            String  s = "update "+TABLE_NAME+" set status = 1 where phone = '"+info+"'";
            db.execSQL(s);
        }else if (type == 2){   // 用户状态异常
            String  s = "update "+TABLE_NAME+" set status = 2 where phone = '"+info+"'";
            db.execSQL(s);
        }else if (type == 3){   // 用户存在，但是频繁
            String  s = "update "+TABLE_NAME+" set status = 3 where phone = '"+info+"'";
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
            if (root!=null){
                returnInfo = findReturn(root);
            }
            SystemClock.sleep(200);
        }while (returnInfo == null);


        if (returnInfo == null){
            Log.i("xyz","找到的返回为null");
        }else {
            Log.i("xyz","找到的返回不为null");
            while (returnInfo!=null && !returnInfo.isClickable()) {
                returnInfo = returnInfo.getParent();
            }
            if (returnInfo!=null){
                // 点击返回
                returnInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
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
                Log.i("fanhui","获取到ImageView");
                Log.i("fanhui","nodeInfo = "+nodeInfo);
                Rect rect = new Rect();
                nodeInfo.getBoundsInScreen(rect);
                int x = rect.centerX();
                int y = rect.centerY();
                Log.i("fanhui","x = "+ x+ " y = "+y);
                if (5 < x && x < 35 && 13 < y && y < 43) {
                    res =  nodeInfo;
                    Log.i("fanhui","找到返回键");
                    Log.i("fanhui","找到返回键的坐标 x = "+ x+ " y = "+y);
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
        List<AccessibilityNodeInfo> list = null;
        AccessibilityNodeInfo root = getRoot();
        if (root!=null){
            list = root.findAccessibilityNodeInfosByText("异常");
        }
        if (list!=null && list.size() > 0){
            Log.i("xyz","找到异常");
            return true;
        }else {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isNonexistent() {
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list = null;
        AccessibilityNodeInfo root = getRoot();
        if (root!=null){
            list = root.findAccessibilityNodeInfosByText("该用户不存在");
        }

        if (list!=null && list.size() > 0){
            Log.i("xyz","用户不存在");
            return true;
        }else {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean isFrequent() {
        // 获取到添加按钮
        List<AccessibilityNodeInfo> list = null;
        AccessibilityNodeInfo root = getRoot();
        if (root!=null){
            list = root.findAccessibilityNodeInfosByText("操作过于频繁");
        }
        if (list!=null && list.size() > 0){
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
        List<AccessibilityNodeInfo> list = null;
        long aa = System.currentTimeMillis();
        do {
            AccessibilityNodeInfo root = getRoot();
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 8000){
                Log.e("xyz","sss99");
                throw new MyTimeoutException("网络异常,没有获取到具体的场景模式99");
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

            if (root!=null){
                list = root.findAccessibilityNodeInfosByText("设置备注");
            }
            SystemClock.sleep(500);
        }while (list == null || list.size() == 0);

        if (list.size() > 0){
            Log.i("xyz","找到设置备住");
            return 4;
        }else {
            throw new MyTimeoutException("网络异常,没有获取到具体的场景模式");
        }
    }

//    private void pushEnter() {
//        // 66 回车
//        String adb = "adb shell input keyevent 66";
//        for (int i = 0; i < 5; i++){ // 这里三次是为了有时网络卡，确保一定会跳转
//            try {
//                Runtime.getRuntime().exec(adb);
//                Log.i("xyz","执行一次回车");
//                SystemClock.sleep(200); // 睡眠是为了保证能够两次执行
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    private void pushEnter2() throws IOException, MyTimeoutException {
        int errorNum = 0;
        while (true){
            String adb = "adb shell input keyevent 66";
            for (int i = 0; i < 2; i++){ // 这里三次是为了有时网络卡，确保一定会跳转
                Runtime.getRuntime().exec(adb);
                SystemClock.sleep(200); // 睡眠是为了保证能够两次执行
            }
            try{
                waitFor(3000);
                break;
            } catch (MyTimeoutException e) {
                e.printStackTrace();
                errorNum ++;
                if (errorNum > 7){
                    throw e;
                }
            }
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void findEditAndInputInfo(String info) throws MyTimeoutException {

        do {
            // 找到界面中的搜索输入框
            AccessibilityNodeInfo root = getRoot();
            if (root!=null){
                editInfo = findEditText(root);
            }
            SystemClock.sleep(200);
        }while (editInfo == null);


        long aa = System.currentTimeMillis();
        do {
            long bb =  System.currentTimeMillis();
            if (bb - aa >= 8000) {
                Log.e("xyz", "sss00");
                throw new MyTimeoutException("输入手机号失败");
            }
            // 填入手机号
            String text = info;
            ClipData data = ClipData.newPlainText("text",text);
            if (manager == null){
                Log.i("xyz","manager == null");
            }
            if (data == null){
                Log.i("xyz","data == null");
            }
            if (manager!=null && data!=null){
                manager.setPrimaryClip(data);
                editInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
            sleepRandom();
        }while (editInfo.getText() == null);



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

                throw new MyTimeoutException("等待辅助类方法超时 "+overTime);
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

    // 随机睡眠 0.2 到 0.4 秒
    private void sleepRandom(){
        double ran = Math.random();
        long lon = (long) (200 + ran *200);
        SystemClock.sleep(lon);
    }

    private int getCnt(){
        int i;
        i = MyService.cnt.get();
        Log.i(TAG,"get cnt = "+ MyService.cnt);
        return i;
    }

    private void setCnt(int i){
        MyService.cnt.set(i);
        Log.i(TAG,"set cnt = "+ MyService.cnt);

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
