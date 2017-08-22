package com.c4chat.mychat.ui.fragments;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.c4chat.mychat.R;
import com.c4chat.mychat.core.chat.ChatContract;
import com.c4chat.mychat.core.chat.ChatPresenter;
import com.c4chat.mychat.events.PushNotificationEvent;
import com.c4chat.mychat.models.Chat;
import com.c4chat.mychat.ui.adapters.ChatRecyclerAdapter;
import com.c4chat.mychat.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;



public class ChatFragment extends Fragment implements ChatContract.View, TextView.OnEditorActionListener {
    private RecyclerView mRecyclerViewChat;
    private EditText mETxtMessage;

    private ProgressDialog mProgressDialog;

    private ChatRecyclerAdapter mChatRecyclerAdapter;

    private ChatPresenter mChatPresenter;

    public static ChatFragment newInstance(String receiver, String receiverUid, String firebaseToken) {
        Bundle args = new Bundle();
        args.putString(Constants.ARG_RECEIVER, receiver);
        args.putString(Constants.ARG_RECEIVER_UID, receiverUid);
        args.putString(Constants.ARG_FIREBASE_TOKEN, firebaseToken);
        ChatFragment fragment = new ChatFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_chat, container, false);
        bindViews(fragmentView);
        return fragmentView;
    }

    private void bindViews(View view) {
        mRecyclerViewChat = (RecyclerView) view.findViewById(R.id.recycler_view_chat);
        mETxtMessage = (EditText) view.findViewById(R.id.edit_text_message);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        init();
    }

    private void init() {
        mProgressDialog = new ProgressDialog(getActivity());
        mProgressDialog.setTitle(getString(R.string.loading));
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setIndeterminate(true);

        mETxtMessage.setOnEditorActionListener(this);

        mChatPresenter = new ChatPresenter(this);
        mChatPresenter.getMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                getArguments().getString(Constants.ARG_RECEIVER_UID));
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_SEND) {
            sendMessage();
            return true;
        }
        return false;
    }

    private void sendMessage() {
        String receiver = getArguments().getString(Constants.ARG_RECEIVER);
        String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
        String sender = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String receiverFirebaseToken = getArguments().getString(Constants.ARG_FIREBASE_TOKEN);
        // room ID of the chat
        String room=senderUid+"_"+receiverUid;
        Log.d("on sendMessage: ", room);

        String message = mETxtMessage.getText().toString();

        try{

            Encryption e= new Encryption(getActivity().getApplicationContext());
            message= e.getAction(room, message);

            if(message==null)
                return;

        }
        catch(Exception e){

            Log.d("catch 132: ", e.getMessage());

        }

        Chat chat = new Chat(sender, receiver, senderUid, receiverUid, message, System.currentTimeMillis());
        mChatPresenter.sendMessage(getActivity().getApplicationContext(), chat, receiverFirebaseToken);
    }


    private void autoSend(String msg) {

        String receiver = getArguments().getString(Constants.ARG_RECEIVER);
        String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
        String sender = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String receiverFirebaseToken = getArguments().getString(Constants.ARG_FIREBASE_TOKEN);
        Chat chat = new Chat(sender, receiver, senderUid, receiverUid, msg, System.currentTimeMillis());
        mChatPresenter.sendMessage(getActivity().getApplicationContext(), chat, receiverFirebaseToken);
    }

    @Override
    public void onSendMessageSuccess() {
        mETxtMessage.setText("");
        Toast.makeText(getActivity(), "Message sent", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSendMessageFailure(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGetMessagesSuccess(Chat chat) {

        String msg= chat.message;
        String receiverUid = getArguments().getString(Constants.ARG_RECEIVER_UID);
        String senderUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String room=senderUid+"_"+receiverUid;
        Log.d("on onGetMessages: ", room);

        try {

            JSONObject json = new JSONObject(msg);

            Log.d("in try: ", "170");

            if(json.getString("p").length() >0){
                Log.d("p exists", msg);


                if(chat.receiverUid.equals(senderUid)){
                    Log.d("i am reciever", "175");

                    try {
                        Encryption e= new Encryption(getActivity().getApplicationContext());

                         msg= e.recieveKey(room, chat.message);
                        if(msg ==null)
                            return;
                         json= new JSONObject(msg);
                        if(!json.get("init").equals(senderUid)) {
                            autoSend(msg);
                            Log.d("done autosend", "191");
                        }
                    } catch (JSONException ex) {
                        Log.d("in catch", "184");
                        Log.d("msg", msg);

                        chat.message= msg;
                        mChatRecyclerAdapter.add(chat);
                        mRecyclerViewChat.smoothScrollToPosition(mChatRecyclerAdapter.getItemCount() - 1);

                    }

                }
            }


        }
        catch (JSONException ex) {

            Encryption e= new Encryption(getActivity().getApplicationContext());

            msg= e.recieveKey(room, chat.message);

            chat.message= msg;
            mChatRecyclerAdapter.add(chat);
            mRecyclerViewChat.smoothScrollToPosition(mChatRecyclerAdapter.getItemCount() - 1);

           Log.d("inside catch 1:", ex.getMessage()) ;

        }



        if (mChatRecyclerAdapter == null) {
            mChatRecyclerAdapter = new ChatRecyclerAdapter(new ArrayList<Chat>());
            mRecyclerViewChat.setAdapter(mChatRecyclerAdapter);
        }

    }

    @Override
    public void onGetMessagesFailure(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Subscribe
    public void onPushNotificationEvent(PushNotificationEvent pushNotificationEvent) {
        if (mChatRecyclerAdapter == null || mChatRecyclerAdapter.getItemCount() == 0) {
            mChatPresenter.getMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(),
                    pushNotificationEvent.getUid());
        }
    }
}