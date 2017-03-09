package com.folushooladipo.lasgiti;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableRow;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class SingleProfileActivity extends AppCompatActivity {
    private Bundle profile;
    private SimpleDraweeView profilePicture;
    private String userId;
    private TextView userIdView;
    private String gitHubWebsiteProfileUrl;
    private TextView gitHubWebsiteProfileUrlView;
    private Button shareBtn;
    private String gitHubSearchApiUrl;
    private FetchDetailedProfileTask detailedProfileTask;
    private TableRow loadingDetailedProfileContainer;
    private TableRow failedToLoadDetailedProfileContainer;
    private LinearLayout profileDetailsContainer;
    private TextView emailView;
    private TextView fullNameView;
    private TextView numberOfReposView;
    private TextView numberOfFollowersView;
    private TextView isHireableView;

    // TODO: Call cancel on detailedProfileTask in onDestroy() and such.
    // TODO: Test for what happens when detailedProfileTask is running and onStop() or onPause() happens.
    // TODO: Move this code to onStart()?
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_profile);

        profile = getIntent().getBundleExtra("profile");
        userId = profile.getString("userId");
        this.setTitle("Profile of: " + userId);

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.single_profile_loading_detailed_profile_progress_bar);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor("#e93ad4"), PorterDuff.Mode.SRC_IN);

        profilePicture = (SimpleDraweeView) findViewById(R.id.single_profile_picture);
        profilePicture.setImageURI(profile.getString("profilePicture"));

        userIdView = (TextView) findViewById(R.id.single_profile_user_id);
        userIdView.setText(userId);

        gitHubWebsiteProfileUrl = profile.getString("websiteProfileUrl");
        gitHubWebsiteProfileUrlView = (TextView) findViewById(R.id.single_profile_github_website_profile_url);
        gitHubWebsiteProfileUrlView.setPaintFlags(gitHubWebsiteProfileUrlView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        gitHubWebsiteProfileUrlView.setText(gitHubWebsiteProfileUrl);
        gitHubWebsiteProfileUrlView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(gitHubWebsiteProfileUrl));
                startActivity(intent);
            }
        });

        shareBtn = (Button) findViewById(R.id.single_profile_share_btn);
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                String shareMsg = "Check out this awesome developer @" + userId + ", " + gitHubWebsiteProfileUrl + ".";
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMsg);
                shareIntent.setType("text/plain");
                startActivity(shareIntent);
            }
        });
        gitHubSearchApiUrl = profile.getString("searchApiProfileUrl");
        detailedProfileTask = new FetchDetailedProfileTask(gitHubSearchApiUrl);
        loadingDetailedProfileContainer = (TableRow) findViewById(R.id.single_profile_loading_detailed_profile_container);
        profileDetailsContainer = (LinearLayout) findViewById(R.id.single_profile_profile_details_container);
        failedToLoadDetailedProfileContainer = (TableRow) findViewById(R.id.single_profile_load_detailed_profile_failed_container);
        Button retryLoadingDetailedProfile = (Button) findViewById(R.id.single_profile_load_detailed_profile_failed_btn);
        retryLoadingDetailedProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                failedToLoadDetailedProfileContainer.setVisibility(View.GONE);
                loadingDetailedProfileContainer.setVisibility(View.VISIBLE);
                detailedProfileTask = new FetchDetailedProfileTask(gitHubSearchApiUrl);
                detailedProfileTask.execute();
            }
        });
        emailView = (TextView) findViewById(R.id.single_profile_email);
        fullNameView = (TextView) findViewById(R.id.single_profile_full_name);
        numberOfReposView = (TextView) findViewById(R.id.single_profile_number_of_repos);
        numberOfFollowersView = (TextView) findViewById(R.id.single_profile_number_of_followers);
        isHireableView = (TextView) findViewById(R.id.single_profile_is_hireable);
        detailedProfileTask.execute();
    }

    private class FetchDetailedProfileTask extends AsyncTask<Void, Void, String> {
        String detailedProfileUrl;
        FetchDetailedProfileTask(String url) {
            detailedProfileUrl = url;
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result;
            try {
                result = Utilities.downloadUrl(detailedProfileUrl);
            } catch (IOException e) {
                result = "ERROR: " + e.getMessage();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            loadingDetailedProfileContainer.setVisibility(View.GONE);
            if (result.substring(0, 5).contains("ERROR")) {
                failedToLoadDetailedProfileContainer.setVisibility(View.VISIBLE);
            }
            else {
                try {
                    JSONObject detailedProfile = new JSONObject(result);
                    String email = detailedProfile.getString("email");
                    if (email.equals("null")) {
                        emailView.setText(R.string.default_not_specified);
                    }
                    else {
                        emailView.setText(email);
                    }
                    fullNameView.setText(detailedProfile.getString("name"));
                    numberOfReposView.setText(String.valueOf(detailedProfile.getInt("public_repos")));
                    numberOfFollowersView.setText(String.valueOf(detailedProfile.getInt("followers")));
                    Boolean isHireable = detailedProfile.optBoolean("hireable", false);
                    if (isHireable) {
                        isHireableView.setText(R.string.default_yes);
                    }
                    else {
                        isHireableView.setText(R.string.default_no);
                    }
                    profileDetailsContainer.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    failedToLoadDetailedProfileContainer.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}
