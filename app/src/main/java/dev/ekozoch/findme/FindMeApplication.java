package dev.ekozoch.findme;

import android.app.Application;
import android.content.Context;

import com.facebook.FacebookSdk;
import com.parse.Parse;
import com.parse.ParseObject;
import com.parse.ParseUser;

import dev.ekozoch.findme.parse.classes.Interest;
import dev.ekozoch.findme.parse.classes.User;


public class FindMeApplication extends Application {
    public static User currentUser;
    private static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        // Регистрация субклассов обязательно до вызова Parse.initialize()!
//        ParseObject.registerSubclass(ParseList.class);
        ParseObject.registerSubclass(Interest.class);
        ParseObject.registerSubclass(User.class);

        Parse.initialize(this, "IkxF598P5bSTh1l6f5tv4sFPeyNdUdr9q9ku1Dw4", "f3mplbIebq40DBFtlGVIE7fXE3g9opvOvFuZLh6D");
        ParseUser.enableRevocableSessionInBackground();
        context = getBaseContext();

        FacebookSdk.sdkInitialize(getApplicationContext());
    }
}
