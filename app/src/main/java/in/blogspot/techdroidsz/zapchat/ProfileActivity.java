package in.blogspot.techdroidsz.zapchat;

import android.app.ProgressDialog;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    private ImageView mProfileImage;
    private TextView mProfileName,mProfileStatus,mProfileFriendsCount;
    private Button mProfileSendReqBtn, mDeclineBtn;


    private DatabaseReference mUsersDatabase;

    private ProgressDialog mProgressDialog;

    private FirebaseUser mCurrent_user;

    private DatabaseReference mFriendReqDatabase;
    private DatabaseReference mFriendDatabase;
    private DatabaseReference mNotificationDatabase;

    private String mCurrent_state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        final String user_id=getIntent().getStringExtra("user_id");

        mUsersDatabase= FirebaseDatabase.getInstance().getReference().child("Users").child(user_id);

        mFriendReqDatabase=FirebaseDatabase.getInstance().getReference().child("Friend_req");
        mFriendDatabase=FirebaseDatabase.getInstance().getReference().child("Friends");
        mNotificationDatabase=FirebaseDatabase.getInstance().getReference().child("notifications");

        mCurrent_user= FirebaseAuth.getInstance().getCurrentUser();

        mProfileImage=(ImageView) findViewById(R.id.profile_dp);
        mProfileName=(TextView) findViewById(R.id.profile_display_name);
        mProfileStatus=(TextView) findViewById(R.id.profile_status);
        mProfileFriendsCount=(TextView) findViewById(R.id.profile_totalFriends);
        mProfileSendReqBtn=(Button) findViewById(R.id.profile_send_req_btn);
        mDeclineBtn=(Button) findViewById(R.id.profile_decline_req_btn);

        mCurrent_state="not_friends";

        mProgressDialog=new ProgressDialog(this);
        mProgressDialog.setTitle("Loading User Data");
        mProgressDialog.setMessage("Please wait while we load the user data.");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();




        mUsersDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                String display_name=dataSnapshot.child("name").getValue().toString();
                String status=dataSnapshot.child("status").getValue().toString();
                String image=dataSnapshot.child("image").getValue().toString();

                mProfileName.setText(display_name);
                mProfileStatus.setText(status);


                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.default_dp).into(mProfileImage);
                mDeclineBtn.setVisibility(View.INVISIBLE);
                mDeclineBtn.setEnabled(false);


                //---------------------------FRIENDS LIST?REQUEST FEATURE----------------------------

                mFriendReqDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(user_id)){
                            String req_type=dataSnapshot.child(user_id).child("request_type").getValue().toString();

                            if(req_type.equals("received")){

                                mCurrent_state="req_received";
                                mProfileSendReqBtn.setText("Accept Friend Request");

                                mDeclineBtn.setVisibility(View.VISIBLE);
                                mDeclineBtn.setEnabled(true);


                            }else if(req_type.equals("sent")){
                                mCurrent_state="req_sent";
                                mProfileSendReqBtn.setText("Cancel Friend Request");

                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                mDeclineBtn.setEnabled(false);



                            }

                            mProgressDialog.dismiss();
                        }else{
                            mFriendDatabase.child(mCurrent_user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    if(dataSnapshot.hasChild(user_id)){
                                        mCurrent_state="friends";
                                        mProfileSendReqBtn.setText("Unfriend this Person");

                                        mDeclineBtn.setVisibility(View.INVISIBLE);
                                        mDeclineBtn.setEnabled(false);



                                    }

                                    mProgressDialog.dismiss();

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                    mProgressDialog.dismiss();

                                }
                            });


                        }



                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mProfileSendReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCurrent_state==("not_friends")){

                    mProfileSendReqBtn.setEnabled(false);

                    //-------------------------------NOT FRIENDS STATE-------------------------------------------------------

                    mFriendReqDatabase.child(mCurrent_user.getUid()).child(user_id).child("request_type").setValue("sent").addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){

                                mFriendReqDatabase.child(user_id).child(mCurrent_user.getUid()).child("request_type").setValue("received").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                        HashMap<String,String>notificationData=new HashMap<>();
                                        notificationData.put("from",mCurrent_user.getUid());
                                        notificationData.put("type","request");


                                        mNotificationDatabase.child(user_id).push().setValue(notificationData).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                mCurrent_state="req_sent";
                                                mProfileSendReqBtn.setText("Cancel Friend Reuqest");

                                                mDeclineBtn.setVisibility(View.INVISIBLE);
                                                mDeclineBtn.setEnabled(false);


                                            }
                                        });




                                        //Toast.makeText(ProfileActivity.this,"Request Sent Successfully.",Toast.LENGTH_LONG).show();

                                    }
                                });


                            }else{
                                Toast.makeText(ProfileActivity.this,"Failed Sending Request.",Toast.LENGTH_LONG).show();
                            }
                            mProfileSendReqBtn.setEnabled(true);
                        }
                    });


                }


                //----------------------------------CANCEL REQUEST STATE------------------------------------

                if(mCurrent_state.equals("req_sent")){

                    mFriendReqDatabase.child(mCurrent_user.getUid()).child(user_id).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendReqDatabase.child(user_id).child(mCurrent_user.getUid()).removeValue().addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {

                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state="not_friends";
                                    mProfileSendReqBtn.setText("Send Friend Request");

                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                    mDeclineBtn.setEnabled(false);


                                }
                            });

                        }
                    });
                }

                //---------------------------REQ RECEIVED STATE------------------------

                if(mCurrent_state.equals("req_received")){

                    final String currentDate= DateFormat.getDateInstance().format(new Date());


                    mFriendDatabase.child(mCurrent_user.getUid()).child(user_id).setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {

                            mFriendDatabase.child(user_id).child(mCurrent_user.getUid()).setValue(currentDate).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {


                                    mProfileSendReqBtn.setEnabled(true);
                                    mCurrent_state="friends";
                                    mProfileSendReqBtn.setText("UnFriend this Person");

                                    mDeclineBtn.setVisibility(View.INVISIBLE);
                                    mDeclineBtn.setEnabled(false);

                                }
                            });
                        }
                    });
                }
            }
        });


    }
}
