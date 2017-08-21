/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.ResultCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.firebase.udacity.friendlychat.dao.MessageDao;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 123;
    private static final int RC_PHOTO_PICKER =  2;
    private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";

   // private ListView mMessageListView;
    private RecyclerView recyclerView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private RelativeLayout relativeLayout;

    private String mUsername;
    private List<Message> messages;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseRef;
    private ChildEventListener mChildEventListener;

    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhotosStorageReference;

    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;

    private FirebaseUser mFirebaseUser;

    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();

        mProgressDialog = new ProgressDialog(this);

        mMessagesDatabaseRef = mFirebaseDatabase.getReference().child("messages");
        mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");


        mUsername = ANONYMOUS;

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        //mMessageListView = (ListView) findViewById(R.id.messageListView);
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);

        // Initialize message ListView and its adapter
        messages = new ArrayList<>();

        mMessageAdapter = new MessageAdapter(messages);



        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
       // mLayoutManager.scrollToPosition(recyclerView.getBottom());
        recyclerView.setLayoutManager(mLayoutManager);

        recyclerView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        auth = FirebaseAuth.getInstance();

        mFirebaseUser = auth.getCurrentUser();

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});



        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Send messages on click
                Message message = new Message(mMessageEditText.getText().toString(), mUsername, null);
               // mesageDao.write(message);
                mMessagesDatabaseRef.push().setValue(message);

                // Clear input box
                mMessageEditText.setText("");
            }
        });

     //   mesageDao.updateList();



        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    //already signed in
                    onSignedInInitialize(user.getDisplayName());
                    //showSnackbar("You are signed in");
                }else{
                    //not signed in
                    onSignedOutInitialize();
                    signIn();
                }
            }
        };

    }

    private void onSignedOutInitialize() {
        mUsername = ANONYMOUS;
        if(mChildEventListener != null){
            mMessagesDatabaseRef.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }


    }

    private void onSignedInInitialize(String displayName) {
        mUsername = displayName;
        if(mChildEventListener == null){
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {


                    messages.add(dataSnapshot.getValue(Message.class));
                    // mMessageAdapter.add(msg);
                    recyclerView.scrollToPosition(messages.size()-1);
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
            };
            mMessagesDatabaseRef.addChildEventListener(mChildEventListener);
        }

    }



    private void signIn() {
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(
                                Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                        new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                        .setIsSmartLockEnabled(false)
                        .build(),
                RC_SIGN_IN);
    }
   // compile 'com.squareup.picasso:picasso:2.5.2'
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
     /*
        if(requestCode == RC_PHOTO_PICKER && resultCode == ResultCodes.OK){
            mProgressDialog.setMessage("Uploading...");
            mProgressDialog.show();

            if(data != null){
                final Uri uri = data.getData();
                StorageReference filePath = mChatPhotosStorageReference.child("chat_photos").child(uri.getLastPathSegment());
                filePath.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        showToast("Ubacio sam sliku");
                        mProgressDialog.dismiss();
                    }
                });

            }

        }
    */

        if(requestCode == RC_PHOTO_PICKER && resultCode == ResultCodes.OK){
            Uri selectedImageUri = data.getData();

            mProgressDialog.setMessage("Uploading...");
            mProgressDialog.show();

            StorageReference photRef =
                    mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
            photRef.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    Message msg = new Message(null, mUsername, downloadUrl.toString());
                    mMessagesDatabaseRef.push().setValue(msg);
                    mProgressDialog.dismiss();
                    showToast("Ubacio sam sliku");
                }
            });
        }
        if(requestCode == RC_SIGN_IN){
            // Successfully signed in
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if(resultCode == ResultCodes.OK){

               // showSnackbar("You are signed in");

            }else if(resultCode == ResultCodes.CANCELED) {
                showToast("Some shit happened");
                finish();
            }


        }
    }

    private void putImageInStorage(StorageReference storageRef, Uri selectedImageUri, final String key) {
        storageRef.putFile(selectedImageUri).addOnCompleteListener(MainActivity.this,
                new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                            Message msg =
                                    new Message(null, mUsername,
                                            task.getResult().getDownloadUrl().toString());
                            mMessagesDatabaseRef.push().setValue(msg);
                        }
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(authStateListener != null){
            auth.removeAuthStateListener(authStateListener);
        }

        onSignedOutInitialize();
    }

    public void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    public void showSnackbar(String msg){
        Snackbar.make(relativeLayout, msg, Snackbar.LENGTH_SHORT).show();
    }
}
