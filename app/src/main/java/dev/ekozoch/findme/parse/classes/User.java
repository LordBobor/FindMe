package dev.ekozoch.findme.parse.classes;

import com.parse.LogInCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseUser;

public class User extends ParseUser {

    public double getLatitude() {
        return getDouble("latitude");
    }

    public void setLatitude(double latitude) {
        put("latitude", latitude);
    }

    public double getLongitude() {
        return getDouble("longitude");
    }

    public void setLongitude(double latitude) {
        put("longitude", latitude);
    }

    public String getName(){
        return getString("fio");
    }

    public void setName(String name){ put("fio", name); }

    public String getUserPic(){
        return getString("user_pic");
    }

    public void setUserPic(String userPic){ put("user_pic", userPic); }

    public static User getCurrentUser(){
        return (User) ParseUser.getCurrentUser();
    }

    public static void logInAnonymous(LogInCallback callback){
        ParseAnonymousUtils.logIn(callback);
    }
}
