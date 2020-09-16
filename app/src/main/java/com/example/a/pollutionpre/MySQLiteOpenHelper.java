package com.example.a.pollutionpre;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLiteOpenHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "gaspoint.db";
    private static final int VERSION = 1;

    public MySQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public MySQLiteOpenHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //数据库创建
        db.execSQL("create table gaspoints (_id integer primary key autoincrement, " +
                "lat real, lng real, thick integer(20))");

        String stu_sql="insert into gaspoints(lat,lng,thick) values(39.90923,116.447428,10)";
        db.execSQL(stu_sql);
        stu_sql="insert into gaspoints(lat,lng,thick) values(39.901,116.447427,5)";
        db.execSQL(stu_sql);
        stu_sql="insert into gaspoints(lat,lng,thick) values(39.902,116.44728,5)";
        db.execSQL(stu_sql);
        stu_sql="insert into gaspoints(lat,lng,thick) values(39.903,116.44728,5)";
        db.execSQL(stu_sql);
        stu_sql="insert into gaspoints(lat,lng,thick) values(39.904,116.44728,10)";
        db.execSQL(stu_sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //数据库升级
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }

}
