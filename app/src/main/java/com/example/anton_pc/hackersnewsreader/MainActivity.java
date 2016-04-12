package com.example.anton_pc.hackersnewsreader;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Map<Integer, String> articleURLs = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer,String>();
    ArrayList<Integer> articleIds = new ArrayList<Integer>();

    SQLiteDatabase articleDb;

    ListView listView;
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> urls = new ArrayList<>();
    ArrayAdapter arrayAdapter;
    ArrayList<String> content = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView)findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("Article Url", urls.get(position));
                Intent i = new Intent(MainActivity.this, WebView.class);
                i.putExtra("URL", urls.get(position));
                i.putExtra("content", content.get(position));
                startActivity(i);
            }
        });

        articleDb = this.openOrCreateDatabase("Articles", Context.MODE_PRIVATE, null);

        articleDb.execSQL("CREATE TABLE IF NOT EXISTS articles(id INTEGER PRIMARY KEY, articleId VARCHAR, url VARCHAR, title VARCHAR, context VARCHAR)");

        updateListView();

        DownoadTask task = new DownoadTask();

        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");


            Log.i("Titles", articleTitles.toString());
            Log.i("URLs", articleURLs.toString());
            Log.i("IDs", articleIds.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateListView () {

        try {
            Cursor c = articleDb.rawQuery("SELECT * FROM articles ORDER BY articleId DESC", null);

            int contentIndex = c.getColumnIndex("content");
            int urlIndex = c.getColumnIndex("url");
            int titleIndex = c.getColumnIndex("title");

            Boolean blsRecord = c.moveToFirst();
            titles.clear();
            urls.clear();

            while (blsRecord) {

                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));
                content.add(c.getString(contentIndex));

                blsRecord = c.moveToNext();


            }

            arrayAdapter.notifyDataSetChanged();
        }
        catch(Exception e){
            e.printStackTrace();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public class DownoadTask extends AsyncTask<String, Void, String>{


        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try{
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while(data != -1){

                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                articleDb.execSQL("DELETE FROM articles");
                for(int i = 0; i < 20; i++){

                    String articleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId +".json?print=pretty");
                    urlConnection = (HttpURLConnection)url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);

                    data = reader.read();
                    String articleInfo = "";

                    while(data != -1){

                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }


                    JSONObject jsonObject = new JSONObject(articleInfo);
                    String articleTitle = jsonObject.getString("title");
                    String articleURL = jsonObject.getString("url");
                    String articleContent = "";
                    /*
                    url = new URL(articleURL);
                    urlConnection = (HttpURLConnection)url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);

                    data = reader.read();
                    String articleContent = "";

                    while(data != -1){

                        char current = (char) data;
                        articleInfo += current;
                        data = reader.read();
                    }*/

                    articleIds.add(Integer.valueOf(articleId));
                    articleURLs.put(Integer.valueOf(articleId), articleURL);
                    articleTitles.put(Integer.valueOf(articleId), articleTitle);

                    String sql = "INSERT INTO articles(articleId, url, title, content) VALUES(?,?,?, ? )";

                    SQLiteStatement statement = articleDb.compileStatement(sql);
                    statement.bindString(1, articleId);
                    statement.bindString(2, articleURL);
                    statement.bindString(3, articleTitle);
                    statement.bindString(4, articleContent);

                    statement.execute();



                }

            }catch (Exception e){
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }
}
