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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;


//import com.google.firebase.database.FirebaseDatabase;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Result;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public  static final String friendly_msg_lenght_Key="friendly_msg_lenght";
    public  static final int RC_SIGN_IN=2;//startActivityForResult for the Sign_in
    private ListView mMessageListView;
    private MessageAdapter mMessageAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private FirebaseDatabase mfbase;
    private DatabaseReference mdr;
    private ChildEventListener mlisten;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private static final int RC_PHOTO_PICKER=1; //This is for startActivityForResult for the Photo
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mChatPhoto;//Storage reference object
    private FirebaseRemoteConfig  mFireremote ;

    private String mUsername;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;
        //Initialize Firebase components
        mfbase=FirebaseDatabase.getInstance();//Intialize Firebase component
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStorage=FirebaseStorage.getInstance();
        mFireremote=FirebaseRemoteConfig.getInstance();

        mdr=mfbase.getReference().child("messages");
        mChatPhoto=mFirebaseStorage.getReference().child("chat_photos");

        // Initialize references to views
        mProgressBar = findViewById(R.id.progressBar);
        mMessageListView =  findViewById(R.id.messageListView);
        mPhotoPickerButton =  findViewById(R.id.photoPickerButton);
        mMessageEditText =  findViewById(R.id.messageEditText);
        mSendButton = findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);
        mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);


//mMessageEditText.setText("");
        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);

                /*Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,RC_PHOTO_PICKER);*/

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
        mSendButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public  void onClick(View view){

                FriendlyMessage friendlyMessage=new FriendlyMessage(mMessageEditText.getText().toString(),mUsername,null);
                mdr.push().setValue(friendlyMessage);
                mMessageEditText.setText("");
            }
        });
      /*  mlisten=new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
FriendlyMessage mMessage=dataSnapshot.getValue(FriendlyMessage.class);
mMessageAdapter.add(mMessage);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mdr.addChildEventListener(mlisten);*/

        mAuthStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=firebaseAuth.getCurrentUser();
                /* We can use firebase UI to build the sign in flow */
                if( user != null){
                    //user is signed_in
                    onSignedIn(user.getDisplayName()) ;
                    Toast.makeText(MainActivity.this, "You're now signed in. Welcome to FriendlyChat.", Toast.LENGTH_SHORT).show();
                }else{
                    //user is signed out
                    // Choose authentication providers
                    onSignedOut();
                    List<AuthUI.IdpConfig> providers = Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),
                            new AuthUI.IdpConfig.GoogleBuilder().build());

                    // Create and launch sign-in intent
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(providers)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };
        //This is for the remote config operations setup
        FirebaseRemoteConfigSettings configSettings=new  FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(BuildConfig.DEBUG).build();// BuildConfig.DEBUG is a boolean created at build time
        mFireremote.setConfigSettingsAsync(configSettings);

        //We define the parameters for the remote config this will be done using a map object
        Map<String,Object> defaltConfigMap=new HashMap<>();
        defaltConfigMap.put(friendly_msg_lenght_Key,DEFAULT_MSG_LENGTH_LIMIT);
        mFireremote.setDefaults(defaltConfigMap);
        //This method( fetchConfig()) will try to fetch value through the servers to see if any of the config value change during you firebase project
        fetchConfig();


    }
/*This is for your onActivityResult modification Contains the result coming from your sign_in and also
    the result coming from your photo having your RC_SIGN_IN and also your RC_PHOTO_PICKER*/
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode==RC_SIGN_IN){
            if (resultCode==RESULT_OK) {
                Toast.makeText(this,"Singed In",Toast.LENGTH_SHORT).show();}
                else if(resultCode==RESULT_CANCELED){
                finish();
                Toast.makeText(this,"Singed in cancelled",Toast.LENGTH_SHORT).show();
                }
            }else if (requestCode==RC_PHOTO_PICKER  && resultCode==RESULT_OK){
            Toast.makeText(this,"IT See rt the PHOTO",Toast.LENGTH_SHORT).show();
            Uri selectedImagesUri=data.getData();
            //Get a reference to store file at chat_photo/<FILENAME>
            final StorageReference photoRef=mChatPhoto.child(selectedImagesUri.getLastPathSegment());
            //Upload file to Firebase Storage
            photoRef.putFile(selectedImagesUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    photoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                          //  DatabaseReference imagestore =FirebaseDatabase.getInstance().getReference().child("chat_photo");

                            FriendlyMessage friendlyMessage=new FriendlyMessage(null,mUsername,String.valueOf(uri));
                           // mdr.push().setValue(friendlyMessage);
                            mdr.push().setValue(friendlyMessage);
                        }
                    });
                   /* StorageMetadata downloadUrl =taskSnapshot.getMetadata();
                    FriendlyMessage friendlyMessage=new FriendlyMessage(null,mUsername,downloadUrl.toString());
                    mdr.push().setValue(friendlyMessage);*/
                }
            });

        }

    }


    private void attachDtabaseListener(){
        if (mlisten==null){


        mlisten=new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                FriendlyMessage mMessage=dataSnapshot.getValue(FriendlyMessage.class);
                mMessageAdapter.add(mMessage);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        mdr.addChildEventListener(mlisten);}
    }

    private void onSignedIn(String displayName) {
        mUsername=displayName;
       attachDtabaseListener();
    }
    private void onSignedOut() {
mUsername=ANONYMOUS;
mMessageAdapter.clear();
    }
private void detachDataReadListener() {
        if (mlisten!=null) {
            mdr.removeEventListener(mlisten);
            mlisten=null;
        }

}
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }
    @Override
    protected void onPause() {
        super.onPause() ;
        if (mAuthStateListener!=null){
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);}
        detachDataReadListener();
        mMessageAdapter.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
      mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
   public  void  fetchConfig(){
        //Specify the cache expiration time
       long  cacheExpiration=3600;
       //set the cache time to 0 when developer mode is enabled
       if (mFireremote.getInfo().getConfigSettings().isDeveloperModeEnabled()){
           //This allows use when we are debugging it to get the latest values from firebase if there are any changes
           cacheExpiration=0;
       }
       mFireremote.fetch(cacheExpiration).addOnSuccessListener(new OnSuccessListener<Void>() {
           @Override
           public void onSuccess(Void aVoid) {
               mFireremote.activateFetched();//active your parameters
               // appropriately update the edit text length
               applyRetrievedLenghtLimit();
           }
       }) .addOnFailureListener(new OnFailureListener() {
           @Override
           public void onFailure(@NonNull Exception e) {
               //onFailure will occur may be when you are offline
               Log.w(TAG,"Error",e);
               // When this is call it will update it values from the cache
               applyRetrievedLenghtLimit();

           }
       });
    }

    private void applyRetrievedLenghtLimit() {
        /* this will contain what was received from the servers */
        Long frendly_msg_length=mFireremote.getLong(friendly_msg_lenght_Key);
        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(frendly_msg_length.intValue())});
        Log.d(TAG,friendly_msg_lenght_Key+"="+frendly_msg_length);
    }


}
