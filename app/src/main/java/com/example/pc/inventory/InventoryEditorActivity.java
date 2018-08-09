package com.example.pc.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.pc.inventory.data.InventoryContract;
import com.example.pc.inventory.data.InventoryContract.InventoryEntry;
import com.example.pc.inventory.data.InventoryDbHelper;

import static com.example.pc.inventory.data.InventoryContract.*;

/**
 * Allows user to create a new product in the inventory or edit an existing one.
 */
public class InventoryEditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the inventory data loader
     */
    private static final int EXISTING_INVENTORY_LOADER = 0;


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


    /**
     * Content URI for the existing product(null if its a new product)
     */

    private Uri mCurrentProductUri;

    /**
     * Boolean flag : edited (true) or not (false)
     */

    private boolean mInventoryHasChanged = false;

    /**
     * Quantity given by the user
     */
    private int givenQuantityInt;

    //OnTouchListener that listens for any user touches on a View( modifying
    // the view), and so it changes the mInventoryHasChanged boolean to true.
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mInventoryHasChanged = true;
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_editor);

        /*
        Examine the intent that was used to launch this activity,
        in order to figure out if we are creating a new product or editing an existing one.
        */
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.editor_activity_title_new_product));

            //Invalidate the options menu, so the "Delete" menu option can be hidden.
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_existing_product));
            getLoaderManager().initLoader(EXISTING_INVENTORY_LOADER, null, this);
        }

        /*
        Find all relevant views that we will need to read user input from
        */

        mProductEditText = findViewById(R.id.edit_product_name);
        mPriceEditText = findViewById(R.id.edit_price);
        mQuantityEditText =  findViewById(R.id.edit_quantity);
        mSupplierEditText =  findViewById(R.id.edit_supplier_name);
        mContactEditText =  findViewById(R.id.edit_contact);

        ImageButton mIncrease = findViewById(R.id.edit_quantity_increase);
        ImageButton mDecrease = findViewById(R.id.edit_quantity_decrease);

        mProductEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mContactEditText.setOnTouchListener(mTouchListener);
        mIncrease.setOnTouchListener(mTouchListener);
        mDecrease.setOnTouchListener(mTouchListener);

        if (TextUtils.isEmpty( mQuantityEditText.getText().toString().trim())) {
            mQuantityEditText.setText("0");

        }
        // Increase quantity with button
        mIncrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String quantityString = mQuantityEditText.getText().toString().trim();
                   givenQuantityInt = Integer.parseInt(quantityString);
                    mQuantityEditText.setText(String.valueOf(givenQuantityInt + 1));
                }

        });

        // Decrease quantity

        mDecrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String quantityString = mQuantityEditText.getText().toString().trim();
                       givenQuantityInt = Integer.parseInt(quantityString);
                    // to validate if quantity is greater than =
                    if ((givenQuantityInt - 1) >= 0) {
                        mQuantityEditText.setText(String.valueOf(givenQuantityInt - 1));
                    } else {
                        Toast.makeText(InventoryEditorActivity.this,
                        R.string.editor_quantity_cannot_be_less_than_0, Toast.LENGTH_SHORT).show();
                    }
                }

        });

        //Setting up the phone button to call the supplier
        final ImageButton mPhoneCallSupplierButton = findViewById(R.id.call_supplier_phone_button);
        if (mCurrentProductUri==null){
            //Hide button if we don't have a phone number in the database yet
            mPhoneCallSupplierButton.setVisibility(View.GONE);
        }else {
            //Show the button as we if we do have a phone number in the database
            mPhoneCallSupplierButton.setVisibility(View.VISIBLE);
        }

        mPhoneCallSupplierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = mContactEditText.getText().toString().trim();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:"+phoneNumber));
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);

                }
            }

        });
    }

    @Override
    public void onBackPressed() {
        // If the inventory editing hasn't changed, continue with handling back button press
        if (!mInventoryHasChanged) {
            super.onBackPressed();
            return;
        }
        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //User clicked "Discard" button, close the current activity-
                        finish();

                    }
                };
        //Show the dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    private void saveProduct() {

        String productString = mProductEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        String contactString = mContactEditText.getText().toString().trim();

        /*
        Check if it supposed to be a new product
        and check if all the fields in the editor are blank
        */

        if ((mCurrentProductUri == null )&&
                TextUtils.isEmpty(productString) && TextUtils.isEmpty(priceString)
                && TextUtils.isEmpty(supplierString) &&
                TextUtils.isEmpty(contactString))
        {
          Toast.makeText(this, getString(R.string.editor_fill_in), Toast.LENGTH_LONG).show();
            /*
            Since no fields were modified, we can return early without creating a new  product.
            No need to create ContentValues and no need to do any InventoryProvider operations.
            */
            return;
        }
        if (TextUtils.isEmpty(productString)) {
            mProductEditText.setError(getString(R.string.editor_question_empty_field_name));
            return;
        }
        if (TextUtils.isEmpty(priceString)) {
            mPriceEditText.setError(getString(R.string.editor_question_empty_field_price));
            return;
        }


        if (TextUtils.isEmpty(supplierString)) {
            mSupplierEditText.setError(getString(R.string.editor_question_empty_field_supplier_name));
            return;
        }
        if (TextUtils.isEmpty(contactString)) {
            mContactEditText.setError(getString(R.string.editor_question_empty_field_contact));
            return;
        }

        /*
        Create a ContentValues object where column name are the keys,
        and inventory attributes from the editor are the values
        */
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_PRODUCT, productString);
        values.put(InventoryEntry.COLUMN_PRICE, priceString);
        values.put(InventoryEntry.COLUMN_QUANTITY, quantityString);
        values.put(InventoryEntry.COLUMN_SUPPLIER, supplierString);
        values.put(InventoryEntry.COLUMN_CONTACT, contactString);

        //Determine if this a new or an existing product by checking if mCurrentProductUri is null or not
        if (mCurrentProductUri == null) {
            /*
            This is a new product, so insert a new product into the provider,
            returning the content URI
            */
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (newUri == null) {
                //If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this,
                      getString(R.string.editor_insert_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                //Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this,
                  getString(R.string.editor_insert_product_successful), Toast.LENGTH_SHORT).show();
            }

            finish();

        } else {
            /*
            Otherwise it is an existing  product, so update the database with content URI:
            mCurrentProductUri and pass in the new ContentValues.
            */

            int rowsAffected =
            getContentResolver().update(mCurrentProductUri, values, null, null);

            //Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                //If no rows were affected, then there was an error with the update.
                Toast.makeText(this,
                    getString(R.string.editor_insert_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                //Otherwise the update was successful and we can display a toast.
                Toast.makeText(this,
                     getString(R.string.editor_insert_product_successful), Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_inventory_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_inventory_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Respond to a click on the "Save" menu option in the app bar overflow menu
        switch (item.getItemId()) {
            case R.id.action_save:
                saveProduct();
                return true;

            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;

            //Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                /*
                If the product hasn't changed, continue with navigating up to parent activity
                which is the {@link InventoryCatalogActivity}.
                */
                if (!mInventoryHasChanged) {
                    NavUtils.navigateUpFromSameTask(InventoryEditorActivity.this);
                    return true;
                }
                /*
                Otherwise if there are unsaved changes, set a dialog to warn the user.
                Create a clickListener to handle the user confirming that
                changes should be discarded.
                */
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //User clicked "Discard" button, navigate to parent activity-
                                NavUtils.navigateUpFromSameTask(
                                        InventoryEditorActivity.this);
                            }

                        };
                //Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        //Since the Editor shows all the  attributes, define a projection that contains
        // all columns from the inventory table
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_PRODUCT,
                InventoryEntry.COLUMN_PRICE,
                InventoryEntry.COLUMN_QUANTITY,
                InventoryEntry.COLUMN_SUPPLIER,
                InventoryEntry.COLUMN_CONTACT
        };
        return new CursorLoader(this,
                mCurrentProductUri,
                projection,
                null,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        //Bail early if the cursor is null or there is less that 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        //Proceed with moving to the first row of the cursor and reading data from it
        // This should be the only row in the cursor
        if (cursor.moveToFirst()) {
            //Find the columns of attributes that we ware interested in
            int productNameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex( InventoryEntry.COLUMN_QUANTITY);
            int supplierNameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_SUPPLIER);
            int contactColumnIndex = cursor.getColumnIndex( InventoryEntry.COLUMN_CONTACT);

            //Extract out the value from the Cursor for the given column index
            String productName = cursor.getString(productNameColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String supplierName = cursor.getString(supplierNameColumnIndex);
            long supplierPhoneNumber = cursor.getLong(contactColumnIndex);

            //Update the views on the screen with the values from the database
            mProductEditText.setText(productName);
            mPriceEditText.setText(Integer.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));
            mSupplierEditText.setText(supplierName);
            mContactEditText.setText(Long.toString(supplierPhoneNumber));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        //If the loader is invalidated, clear out all the data from the input fields.
        mProductEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mSupplierEditText.setText("");
        mContactEditText.setText("");
    }

    /**
     * Delete product from the database
     */

    private void deleteProduct() {

        //Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            /*
            Call the ContentResolver to delete the product at the given content URI.
            Pass in null for the selection and selection args because the mCurrentProductUri
            content Uri already identifies the product that we want.
            */
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                //If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this,
                     getString(R.string.editor_delete_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                //Otherwise: the delete was successful, display a toast.
                Toast.makeText(this,
                  getString(R.string.editor_delete_product_successful), Toast.LENGTH_SHORT).show();
            }
        }
        // Close the activity
        finish();
    }

    private void showDeleteConfirmationDialog() {
        //Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                //user clicked the "Delete Button, so delete the product.
                deleteProduct();

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }

            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener
    ) {
        /*
        Create an AlertDialog.Builder and set the message, and click listeners
        for the positive and negative button on the dialog.
        */
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                /*
                User clicked the "Keep editing" button, so dismiss the dialog
                and continue editing the inventory.
                */
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        //Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}