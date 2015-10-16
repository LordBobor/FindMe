package dev.ekozoch.findme;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.github.gorbin.asne.core.SocialNetwork;
import com.github.gorbin.asne.core.SocialNetworkManager;
import com.github.gorbin.asne.core.listener.OnLoginCompleteListener;
import com.github.gorbin.asne.core.listener.OnRequestSocialPersonCompleteListener;
import com.github.gorbin.asne.core.persons.SocialPerson;
import com.github.gorbin.asne.facebook.FacebookSocialNetwork;
import com.github.gorbin.asne.vk.VkSocialNetwork;
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;
import com.vk.sdk.VKScope;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dev.ekozoch.findme.parse.classes.User;


public class LoginFragment extends Fragment implements OnLoginCompleteListener,
        SocialNetworkManager.OnInitializationCompleteListener,
        OnRequestSocialPersonCompleteListener {
    public static SocialNetworkManager mSocialNetworkManager;
    /**
     * SocialNetwork Ids in ASNE:
     * 1 - Twitter
     * 2 - LinkedIn
     * 3 - Google Plus
     * 4 - Facebook
     * 5 - Vkontakte
     * 6 - Odnoklassniki
     * 7 - Instagram
     */
    public static final int FACEBOOK = 4;
    public static final int VK = 5;

    private Button facebook;
    private Button vkontakte;

    public LoginFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);
        // init buttons and set Listener
        facebook = (Button) rootView.findViewById(R.id.facebook);
        facebook.setOnClickListener(loginClick);
        vkontakte = (Button) rootView.findViewById(R.id.vk);
        vkontakte.setOnClickListener(loginClick);

        //Get Keys for initiate SocialNetworks
        String VK_KEY = getActivity().getString(R.string.vk_app_id);

        //Chose permissions
        ArrayList<String> fbScope = new ArrayList<String>();
        fbScope.addAll(Arrays.asList("public_profile, email, user_friends"));
        String[] vkScope = new String[] {
                VKScope.FRIENDS,
                VKScope.WALL,
                VKScope.PHOTOS,
                VKScope.NOHTTPS,
                VKScope.STATUS,
        };

        //Use manager to manage SocialNetworks
        mSocialNetworkManager = (SocialNetworkManager) getFragmentManager().findFragmentByTag(BaseActivity.SOCIAL_NETWORK_TAG);

        //Check if manager exist
        if (mSocialNetworkManager == null) {
            mSocialNetworkManager = new SocialNetworkManager();

            //Init and add to manager FacebookSocialNetwork
            FacebookSocialNetwork fbNetwork = new FacebookSocialNetwork(this, fbScope);
            mSocialNetworkManager.addSocialNetwork(fbNetwork);

            VkSocialNetwork vkNetwork = new VkSocialNetwork(this, VK_KEY, vkScope);
            mSocialNetworkManager.addSocialNetwork(vkNetwork);

            //Initiate every network from mSocialNetworkManager
            getFragmentManager().beginTransaction().add(mSocialNetworkManager, BaseActivity.SOCIAL_NETWORK_TAG).commit();
            mSocialNetworkManager.setOnInitializationCompleteListener(this);
        } else {
            //if manager exist - get and setup login only for initialized SocialNetworks
            if(!mSocialNetworkManager.getInitializedSocialNetworks().isEmpty()) {
                List<SocialNetwork> socialNetworks = mSocialNetworkManager.getInitializedSocialNetworks();
                for (SocialNetwork socialNetwork : socialNetworks) {
                    socialNetwork.setOnLoginCompleteListener(this);
                }
            }
        }
        return rootView;
    }



    @Override
    public void onSocialNetworkManagerInitialized() {
        //when init SocialNetworks - get and setup login only for initialized SocialNetworks
        for (SocialNetwork socialNetwork : mSocialNetworkManager.getInitializedSocialNetworks()) {
            socialNetwork.setOnLoginCompleteListener(this);
        }
    }

    @Override
    public void onLoginSuccess(int networkId) {
        startProfile(networkId);
    }

    @Override
    public void onError(int networkId, String requestID, String errorMessage, Object data) {
        Toast.makeText(getActivity(), getActivity().getString(R.string.fm_err) + errorMessage, Toast.LENGTH_LONG).show();
    }

    private void startProfile(int networkId){
        SocialNetwork socialNetwork = mSocialNetworkManager.getSocialNetwork(networkId);
        socialNetwork.setOnRequestCurrentPersonCompleteListener(this);
        socialNetwork.requestCurrentPerson();
    }

    @Override
    public void onRequestSocialPersonSuccess(int i, SocialPerson socialPerson) {
        final String accountUsername = socialPerson.id;
        final String accountPassword = socialPerson.id;
        final String userName = socialPerson.name;
        final String userPic = socialPerson.avatarURL;
        final User user = (User) ParseUser.getCurrentUser();
        user.setUsername(accountUsername);
        user.setPassword(accountPassword);
        user.setName(userName);
        user.setUserPic(userPic);
        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(final ParseException e) {
                if (e != null) {
                    ParseUser.logInInBackground(accountUsername, accountPassword, new LogInCallback() {
                        public void done(ParseUser user, ParseException e) {
                            if (user != null) {
                                FindMeApplication.currentUser = (User) user;
                                Toast.makeText(getActivity(), "LogIn success", Toast.LENGTH_SHORT).show();
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


    private View.OnClickListener loginClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int networkId = 0;
            switch (view.getId()){
                case R.id.facebook:
                    networkId = FACEBOOK;
                    break;
                case R.id.vk:
                    networkId = VK;
                    break;
            }
            SocialNetwork socialNetwork = mSocialNetworkManager.getSocialNetwork(networkId);
            if(!socialNetwork.isConnected()) {
                if(networkId != 0) {
                    socialNetwork.requestLogin();
                } else {
                    Toast.makeText(getActivity(), "Social Network Authorization Error", Toast.LENGTH_LONG).show();
                }
            } else {
                startProfile(socialNetwork.getID());
            }
        }
    };
}
