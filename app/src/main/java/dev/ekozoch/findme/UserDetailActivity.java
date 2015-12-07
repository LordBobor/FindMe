package dev.ekozoch.findme;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.fivehundredpx.android.blur.BlurringView;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;

import java.util.List;

import dev.ekozoch.findme.parse.classes.Interest;
import dev.ekozoch.findme.parse.classes.User;
import me.kaede.tagview.Tag;
import me.kaede.tagview.TagView;

public class UserDetailActivity extends AppCompatActivity {

    private static final String KEY_ID = "key_id";
    private static final String USER_PIC = "user_pic";

    TextView tvUserName;
    TagView tagView;

    public static void show(Activity activity, String data) {
        String[] parsedData = data.split(" ");
        Intent i = new Intent(activity, UserDetailActivity.class);
        i.putExtra(KEY_ID, parsedData[0]);
        i.putExtra(USER_PIC, parsedData[1]);
        activity.startActivityForResult(i, BaseActivity.ACTIVITY_USER_DETAILS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvUserName = (TextView) findViewById(R.id.tvUserName);
        tagView = (TagView) findViewById(R.id.tagView);

        ImageView ivUserPic = (ImageView) findViewById(R.id.ivUserPic);
        ImageView ivUserPicBackground = (ImageView) findViewById(R.id.ivUserPicBlurred);
        BlurringView blurringView = (BlurringView) findViewById(R.id.blurring_view);

        // Give the blurring view a reference to the blurred view.
        blurringView.setBlurredView(ivUserPicBackground);

        Picasso.with(this).load(getIntent().getExtras().getString(USER_PIC)).into(ivUserPic);
        Picasso.with(this).load(getIntent().getExtras().getString(USER_PIC)).into(ivUserPicBackground);

        loadUser(getIntent().getExtras().getString(KEY_ID));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                            }
                        }).show();
            }
        });
    }

    private void loadUser(String id) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.getInBackground(id, new GetCallback<ParseUser>() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                User user = (User) parseUser;
                tvUserName.setText(((User) parseUser).getName());
            }
        });

        Interest.getItemsQuery(id, new FindCallback<Interest>() {
            @Override
            public void done(List<Interest> list, ParseException e) {
                for (Interest interest : list) {
                    Tag tag = new Tag(interest.getName());
                    tag.tagTextColor = Color.parseColor("#FFFFFF");
                    tag.layoutColor = Color.parseColor("#F57c00");
                    tag.layoutColorPress = Color.parseColor("#555555");
                    tag.layoutBorderColor = Color.parseColor("#FFFFFF");
                    tagView.addTag(tag);
                }
            }
        });
    }

}
