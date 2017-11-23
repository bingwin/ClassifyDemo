package com.example.admin.classifydemo;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


/**
 * Created by admin on 2017/11/21.
 */

public class MyProvider extends ContentProvider {


    @Override
    public boolean onCreate() {

        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("tmp", Context.MODE_WORLD_READABLE);
        String wxid = sharedPreferences.getString("wxid"," ");
        String nickName = sharedPreferences.getString("nickName"," ");
        int sex = sharedPreferences.getInt("sex",-1);
        int flag = sharedPreferences.getInt("flag",-1);
        String signature = sharedPreferences.getString("signature"," ");
        String v1 = sharedPreferences.getString("v1"," ");
        String v2 = sharedPreferences.getString("v2"," ");


        Bundle extras = new Bundle();
        extras.putString("wxid",wxid);
        extras.putString("nickName",nickName);
        extras.putInt("sex",sex);
        extras.putInt("flag",flag);
        extras.putString("signature",signature);
        extras.putString("v1",v1);
        extras.putString("v2",v2);


        MyCursor cursor = new MyCursor();
        cursor.setExtras(extras);
        Log.i("xyzz","contentprovider查询数据");

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        // 通过ContentProvider插入数据到SharedPreferences

        String wxid =  values.getAsString("wxid");
        int sex = values.getAsInteger("sex");
        String nickName = values.getAsString("nickName");
        int flag = values.getAsInteger("flag");
        String signature = values.getAsString("signature");  //   个性签名
        String v1 = values.getAsString("v1");               // v1 值
        String v2 = values.getAsString("v2");             // v2 值


        SharedPreferences sharedPreferences  = getContext().getSharedPreferences("tmp", Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("wxid",wxid);
        editor.putInt("sex",sex);
        editor.putString("nickName",nickName);
        editor.putInt("flag",flag);
        editor.putString("signature",signature);
        editor.putString("v1",v1);
        editor.putString("v2",v2);

        editor.commit();
        Log.i("xyzz","contentprovider插入数据");
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        int flag = values.getAsInteger("flag");
        SharedPreferences sharedPreferences  = getContext().getSharedPreferences("tmp", Context.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("flag",flag);
        editor.commit();
        Log.i("xyzz","contentprovider更新数据");
        return 0;
    }
}
