package com.folushooladipo.lasgiti;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class ProfilesActivity extends AppCompatActivity {
    private int pageNumber;
    private int PROFILES_PER_PAGE = 50;
    private int TOTAL_AVAILABLE_PROFILES = -1;
    private final String GITHUB_SEARCH_API = "https://api.github.com/search/users";
    private final String GITHUB_SEARCH_API_DEFAULT_QUERY_STRING = "q=location:lagos+type:user+language:java&order=desc&sort=repositories&per_page=50&";
    private final String GITHUB_SEARCH_API_DEFAULT_URL = GITHUB_SEARCH_API +
            "?access_token=" + SensitiveData.GITHUB_ACCESS_TOKEN + "&" + GITHUB_SEARCH_API_DEFAULT_QUERY_STRING;

    private Boolean isLoadingProfiles;
    private LinearLayout loadingContainer;
    private TextView loadingView;
    private LinearLayout loadingFailedContainer;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private RecyclerView.Adapter adapter;
    private ArrayList<JSONObject> profilesDataset = new ArrayList<JSONObject>();
    private FetchGitHubProfilesTask fetchProfilesTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiles);

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.profiles_activity_loading_progress_bar);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#e93ad4"), PorterDuff.Mode.SRC_IN);

        // IMPORTANT: This is only incremented after a successful retrieval of profiles.
        pageNumber = 1;
        // isLoadingProfiles is especially useful in the infinite scrolling functionality because
        // the RecyclerView's onScrollListener gets called many times for EACH scroll by the user.
        isLoadingProfiles = false;
        loadingContainer = (LinearLayout) findViewById(R.id.profiles_activity_loading_container);
        loadingView = (TextView) findViewById(R.id.profiles_activity_loading);
        loadingFailedContainer = (LinearLayout) findViewById(R.id.profiles_activity_loading_failed_container);
        recyclerView = (RecyclerView) findViewById(R.id.profiles_activity_profiles_container);
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
                loadProfiles(pageNumber);

            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Only when the user scrolls down.
                if (dy > 0) {
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                    int numberOfItems = adapter.getItemCount();
                    // Remember that lastVisibleItem is a zero-based index.
                    if (lastVisibleItem == (numberOfItems - 1)) {
                        if (pageNumber == 0) {
                            loadingView.setText(R.string.loading_more_profiles_msg);
                        }
                        loadingContainer.setVisibility(View.VISIBLE);
                        int numberOfProfilesFetched = pageNumber * PROFILES_PER_PAGE;
                        Log.i("profileCount", "" + numberOfProfilesFetched);
                        if (!isLoadingProfiles) {
                            if (numberOfProfilesFetched < TOTAL_AVAILABLE_PROFILES
                                    && numberOfProfilesFetched < Utilities.GITHUB_SEARCH_RESULTS_LIMIT) {
                                isLoadingProfiles = true;
                                loadProfiles(pageNumber);
                            }
                            else {
                                loadingContainer.setVisibility(View.GONE);
                                if (numberOfProfilesFetched >= TOTAL_AVAILABLE_PROFILES) {
                                    Toast.makeText(getApplicationContext(), "No more profiles to display.", Toast.LENGTH_SHORT).show();
                                }
                                if (numberOfProfilesFetched >= Utilities.GITHUB_SEARCH_RESULTS_LIMIT) {
                                    Toast.makeText(getApplicationContext(), "You can only view 1000 profiles.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    }
                }
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

        // Return the size of the dataset (invoked by the layout manager).
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
                result = Utilities.downloadUrl(urlToQuery);
            }
            catch (Exception e) {
                result = "ERROR: " + e.getMessage();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("url", urlToQuery);
            loadingContainer.setVisibility(View.GONE);
            if (result.substring(0, 5).contains("ERROR")) {
                // Show an error message and a "RETRY" button.
                loadingFailedContainer.setVisibility(View.VISIBLE);
            }
            else {
                try {
                    JSONObject resultObj = new JSONObject(result);
                    // Assign once.
                    if (TOTAL_AVAILABLE_PROFILES == -1) {
                        TOTAL_AVAILABLE_PROFILES = resultObj.getInt("total_count");
                    }
                    JSONArray profiles = resultObj.getJSONArray("items");
                    for (int i = 0; i < profiles.length(); i++) {
                        JSONObject profile = (JSONObject) profiles.get(i);
                        profilesDataset.add(profile);
                    }
                    adapter.notifyDataSetChanged();
                    isLoadingProfiles = false;
                    pageNumber++;
                }
                catch (JSONException e) {
                    loadingFailedContainer.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void loadProfiles(int page) {
        Log.i("pagination", "" + page);
        String url = GITHUB_SEARCH_API_DEFAULT_URL + "page=" + page + "&";
        fetchProfilesTask = new FetchGitHubProfilesTask(url);
        fetchProfilesTask.execute();
    }

}
