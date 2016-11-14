package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

public class WidgetRemoteViewService extends RemoteViewsService {
    public WidgetRemoteViewService() {
    }


    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {

                // get the data from the Provider
                if (data != null){
                    data.close();
                }

                // Clear the identityToken as in the Sunshine Project
                final long identityToken = Binder.clearCallingIdentity();

                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if(data != null){
                    data.close();
                    data = null;
                }

            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {

                if(position == AdapterView.INVALID_POSITION
                        || data == null
                        || !data.moveToPosition(position)){
                    return null;
                }

                RemoteViews views = new RemoteViews(getPackageName(), R.layout.list_item_widget);

                // put the data from data cursor into the views

                // Set symbol
                String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL));
                views.setTextViewText(R.id.stock_symbol_widget, symbol);

                // Set bid price
                String bidPrice = data.getString(data.getColumnIndex(QuoteColumns.BIDPRICE));
                if(bidPrice.equals("")){
                    bidPrice = getString(R.string.stock_not_found);
                }
                views.setTextViewText(R.id.bid_price_widget, bidPrice);

                // Set Change
                String changeText;
                if (Utils.showPercent){
                    changeText = data.getString(data.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
                } else{
                    changeText = data.getString(data.getColumnIndex(QuoteColumns.CHANGE));
                }
                if(changeText.equals("")){
                    changeText = getString(R.string.not_applicable);
                }
                views.setTextViewText(R.id.change_widget, changeText);

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtra(MyStocksActivity.EXTRA_PLOT_SYMBOL, symbol);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.list_item_widget);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if(data.moveToPosition(position)){
                    return data.getLong(data.getColumnIndex(QuoteColumns._ID));
                }
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
