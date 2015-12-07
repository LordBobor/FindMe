package dev.ekozoch.findme;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

import dev.ekozoch.findme.parse.classes.Interest;
import dev.ekozoch.findme.parse.classes.User;


public class LoginFragment extends Fragment {
    CallbackManager callbackManager;

    public LoginFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        callbackManager = CallbackManager.Factory.create();
        LoginButton loginButton = (LoginButton) rootView.findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList("public_profile", "user_friends", "user_photos", "user_likes"));
        // If using in a fragment
        loginButton.setFragment(this);
        // Other app specific specialization

        final User user = (User) ParseUser.getCurrentUser();

        // Callback registration
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                requestUserName(user, loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }
        });
        return rootView;
    }

    private void requestUserName(final User user, AccessToken accessToken) {
        GraphRequest request = GraphRequest.newMeRequest(
                accessToken,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.e("LOL", object.toString());

                        setUserName(user, response.getJSONObject());
                        requestUserPic(user);
                    }
                });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name, groups");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void setUserName(User user, JSONObject jsonObject) {
        try {
            user.setUsername(jsonObject.getString("id"));
            user.setPassword(jsonObject.getString("id"));
            user.setName(jsonObject.getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void requestUserPic(final User user) {
        Bundle parameters = new Bundle();
        parameters.putString("height", "720");
        parameters.putString("width", "720");
        parameters.putBoolean("redirect", false);

        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/picture",
                parameters,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.e("LOL1", response.getJSONObject().toString());

                        setUserPic(user, response.getJSONObject());
                        saveUserToParse(user);
                    }
                }
        ).executeAsync();
    }

    private void setUserPic(User user, JSONObject jsonObject) {
        try {
            user.setUserPic(jsonObject.getJSONObject("data").getString("url").replace("\\", ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void requestUserLikes(final User user) {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "me/likes",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                        Log.e("LOL2", response.getJSONObject().toString());
                        setUserInterests(user, response.getJSONObject());
                    }
                }
        ).executeAsync();
    }

    private void setUserInterests(final User user, JSONObject jsonObject) {
        try {
            JSONArray interests = jsonObject.getJSONArray("data");
            final int[] j = {0};
            final int count = interests.length();
            for (int i = 0; i < count; ++i) {
                String interest = interests.getJSONObject(i).getString("name");
                Interest.saveInterestQuery(interest, user, new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        j[0]++;
                        if (j[0] == count - 1) {
                            user.saveInBackground();
                        }
                    }
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    private void saveUserToParse(final User user) {
        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(final ParseException e) {
                if (e != null) {
                    ParseUser.logInInBackground(user.getUsername(), user.getUsername(), new LogInCallback() {
                        public void done(ParseUser user, ParseException e) {
                            if (user != null) {
                                FindMeApplication.currentUser = (User) user;
                                Toast.makeText(getActivity(), "LogIn success", Toast.LENGTH_SHORT).show();
                                requestUserLikes((User) user);
                            } else {
                                Toast.makeText(getActivity(), "LogIn fail", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    Toast.makeText(getActivity(), "Signup success", Toast.LENGTH_SHORT).show();
                    final User user = (User) ParseUser.getCurrentUser();
                    user.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(final ParseException e) {
                            if (e != null) {
                                //Toast.makeText(getActivity(), "Save data Fail", Toast.LENGTH_SHORT).show();
                            } else {
                                //Toast.makeText(getActivity(), "Save data success", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}
