package com.example.chatapp.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.Adapters.TopStatusAdapter;
import com.example.chatapp.Models.UserStatus;
import com.example.chatapp.R;
import com.example.chatapp.Models.User;
import com.example.chatapp.Adapters.UsersAdapter;
import com.example.chatapp.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.*;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.example.chatapp.Models.Status;
public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    FirebaseDatabase database;
    FirebaseStorage storage;
    UsersAdapter usersAdapter;
    ArrayList<User> users;
    boolean status_flag;
    User user;

    TopStatusAdapter statusAdapter;
    ArrayList<UserStatus> userStatuses;

    ProgressDialog dialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        database = FirebaseDatabase.getInstance();
        users = new ArrayList<>();
        userStatuses = new ArrayList<>();

        //Reading logged in users profile details
        //to e used for status when photo select from bottom navigation view
        database.getReference().child("users").child(FirebaseAuth.getInstance().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                 user = snapshot.getValue(User.class);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        //get statuses
        //sabke status milenge jitne users he

        database.getReference().child("stories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
             if(snapshot.exists())
             {
                 userStatuses.clear();
                 for(DataSnapshot storySnapshot : snapshot.getChildren()) {
                     //sab users stories ki array banke agai
                     //since it is in form kof HasnMap get karne ka tareek alag
                     status_flag = false;
                     UserStatus status = new UserStatus();
                     status.setName(storySnapshot.child("name").getValue(String.class));
                     status.setProfileImage(storySnapshot.child("profileImage").getValue(String.class));
                     status.setLastUpdated(storySnapshot.child("lastUpdated").getValue(Long.class));


                     //ab us profile ki individual photos lenge
                     ArrayList<Status> statuses = new ArrayList<>();
                     for(DataSnapshot statusSnapshot : storySnapshot.child("statuses").getChildren()) {

                         Status sampleStatus = statusSnapshot.getValue(Status.class);
                         //TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
                         if(new Date().getTime() - sampleStatus.getTimeStamp() > TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS))
                         {
                             DatabaseReference ref = statusSnapshot.getRef();
                             ref.removeValue();
                         }
                         else {
                             statuses.add(sampleStatus);
                             status_flag=true;
                         }
                     }

                     status.setStatuses(statuses);
                     userStatuses.add(status);
                 }
                 binding.statusList.hideShimmerAdapter();
                 if(status_flag!=true)
                     hideMargin(true);
                 else
                     hideMargin(false);
                 statusAdapter.notifyDataSetChanged();
             }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading image");
        dialog.setCancelable(false);


        usersAdapter = new UsersAdapter(this,users);
        binding.recyclerView.setAdapter(usersAdapter);
        binding.recyclerView.showShimmerAdapter();//hide just before notifying data set changed


        statusAdapter = new TopStatusAdapter(this, userStatuses);
//        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.HORIZONTAL);
        binding.statusList.setLayoutManager(layoutManager);
        binding.statusList.setAdapter(statusAdapter);
        binding.statusList.showShimmerAdapter();

        database.getReference().child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                users.clear();
                for (DataSnapshot snapshot1 : snapshot.getChildren()) {
                    User user = snapshot1.getValue(User.class);
                    //taki khudse na chats ho
                     if(!user.getUid().equals(FirebaseAuth.getInstance().getUid()))
                    users.add(user);
                }
                binding.recyclerView.hideShimmerAdapter();
                usersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }
        );


        binding.bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.status:
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        startActivityForResult(intent, 75);
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data !=null && data.getData()!=null && requestCode == 75 && resultCode ==  RESULT_OK)
        {

            storage = FirebaseStorage.getInstance();
            // current time k name se image store kar rahe he
            StorageReference reference = storage.getReference().child("status").child(""+ new Date().getTime() );
             reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                 @Override
                 public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                     //show dialog
                     dialog.show();

                     if(task.isSuccessful())
                     {
                         reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                             @Override
                             public void onSuccess(Uri uri) {
                                 UserStatus userStatus = new UserStatus();
                                 userStatus.setName(user.getName());
                                 userStatus.setProfileImage(user.getProfileImage());
                                 userStatus.setLastUpdated(new Date().getTime());

                                 //ye tab use karo jB TUM chaho k dobara ye lines execute ho to
                                 //databsde update ho
                                 // jan naya status dalega tan name and profile image same hogi bas last updated change hojaega
                                 HashMap<String, Object> obj = new HashMap<>();
                                 obj.put("name", userStatus.getName());
                                 obj.put("profileImage", userStatus.getProfileImage());
                                 obj.put("lastUpdated", userStatus.getLastUpdated());

                                 String imageUrl = uri.toString();
                                 Status status = new Status(imageUrl, userStatus.getLastUpdated());


                                 //ye update hota rahega jese jese naye status aega
                                 database.getReference()
                                         .child("stories")
                                         .child(FirebaseAuth.getInstance().getUid())
                                         .updateChildren(obj);

                                 //each photo / status will be added to user profile
                                 database.getReference().child("stories")
                                         .child(FirebaseAuth.getInstance().getUid())
                                         .child("statuses")
                                         .push()//creates unique key field
                                         .setValue(status);
                                 dialog.dismiss();
                             }
                         });
                     }
                 }
             });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                Toast.makeText(this, "Search clicked.", Toast.LENGTH_SHORT).show();
                break;

            case R.id.profile:
                Intent intent = new Intent (MainActivity.this,UpdateProfile.class);
                startActivity(intent);


            case R.id.settings:
                Toast.makeText(this, "Settings Clicked.", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.topmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void hideMargin(boolean b)
    {
        if(b)
        binding.view3.setVisibility(View.GONE);
        else binding.view3.setVisibility(View.VISIBLE);
    }

}