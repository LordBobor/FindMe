package dev.ekozoch.findme;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;

import dev.ekozoch.findme.parse.classes.User;

/**
 * Created by ekozoch on 15.10.15.
 */
public abstract class BaseActivity extends AppCompatActivity {
    public static final String SOCIAL_NETWORK_TAG = "SocialIntegrationMain.SOCIAL_NETWORK_TAG";

    protected User getUser(){
        return FindMeApplication.currentUser;
    }
}
