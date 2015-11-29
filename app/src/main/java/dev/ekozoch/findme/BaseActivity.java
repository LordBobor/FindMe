package dev.ekozoch.findme;

import android.support.v7.app.AppCompatActivity;

import dev.ekozoch.findme.parse.classes.User;

/**
 * Created by ekozoch on 15.10.15.
 */
public abstract class BaseActivity extends AppCompatActivity {
    public static final String SOCIAL_NETWORK_TAG = "SocialIntegrationMain.SOCIAL_NETWORK_TAG";
    public static final int ACTIVITY_USER_DETAILS = 0;

    protected User getUser(){
        return FindMeApplication.currentUser;
    }
}
