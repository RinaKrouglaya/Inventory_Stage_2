package com.example.pc.inventory.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentUris;
import android.content.UriMatcher;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;
import java.util.Objects;
import com.example.pc.inventory.data.InventoryContract.InventoryEntry;
import static java.util.Objects.*;

/**
 * {@link ContentProvider} for Inventory app.
 */
public class InventoryProvider extends ContentProvider {

    /** Tag for the log messages */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    private static final int INVENTORY = 100;
    private static final int INVENTORY_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, INVENTORY);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", INVENTORY_ID);
    }

    /** Database helper object */
    private InventoryDbHelper mDbHelper;

    /**
     * Initialize the provider and the database helper object.
     */
    @Override
    public boolean onCreate() {
    mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    /**
     * Perform the query for the given URI. Use the given projection, selection,
     * selection arguments, and sort order.
     */
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        //Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        //This cursor will hold the result of the query
        Cursor cursor;

        //Figure out if the Uri matcher can match the URI to a specific code

        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                /*
                For the INVENTORY code, query the inventory table directly with the given
                projection, selection, selection arguments, and sort order. The cursor
                could contain multiple row of the inventory table.
                */

                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case INVENTORY_ID:
                /*
                For the INVENTORY_ID code  extract out the ID from the URI.
                For an example: URI such as "content://com.example.pc.inventory/inventory/2",
                the selection will be "_id=?" and the selection argument will be a
                String array containing the actual ID of 2 in this case.

                For every "?" in the selection, we need to have an element in the selection
                argument that will filled in the "?". Since we have 1 question mark in the
                selection, we have 1 String in the selection argument String array.
                */
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};

                /*
                This will perform a query on the inventory table where the _id equals 3 to return a
                Cursor containing that row of the table.
                */

                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI" + uri);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            cursor.setNotificationUri(requireNonNull(getContext()).getContentResolver(), uri);
        } else{

            if (getContext() != null) {
                cursor.setNotificationUri(getContext().getContentResolver(), uri);
            }
        }
        return cursor;
    }

    /**
     * Insert new data into the provider with the given ContentValues.
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return insertInventory(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);

        }
    }

    private Uri insertInventory(Uri uri, ContentValues values) {

        String productName = values.getAsString(InventoryEntry.COLUMN_PRODUCT);
        if (productName == null) {
            throw new IllegalArgumentException("Inventory need a name");
        }
        Integer price = values.getAsInteger(InventoryEntry.COLUMN_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("The product needs a price over 0");
        }

        Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("The quantity must be 0 or above");
        }
        String supplierName = values.getAsString(InventoryEntry.COLUMN_SUPPLIER);
        if (supplierName == null) {
            throw new IllegalArgumentException(" A name of the supplier must be filled in");
        }

        Long supplierPhoneNumber = values.getAsLong(InventoryEntry.COLUMN_CONTACT);
        if (supplierPhoneNumber == null)
            throw new IllegalArgumentException(" A valid phone number must be filled in");

        //Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        //Insert a new product with the given values
        long id = database.insert(InventoryEntry.TABLE_NAME, null, values);

        //If the ID is -1 then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        //Notify all listeners that the data has changed for the content URI
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        } else{

            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(uri, null);
            }
        }

        return ContentUris.withAppendedId(uri, id);

    }


    /**
     * Updates the data at the given selection and selection arguments, with the new ContentValues.
     */

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return updateInventory(uri, contentValues, selection, selectionArgs);
            case INVENTORY_ID:
                /*
                For the INVENTORY_ID case, extract out the ID from the URI,
                to know which row to update. Selection will be "_id=?" and selection
                arguments will be String array containing the actual ID.
                */
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateInventory(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update the inventory database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments.
     * Return the number of rows that were successfully updated.
     */
    private int updateInventory(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        /*
        If the {@link InventoryEntry#COLUMN_PRODUCT} key is present,
        check that the name value is not null.
        */
        if (values.containsKey(InventoryEntry.COLUMN_PRODUCT)) {
            String productName = values.getAsString(InventoryEntry.COLUMN_PRODUCT);
            if (productName == null) {
                throw new IllegalArgumentException(" Product requires a name");
            }
        }

        /*
        If the {@link InventoryEntry#COLUMN_PRICE} key is present,
        check that the price value is valid.
        */

        if (values.containsKey(InventoryEntry.COLUMN_PRICE)) {
            Integer price = values.getAsInteger(InventoryEntry.COLUMN_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException("Product requires a valid price either 0 or above");
            }
        }

        /*
        If the {@link InventoryEntry#COLUMN_SUPPLIER} key is present,
        check that the supplier value is valid.
        */
        if (values.containsKey(InventoryEntry.COLUMN_SUPPLIER)) {
            String supplierName = values.getAsString(InventoryEntry.COLUMN_SUPPLIER);
            if (supplierName == null) {
                throw new IllegalArgumentException("Please insert a valid supplier name");
            }
        }

        /*
        If the {@link InventoryEntry#COLUMN_CONTACT key is present,
        check that the phone number value is valid.
        */
        if (values.containsKey(InventoryEntry.COLUMN_CONTACT)) {
            Long supplierPhoneNumber = values.getAsLong(InventoryEntry.COLUMN_CONTACT);
            if (supplierPhoneNumber == null) {
                throw new IllegalArgumentException("Valid phone number required");
            }
            // If there are no values to update, then don't try to update the database.
            if (values.size() == 0) {
                return 0;
            }
        }

        // Otherwise , get writable database to update the data

        SQLiteDatabase database = mDbHelper.getWritableDatabase();
        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(InventoryEntry.TABLE_NAME, values, selection, selectionArgs);

        //If  row/s were updated, then notify all listeners that the data at the
        // given URL has changed
        if (rowsUpdated != 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
            } else {

                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
            }
        }
            // Return the number of rows updated
            return rowsUpdated;

    }

     // Delete the data at the given selection and selection arguments.

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Count the number of rows that were deleted

        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case INVENTORY_ID:
                //Delete a single row given by the ID in the URI
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        /*
        If 1 or more rows were deleted, then notify all listeners that the data at the
        given URI has changed
        */
        if (rowsDeleted != 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Objects.<Context>requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
            } else {

                if (getContext() != null) {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
            }
        }
        return rowsDeleted;
    }


    /**
     * Returns the MIME type of data for the content URI.
     */

    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return InventoryEntry.CONTENT_LIST_TYPE;
            case INVENTORY_ID:
                return InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI" + uri + " with match " + match);
        }

    }
}