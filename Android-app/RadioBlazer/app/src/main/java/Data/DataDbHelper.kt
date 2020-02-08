package Data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper

class DataDbHelper   (context: Context): SQLiteOpenHelper(context, DATABASE_NAME,  null, DATABASE_VERSION){


    private val db: SQLiteDatabase
    private val values: ContentValues
    companion object{
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "memoryFrequency"
    }

    init {
        db = this.writableDatabase
        values = ContentValues()
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL("CREATE TABLE "+Tables.memoryFrequency.TABLE_NAME + " (" +
            Tables.memoryFrequency._ID + " INTEGER PRIMARY KEY," +
            Tables.memoryFrequency.COLUMN_FREQUENCYVALUE + " TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }

    fun insert(_id: Int, frequency: String){
        values.put(Tables.memoryFrequency._ID, _id)
        values.put(Tables.memoryFrequency.COLUMN_FREQUENCYVALUE, frequency)
        db.insert(Tables.memoryFrequency.TABLE_NAME, null, values)
    }

    fun getData(_id: Int): String{
        var cursor: Cursor? = null
        var freq: String = ""
        try {
            cursor = db.rawQuery("SELECT * FROM " + Tables.memoryFrequency.TABLE_NAME + " WHERE " + Tables.memoryFrequency._ID + " = " + _id, null)
        }catch (e: SQLiteException){

        }
        if(cursor!!.moveToFirst()){
            while (cursor.isAfterLast == false) {
                freq = cursor.getString(cursor.getColumnIndex(Tables.memoryFrequency.COLUMN_FREQUENCYVALUE))
            }
        }
        return freq
    }
}