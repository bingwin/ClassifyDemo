package com.example.admin.classifydemo;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 2017/11/24.
 */

public class DBManager {

    private static MyDatabaseHelper helper;
    public static MyDatabaseHelper getInstance(Context context){
        if (helper==null){
            helper = new MyDatabaseHelper(context);
        }
        return helper;
    }

    public static Cursor queryBySQL(SQLiteDatabase db, String sql, String []selectionArgs){
        Cursor cursor = null;
        if (db!=null){
            cursor = db.rawQuery(sql,selectionArgs);
        }
        return cursor;
    }

    public static List<Person> cursorToPerson(Cursor cursor){
        List<Person> list = new ArrayList<>();
        while (cursor.moveToNext()){
            String phone = cursor.getString(cursor.getColumnIndex(Constant.PHONE));
            String info = cursor.getString(cursor.getColumnIndex(Constant.INFO));
            int status = cursor.getInt(cursor.getColumnIndex(Constant.STATUS));
            Person person = new Person(phone,info,status);
            list.add(person);
        }
        return list;
    }
}
