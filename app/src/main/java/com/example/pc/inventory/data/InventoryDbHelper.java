package com.example.pc.inventory.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.pc.inventory.data.InventoryContract.InventoryEntry;

/**
 * Database helper for Inventory app. Manages database creation and version management.
 */

public class InventoryDbHelper extends SQLiteOpenHelper {

    public static final String LOG_TAG = InventoryDbHelper.class.getSimpleName();
    /**
     * Name of the database file
     */
    private static final String DATABASE_NAME = "inventory.db";
    /**
     * Database version. If you change the database schema, you must increment the database version.
     */
    private static final int DATABASE_VERSION = 1;
    private static InventoryDbHelper mInstance = null;

    /**
     * Constructs a new instance of {@link InventoryDbHelper}.*
     * @param context of the app
     */
   public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the inventory table
        String SQL_CREATE_INVENTORY_TABLE = "CREATE TABLE " + InventoryEntry.TABLE_NAME + " ("
                + InventoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + InventoryEntry.COLUMN_PRODUCT + " TEXT NOT NULL, "
                + InventoryEntry.COLUMN_PRICE + " INTEGER NOT NULL, "
                + InventoryEntry.COLUMN_QUANTITY + " INTEGER NOT NULL DEFAULT 0, "
                + InventoryEntry.COLUMN_SUPPLIER + " TEXT NOT NULL, "
                + InventoryEntry.COLUMN_CONTACT + " LONG NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // The database is still at version 1, so there's nothing to do be done here.
    }

}