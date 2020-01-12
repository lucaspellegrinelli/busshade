package com.lucaspellegrinelli.busao.googleapi;

/**
 * Created by lucas on 09-Feb-17.
 */

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class WebsiteRequest extends AsyncTask<Void, Void, JSONObject>{
    private String requestUrl;
    private PostExecute postExecute;

    public WebsiteRequest(String requestUrl, PostExecute postExecute){
        this.requestUrl = requestUrl;
        this.postExecute = postExecute;
    }

    @Override
    protected JSONObject doInBackground(Void... params) {
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String allText = "";

            String output;
            while ((output = br.readLine()) != null) {
                allText += output;
            }

            conn.disconnect();

            return new JSONObject(allText);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(JSONObject result) {
        if(postExecute != null && result != null){
            postExecute.postAction(result);
        }
    }

    private Document getDocFromUrl(String url) {
        final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.101 Safari/537.36";
        Document doc = null;
        try {
            doc = Jsoup.connect(url).userAgent(USER_AGENT).get();
        } catch (IOException e) {
        }

        return doc;
    }
}
