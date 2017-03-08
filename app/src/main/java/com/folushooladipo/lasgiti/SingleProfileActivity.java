package com.folushooladipo.lasgiti;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

public class SingleProfileActivity extends AppCompatActivity {
    private Bundle profile;
    private SimpleDraweeView profilePicture;
    private String userId;
    private TextView userIdView;
    private String gitHubWebsiteProfileUrl;
    private TextView gitHubWebsiteProfileUrlView;
    private Button shareBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_profile);

        profile = getIntent().getBundleExtra("profile");
        userId = profile.getString("userId");
        this.setTitle("Profile of: " + userId);

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

    }
}
