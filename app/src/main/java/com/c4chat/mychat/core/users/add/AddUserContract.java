package com.c4chat.mychat.core.users.add;

/**
 * Created by hendalzahrani on 7/18/17.
 */

import android.content.Context;

import com.google.firebase.auth.FirebaseUser;



public interface AddUserContract {
    interface View {
        void onAddUserSuccess(String message);

        void onAddUserFailure(String message);
    }

    interface Presenter {
        void addUser(Context context, FirebaseUser firebaseUser);
    }

    interface Interactor {
        void addUserToDatabase(Context context, FirebaseUser firebaseUser);
    }

    interface OnUserDatabaseListener {
        void onSuccess(String message);

        void onFailure(String message);
    }
}