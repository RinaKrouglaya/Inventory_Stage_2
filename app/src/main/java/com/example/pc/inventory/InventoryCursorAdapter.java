package com.example.pc.inventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pc.inventory.data.InventoryContract.InventoryEntry;

public class InventoryCursorAdapter extends CursorAdapter {

    //Constructor
    public InventoryCursorAdapter(Context context, Cursor cursor) {
        super(context, cursor, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.inventory_list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        //Find field to populate in inflated in inventory_list_item.xml
        TextView productNameTextView = view.findViewById(R.id.product_name);
        TextView priceTextView =  view.findViewById(R.id.price);
        TextView quantityTextView = view.findViewById(R.id.quantity);
        ImageButton saleButton = view.findViewById(R.id.sale_button);

        //Find the relevant columns
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRODUCT);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_QUANTITY);

        //Read the inventory attributes from the Cursor for the current product
        String productName = cursor.getString(nameColumnIndex);
        String price = cursor.getString(priceColumnIndex);
        String quantity = cursor.getString(quantityColumnIndex);

        // sets the text on the TextViews
        productNameTextView.setText(productName);
        priceTextView.setText(price);
        quantityTextView.setText(quantity);

        //Get the current quantity and make into an integer
       String currentQuantityString = cursor.getString(quantityColumnIndex);

       final int currentQuantity = Integer.valueOf(currentQuantityString);

       // final int currentQuantity = quantity;
        // Get the rows from the table with the ID
        final int productId = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));


        //Decrement on the sale button
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (currentQuantity > 0) {
                    int newQuantity = currentQuantity - 1;

                    //Getting the URI with the append of the ID for the row
                    Uri quantityUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, productId);

                    //Getting the current value for quantity and decreasing value -1
                    ContentValues values = new ContentValues();
                    values.put(InventoryEntry.COLUMN_QUANTITY, newQuantity);
                    context.getContentResolver().update(quantityUri, values, null, null);
                }

                //A Toast message for the case of out of stock
                else {
                    Toast.makeText(context, R.string.sale_button_out_of_stock, Toast.LENGTH_SHORT).show();

                }
            }
        });

    }

}
