package com.google.firebase.udacity.friendlychat.dao;

import android.app.Application;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.udacity.friendlychat.Message;
import com.google.firebase.udacity.friendlychat.MessageAdapter;
import com.google.firebase.udacity.friendlychat.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nemus on 09-Aug-17.
 */

public class MessageDao extends Application {
    private FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();
    private DatabaseReference mMessagesDatabaseRef = mFirebaseDatabase.getReference().child("messages");;

    private Context context;
    private List<Message> messages = new ArrayList<>();
    private  MessageAdapter mMessageAdapter = new MessageAdapter(messages);


    private RecyclerView recyclerView;



    public void write(Message message){
        mMessagesDatabaseRef.push().setValue(message);

    }

    public void updateList(){

        mMessagesDatabaseRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                messages.add(dataSnapshot.getValue(Message.class));

                mMessageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Message msg = dataSnapshot.getValue(Message.class);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
