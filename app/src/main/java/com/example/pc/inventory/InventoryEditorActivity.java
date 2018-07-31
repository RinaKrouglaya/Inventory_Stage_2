package com.example.pc.inventory;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.example.pc.inventory.data.InventoryContract;
import com.example.pc.inventory.data.InventoryDbHelper;

/**
 * Allows user to create a new product in the inventory or edit an existing one.
 */
public class InventoryEditorActivity extends AppCompatActivity {


    /**
     * EditText field to enter the name of the product
     */
    private EditText mProductEditText;

    /**
     * EditText field to enter the price of the product
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the quantity of the product
     */
    private EditText mQuantityEditText;

    /**
     * EditText field to enter the supplier of the product
     */
    private EditText mSupplierEditText;

    /**
     * EditText field to enter the phone number of the supplier of the product
     */
    private EditText mContactEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_editor);

        // Find all relevant views that we will need to read user input from
        mProductEditText = (EditText) findViewById(R.id.edit_product_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mSupplierEditText = (EditText) findViewById(R.id.edit_supplier_name);
        mContactEditText = (EditText) findViewById(R.id.edit_contact);
    }

    /**
     * Get user input from editor and save new product into database.
     */
    private void insertProduct() {

        String productString = mProductEditText.getText().toString().trim();

        String priceString = mPriceEditText.getText().toString().trim();
        int priceInt = Integer.parseInt(priceString);

        String quantityString = mQuantityEditText.getText().toString().trim();
        int quantityInt = Integer.parseInt(quantityString);

        String supplierString = mSupplierEditText.getText().toString().trim();

        String contactString = mContactEditText.getText().toString().trim();
        long contactLong = Long.parseLong(contactString);

        InventoryDbHelper mDbHelper = InventoryDbHelper.getInstance(this);

        //Gets the data  repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(InventoryContract.InventoryEntry.COLUMN_PRODUCT, productString);
        values.put(InventoryContract.InventoryEntry.COLUMN_PRICE, priceInt);
        values.put(InventoryContract.InventoryEntry.COLUMN_QUANTITY, quantityInt);
        values.put(InventoryContract.InventoryEntry.COLUMN_SUPPLIER, supplierString);
        values.put(InventoryContract.InventoryEntry.COLUMN_CONTACT, contactLong);

        long newRowId = db.insert(InventoryContract.InventoryEntry.TABLE_NAME, null,
                values);
        //Log.v("CatalogActivity", "New row ID" + newRowId);

        if (newRowId == -1) {
            Toast.makeText(this, getString(R.string.error_insert_product),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.success_insert_product) + newRowId,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_inventory_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product into the database
                insertProduct();
                //Exit InventoryEditorActivity
                finish();
                return true;

            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // Navigate back to parent activity (CatalogActivity)
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}