package dev.ekozoch.findme.parse.classes;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * Created by ekozoch on 03.12.15.
 */
@ParseClassName("Interest")
public class Interest extends ParseObject {

    public static void getItemsQuery(String userId, final FindCallback callback) {

        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.getInBackground(userId, new GetCallback<ParseUser>() {
            public void done(ParseUser parseUser, ParseException e) {
                if (e == null) {
                    User user = (User) parseUser;
                    ParseQuery<ParseObject> query = user.getRelation("interests").getQuery();
                    query.orderByAscending("updatedAt");
                    query.findInBackground(callback);
                } else {
                    e.printStackTrace();
                }
            }
        });


    }

    public static void saveInterestQuery(String name, final User user, final SaveCallback saveCallback) {
        final Interest interest = new Interest();
        interest.setName(name);

        ParseQuery<Interest> query = ParseQuery.getQuery("Interest");
        query.whereContains("name", name);
        query.getFirstInBackground(new GetCallback<Interest>() {
            public void done(final Interest result, ParseException e) {
                final ParseRelation relation = user.getRelation("interests");
                if (e != null && result == null) {
                    interest.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            interest.fetchInBackground(new GetCallback<ParseObject>() {
                                @Override
                                public void done(ParseObject object, ParseException e) {
                                    relation.add(object);
                                    saveCallback.done(e);
                                }
                            });
                        }
                    });
                } else if (result != null) {
                    relation.add(result);
                    saveCallback.done(e);
                }
            }
        });
    }

    public String getName() {
        return getString("name");
    }

    public void setName(String name) {
        put("name", name);
    }
}
