package org.pstorch.androidirclogviewer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;

    private final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));

    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private String baseUrl = null;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_back:
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    loadLog();
                    return true;
                case R.id.navigation_refresh:
                    loadLog();
                    return true;
                case R.id.navigation_forward:
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    final Date now = new Date();
                    if (cal.getTime().after(now)) {
                        cal.setTime(now);
                    }
                    loadLog();
                    return true;
            }
            return false;
        }

    };

    private void loadLog() {
        new RetrieveLogTask().execute(cal.getTime());
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id==R.id.settings){
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(final String html){
        final Spanned text;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        } else {
            text = Html.fromHtml(html);
        }
        return text;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextMessage = (TextView) findViewById(R.id.message);
        mTextMessage.setLinksClickable(true);
        mTextMessage.setMovementMethod(LinkMovementMethod.getInstance());
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        baseUrl = PreferenceManager.getDefaultSharedPreferences(this).getString("base_url", null);

        loadLog();
    }

    private class RetrieveLogTask extends AsyncTask<Date, Void, String> {

        final Pattern pattern = Pattern.compile("^(\\d\\d:\\d\\d:\\d\\d) <([^>]*)> (.*)");

        protected String doInBackground(final Date... dates) {
            final StringBuilder formattedText = new StringBuilder();
            formattedText.append("<h1>").append(DateFormat.getDateInstance().format(dates[0])).append("</h1>");
            try {
                final URL url = new URL(baseUrl + "/" + df.format(dates[0]) + ".txt");
                Log.i("RetrieveLogTask", "Loading " + url);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
                String line = null;
                do {
                    line = reader.readLine();
                    if (line != null) {
                        appendFormatted(formattedText, line);
                    }
                } while (line != null);
            } catch (final IOException e) {
                Log.e("RetrieveLogTask", "Error loading log", e);
                formattedText.append("<p><font color=\"#ff0000\">").append(e.toString()).append("</font></p>");
            }
            return formattedText.toString();
        }

        private void appendFormatted(final StringBuilder formattedText, final String line) {
            final Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                final String user = matcher.group(2);
                final String color = "#" + Integer.toHexString(user.hashCode()).substring(0, 6);
                formattedText.append(matcher.group(1))
                        .append("<font color=\"").append(color).append("\"> ").append(user).append(": </font>")
                        .append(Html.escapeHtml(matcher.group(3)));
            } else {
                formattedText.append("<font color=\"#cccccc\">").append(Html.escapeHtml(line)).append("</font>");
            }
            formattedText.append("<br/>");
        }

        protected void onPostExecute(final String formattedText) {
            mTextMessage.setText(fromHtml(formattedText));
        }
    }

}
