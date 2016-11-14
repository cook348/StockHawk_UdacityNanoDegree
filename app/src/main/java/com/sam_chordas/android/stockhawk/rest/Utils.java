package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.db.chart.model.LineSet;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by sam_chordas on 10/8/15.
 *
 */
public class Utils {

  public static final String TAG = "tag";
  public static final String ADD = "add";
  public static final String INIT = "init";
  public static final String PERIODIC = "periodic";
  public static final String DISTINCT = "Distinct ";

  private static final String QUERY = "query";
  private static final String COUNT = "count";
  private static final String QUOTE = "quote";
  private static final String RESULTS = "results";
  private static final String NULL = "null";
  private static final String CHANGE = "Change";
  private static final String BID = "Bid";
  private static final String CHANGE_IN_PERCENT = "ChangeinPercent";
  private static final String DATE = "Date";
  private static final String CLOSE = "Close";


  private static String LOG_TAG = Utils.class.getSimpleName();

  public static boolean showPercent = true;

  public static LineSet historyJsonToPlotVals(String JSON){

    LineSet lineSet = null;

    JSONObject jsonObject = null;
    JSONArray jsonArray = null;

    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0) {
        jsonObject = jsonObject.getJSONObject(QUERY);
        jsonArray = jsonObject.getJSONObject(RESULTS).getJSONArray(QUOTE);

        if (jsonArray != null && jsonArray.length() != 0) {

          String[] labels = new String[jsonArray.length()];
          float[] values = new float[jsonArray.length()];

          int counter = 0;
          for (int i = jsonArray.length() - 1; i >= 0 ; i--) {
            labels[counter] = jsonArray.getJSONObject(i).getString(DATE);
            values[counter] = (float) jsonArray.getJSONObject(i).getDouble(CLOSE);
            counter++;
          }
          return new LineSet(labels, values);
        }
      }

    } catch (JSONException je){
      Log.e(LOG_TAG, "String to JSON failed: " + je);
    }

    return null;
  }

  public static ArrayList quoteJsonToContentVals(String JSON){
    ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
    JSONObject jsonObject = null;
    JSONArray resultsArray = null;
    try{
      jsonObject = new JSONObject(JSON);
      if (jsonObject != null && jsonObject.length() != 0){
        jsonObject = jsonObject.getJSONObject(QUERY);
        int count = Integer.parseInt(jsonObject.getString(COUNT));
        if (count == 1){
          jsonObject = jsonObject.getJSONObject(RESULTS)
              .getJSONObject(QUOTE);
          batchOperations.add(buildBatchOperation(jsonObject));
        } else{
          resultsArray = jsonObject.getJSONObject(RESULTS).getJSONArray(QUOTE);

          if (resultsArray != null && resultsArray.length() != 0){
            for (int i = 0; i < resultsArray.length(); i++){
              jsonObject = resultsArray.getJSONObject(i);
              batchOperations.add(buildBatchOperation(jsonObject));
            }
          }
        }
      }
    } catch (JSONException e){
      Log.e(LOG_TAG, "String to JSON failed: " + e);
      Log.d(LOG_TAG, "JSON: " + JSON);
    }
    return batchOperations;
  }

  public static String truncateBidPrice(String bidPrice){
    if(bidPrice.equals(NULL)){
      bidPrice = "";
    } else {
      bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
    }
    return bidPrice;
  }

  public static String truncateChange(String change, boolean isPercentChange){
    if(change.equals(NULL)){
      change = "";
    } else {
      String weight = change.substring(0, 1);
      String ampersand = "";
      if (isPercentChange) {
        ampersand = change.substring(change.length() - 1, change.length());
        change = change.substring(0, change.length() - 1);
      }
      change = change.substring(1, change.length());
      double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
      change = String.format("%.2f", round);
      StringBuffer changeBuffer = new StringBuffer(change);
      changeBuffer.insert(0, weight);
      changeBuffer.append(ampersand);
      change = changeBuffer.toString();
    }
    return change;

  }

  public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
    try {
      String change = jsonObject.getString(CHANGE);
      builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(QuoteColumns.SYMBOL));
      builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString(BID)));
      builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
          jsonObject.getString(CHANGE_IN_PERCENT), true));
      builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
      builder.withValue(QuoteColumns.ISCURRENT, 1);
      if (change.charAt(0) == '-'){
        builder.withValue(QuoteColumns.ISUP, 0);
      }else{
        builder.withValue(QuoteColumns.ISUP, 1);
      }

    } catch (JSONException e){
      e.printStackTrace();
    }
    return builder.build();
  }
}
