package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.db.chart.model.ChartEntry;
import com.db.chart.model.LineSet;
import com.db.chart.renderer.AxisRenderer;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class PlotStockActivity extends AppCompatActivity {

    private static final String EXTRA_LABELS_ARRAY = "labels";
    private static final String EXTRA_VALUES_ARRAY = "values";
    private static final java.lang.String EXTRA_STOCK_SYMBOL = "stock_symbol";

    private LineChartView mLineChart;
    private TextView mEmptyTextView;
    private Context mContext;
    private LineSet mLineset;
    private String mStockSymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;

        setContentView(R.layout.activity_line_graph);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mLineChart = (LineChartView) findViewById(R.id.linechart);
        mEmptyTextView = (TextView) findViewById(R.id.empty_view_chart);

        if(savedInstanceState != null
                && savedInstanceState.containsKey(EXTRA_VALUES_ARRAY)
                && savedInstanceState.containsKey(EXTRA_LABELS_ARRAY)
                && savedInstanceState.containsKey(EXTRA_STOCK_SYMBOL)){

            // get the data out of the saved instance state and set the data
            float[] valuesArray = savedInstanceState.getFloatArray(EXTRA_VALUES_ARRAY);
            String[] labelsArray = savedInstanceState.getStringArray(EXTRA_LABELS_ARRAY);
            mStockSymbol = savedInstanceState.getString(EXTRA_STOCK_SYMBOL);

            mLineset = new LineSet(labelsArray, valuesArray);

            plotValues(mLineset, mLineChart);

        } else {
            // get the stock's value over time and plot it - get the stock symbol passed in as an Extra and query the database
            Intent intent = getIntent();
            if(intent != null && intent.getExtras() != null && intent.getExtras().containsKey(MyStocksActivity.EXTRA_PLOT_SYMBOL)){
                mStockSymbol = intent.getStringExtra(MyStocksActivity.EXTRA_PLOT_SYMBOL);

                // query the api for the stock over time
                FetchStockHistory fetchStockHistory = new FetchStockHistory();
                fetchStockHistory.execute(mStockSymbol);

            }
        }

        if(mStockSymbol != null) {
            String titleText = mStockSymbol + " - " + getString(R.string.month_performance);
            setTitle(titleText);
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {

        // save the data in saved instance state so the graph can be recreated on rotation without query of the API
        if(mLineset != null){
            ArrayList<ChartEntry> entries = mLineset.getEntries();
            String[] labels = new String[entries.size()];
            float[] values = new float[entries.size()];
            int c = 0;
            for(ChartEntry entry : entries){
                labels[c] = entry.getLabel();
                values[c] = entry.getValue();
                c++;
            }

            outState.putStringArray(EXTRA_LABELS_ARRAY, labels);
            outState.putFloatArray(EXTRA_VALUES_ARRAY, values);

            if(mStockSymbol != null) {
                outState.putString(EXTRA_STOCK_SYMBOL, mStockSymbol);
            }
        }
        super.onSaveInstanceState(outState);
    }

    public class FetchStockHistory extends AsyncTask<String, Void, LineSet> {

        String fetchData(String url) throws IOException {

            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        }

        @Override
        protected LineSet doInBackground(String... params) {

            String stockSymbol = params[0];

            Calendar cal = Calendar.getInstance();
            Date today = cal.getTime();

            // Get a date instance for one Month ago for the start date of the query
            cal.add(Calendar.MONTH, -1);
            Date lastMonth = cal.getTime();

            android.text.format.DateFormat df = new android.text.format.DateFormat();

            String todayString = df.format("yyyy-MM-dd", today).toString();
            String lastYearString = df.format("yyyy-MM-dd", lastMonth).toString();

            StringBuilder urlStringBuilder = new StringBuilder();
            try {
                // Base URL for the Yahoo query
                urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata " +
                        "where symbol = ", "UTF-8"));
                urlStringBuilder.append(URLEncoder.encode("\"" + stockSymbol + "\"", "UTF-8"));
                urlStringBuilder.append(URLEncoder.encode(" and startDate = \"" + lastYearString + "\" ", "UTF-8"));
                urlStringBuilder.append(URLEncoder.encode(" and endDate = \"" + todayString + "\" ", "UTF-8"));


            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            // finalize the URL for the API query.
            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");

            String urlString;
            String getResponse;

            if (urlStringBuilder != null) {
                urlString = urlStringBuilder.toString();
                try {
                    getResponse = fetchData(urlString);
                    return Utils.historyJsonToPlotVals(getResponse);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(LineSet lineSet) {
            if(lineSet != null) {
                mLineset = lineSet;

                mEmptyTextView.setVisibility(View.GONE);
                mLineChart.setVisibility(View.VISIBLE);

                plotValues(mLineset, mLineChart);

            } else {
                mEmptyTextView.setVisibility(View.VISIBLE);
                mLineChart.setVisibility(View.GONE);
            }
            super.onPostExecute(lineSet);
        }

    }

    private void plotValues(LineSet lineset, LineChartView lineChart){

        lineset.setColor(ContextCompat.getColor(mContext, R.color.white));

        int cushion = (int) Math.round(0.1*(lineset.getMax().getValue() - lineset.getMin().getValue()));
        int minVal = (int) lineset.getMin().getValue() - cushion;
        int maxVal = (int) lineset.getMax().getValue() + cushion ;

        lineChart.setAxisBorderValues(minVal, maxVal);
        lineChart.addData(lineset);
        lineChart.setStep(5);
        lineChart.setXLabels(AxisRenderer.LabelPosition.NONE);
        lineChart.show();
    }

}
