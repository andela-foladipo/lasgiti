package com.folushooladipo.lasgiti;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class ProfilesActivity extends AppCompatActivity {
    private int pageNumber;
    private final String GITHUB_SEARCH_API = "https://api.github.com/search/users";
    private final String GITHUB_SEARCH_API_DEFAULT_QUERY_STRING = "q=location:lagos+type:user+language:java&order=desc&sort=repositories&per_page=50&";
    private final String GITHUB_SEARCH_API_DEFAULT_URL = GITHUB_SEARCH_API +
            "?access_token=" + SensitiveData.GITHUB_ACCESS_TOKEN + GITHUB_SEARCH_API_DEFAULT_QUERY_STRING;

    private LinearLayout loadingContainer;
    private LinearLayout loadingFailedContainer;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;
    private ArrayList<JSONObject> profilesDataset = new ArrayList<JSONObject>();
    private FetchGitHubProfilesTask fetchProfilesTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

        pageNumber = 0;
        loadingContainer = (LinearLayout) findViewById(R.id.profiles_activity_loading_container);
        loadingFailedContainer = (LinearLayout) findViewById(R.id.profiles_activity_loading_failed_container);
        recyclerView = (RecyclerView) findViewById(R.id.profiles_activity_profiles_container);
//        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ProfilesAdapter();
        recyclerView.setAdapter(adapter);

        String url = GITHUB_SEARCH_API_DEFAULT_URL + "page=" + pageNumber + "&";
        fetchProfilesTask = new FetchGitHubProfilesTask(url);
        fetchProfilesTask.execute();

        Button retryLoading = (Button) findViewById(R.id.profiles_activity_retry_loading);
        retryLoading.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadingFailedContainer.setVisibility(View.GONE);
                loadingContainer.setVisibility(View.VISIBLE);
                loadMoreProfiles();

            }
        });

    }

    public class ProfilesAdapter extends RecyclerView.Adapter<ProfileSummaryViewHolder> {
        @Override
        public ProfileSummaryViewHolder onCreateViewHolder(ViewGroup parent,
                                                 int viewType) {
            // This method is only for creating but NOT instantiating view holders.
            // It will just return empty view holders, which will then be filled
            // with content in onBindViewHolder().
            
            TableRow summary = (TableRow) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.template_profile_summary, parent, false);
            return new ProfileSummaryViewHolder(summary);
        }

        @Override
        public void onBindViewHolder(final ProfileSummaryViewHolder holder, int position) {
            JSONObject singleProfileData = profilesDataset.get(position);
            String id;
            String profilePicUrl;
            try {
                id = singleProfileData.getString("login");
                profilePicUrl = singleProfileData.getString("avatar_url");
            } catch (Exception e) {
                id = "Anonymous";
                profilePicUrl = null;
            }

            final String finalProfilePicUrl = profilePicUrl;
            holder.summaryContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = holder.getAdapterPosition();
                    JSONObject profile = profilesDataset.get(position);
                    Bundle args = new Bundle();
                    args.putString("userId", profile.optString("login"));
                    args.putString("profilePicture", finalProfilePicUrl);
                    args.putString("websiteProfileUrl", profile.optString("html_url"));
                    args.putString("searchApiProfileUrl", profile.optString("url"));
                    Intent intent = new Intent(getApplicationContext(), SingleProfileActivity.class);
                    intent.putExtra("profile", args);
                    startActivity(intent);
                }
            });
            holder.userId.setText(id);
            holder.profilePicture.setImageURI(profilePicUrl);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return profilesDataset.size();
        }
    }

    // This is the custom view holder that ProfilesAdapter uses.
    private class ProfileSummaryViewHolder extends RecyclerView.ViewHolder {
        TableRow summaryContainer;
        SimpleDraweeView profilePicture;
        TextView userId;

        ProfileSummaryViewHolder(TableRow container) {
            super(container);
            summaryContainer = container;
            profilePicture = (SimpleDraweeView) container.findViewById(R.id.small_display_picture);
            userId = (TextView) container.findViewById(R.id.userId);
        }
    }

    private class FetchGitHubProfilesTask extends AsyncTask<Void, Void, String> {
        String urlToQuery;

        FetchGitHubProfilesTask(String url) {
            urlToQuery = url;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result = "";
            try {
                result = downloadUrl(urlToQuery);
            }
            catch (IOException e) {
                e.printStackTrace();
                result = "ERROR: " + e.getMessage();
            }
            catch (Exception e) {
                e.printStackTrace();
                result = "ERROR: " + e.getMessage();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("foo", result);
            if (result.substring(0, 5).contains("ERROR")) {
                // Show an error message and a "RETRY" button.
                loadingFailedContainer.setVisibility(View.VISIBLE);
                loadingContainer.setVisibility(View.GONE);
            }
            else {
                try {
                    JSONObject resultObj = new JSONObject(result);
                    JSONArray profiles = resultObj.getJSONArray("items");
                    for (int i = 0; i < profiles.length(); i++) {
                        JSONObject profile = (JSONObject) profiles.get(i);
                        profilesDataset.add(profile);
                    }
                    adapter.notifyDataSetChanged();
                    pageNumber++;
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String downloadUrl(String myUrl) throws IOException {
        InputStream inStream = null;
        try {
            URL url = new URL(myUrl);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            inStream = conn.getInputStream();

            return readIt(inStream);

        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new IOException();
        }
        finally {
            if (inStream != null) {
                inStream.close();
            }
        }
    }

    private String readIt(InputStream stream) throws IOException {
        String result = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            result += line;
        }
        reader.close();
        return result;
    }

    private void loadMoreProfiles() {
        String url = GITHUB_SEARCH_API_DEFAULT_URL + "page=" + pageNumber + "&";
        fetchProfilesTask = new FetchGitHubProfilesTask(url);
        fetchProfilesTask.execute();

    }

}
