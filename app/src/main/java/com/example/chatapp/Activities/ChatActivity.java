package com.example.chatapp.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import com.example.chatapp.Adapters.MessagesAdapter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.Models.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    ActivityChatBinding binding;
    MessagesAdapter adapter;
    ArrayList<Message> messages;
    File photoFile = null;
    String senderRoom, receiverRoom;
    FirebaseDatabase database;
    FirebaseStorage storage;
    Uri photoURI;
    String senderUid;
    String receiverUid;
    Uri filePath;
    String mCurrentPhotoPath;
    Date date;

    Message myMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        date = new Date();
        messages = new ArrayList<>();
        storage= FirebaseStorage.getInstance();

        String name = getIntent().getStringExtra("name");
        //jiske profile pe tap kiye
         receiverUid = getIntent().getStringExtra("uid");
         senderUid = FirebaseAuth.getInstance().getUid();

        senderRoom = senderUid + receiverUid;
        receiverRoom = receiverUid + senderUid;

        adapter = new MessagesAdapter(this, messages, senderRoom, receiverRoom);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setStackFromEnd(true);
        binding.recyclerView.setLayoutManager(manager);
        binding.recyclerView.setAdapter(adapter);
        database = FirebaseDatabase.getInstance();


        binding.attachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 45);
            }
        });

        binding.camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (hasCamera()) {
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    // Ensure that there's a camera activity to handle the intent
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go

                        try {
                            photoFile = createImageFile();
                        } catch (IOException ex) {
                            // Error occurred while creating the File

                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {

                           // filePath = Uri.fromFile(photoFile);
                            photoURI = FileProvider.getUriForFile(ChatActivity.this,
                                    getApplicationContext().getPackageName() + ".provider",
                                    photoFile);

                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    photoURI);
                          //  Log.e("Uri ", photoURI + "");
                            startActivityForResult(takePictureIntent, 0);
                        }
                    }
                }
            }
        });


        database.getReference().child("chats")
                .child(senderRoom)
                .child("messages")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        messages.clear();
                        for(DataSnapshot snapshot1 : snapshot.getChildren()) {
                            Message message = snapshot1.getValue(Message.class);
                            // jaha data stored he uski key
                            //jisko hamne assign kiya the push().getKey();
                            message.setMessageId(snapshot1.getKey());
                            messages.add(message);
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        binding.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String messageTxt = binding.messageBox.getText().toString();

                Date date = new Date();
                Message message = new Message(messageTxt, senderUid, date.getTime());
                binding.messageBox.setText("");

                //Instead database hamare liye koi unique key se data add kare
                // ham chahte he ham khud koi random key generate karke data associate karde
                //fayda ham jab msg beheje to wo
                //sender room and receiver room me same key se store ho
                // taki jab hama yaha emoji add kare ,to ham us wale data k unique id key se
                //corresposnif receiver room k msg data me bhi emoji add karde
                String randomKey = database.getReference().push().getKey();


                //ye tab use karo jB TUM chaho k dobara ye lines execute ho to
                //databsde update ho
                HashMap<String, Object> lastMsgObj = new HashMap<>();
                lastMsgObj.put("lastMsg", message.getMessage());
                lastMsgObj.put("lastMsgTime", date.getTime());

                database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);


              /*  database.getReference().
                        child("chats").
                        child(senderRoom).
                        child("messages").
                        push().//builds a unique node*/

                // yaha push() ek aur random child me data dalta tha
                // yaha ham random key nam ki child me data dal rahe he
                database.getReference().child("chats")
                        .child(senderRoom)
                        .child("messages")
                        .child(randomKey).
                        setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                       //if succesfuuly uploaded in database
                        //to ye message receiver k room me bhi bhej do
                        database.getReference().child("chats")
                                .child(receiverRoom)
                                .child("messages")
                                .child(randomKey)
                                .setValue(message).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //if succesfuuly uploaded in database
                                //to ye message receiver k room me bhi bhej do

                            }
                        });



                    }
                });
            }
        });



        getSupportActionBar().setTitle(name);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( (requestCode == 45  )&&  resultCode == RESULT_OK)
        {

            StorageReference reference =  storage.getReference().child("imageChats")
                .child(senderRoom)
                .child("messages")
                .child(""+new Date().getTime());
            reference.putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            final Uri downloadUrl = uri;
                            myMessage = new Message(date.getTime(),downloadUrl.toString(), senderUid) ;
                            String randomKey = database.getReference().push().getKey();


                            //ye tab use karo jB TUM chaho k dobara ye lines execute ho to
                            //databsde update ho
                            HashMap<String, Object> lastMsgObj = new HashMap<>();
                            lastMsgObj.put("lastMsg", "photo");
                            lastMsgObj.put("lastMsgTime", date.getTime());

                            database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                            database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                            database.getReference().child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(randomKey).
                                    setValue(myMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //if succesfuuly uploaded in database
                                    //to ye message receiver k room me bhi bhej do
                                    database.getReference().child("chats")
                                            .child(receiverRoom)
                                            .child("messages")
                                            .child(randomKey)
                                            .setValue(myMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //if succesfuuly uploaded in database
                                            //to ye message receiver k room me bhi bhej do

                                        }
                                    });

                                }
                            });
                        }
                    });
                }
            });

        }

        if( requestCode==0 && resultCode==RESULT_OK)
        {
            Log.e("hiiiiii",mCurrentPhotoPath);
            BitmapFactory.Options options = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, options);

         //Bitmap   bitmap = ((BitmapDrawable) mCurrentPhotoPath.getDrawable().getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
         //   bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] imageBytes = baos.toByteArray();
            StorageReference reference = storage.getReference().child("imageChats")
                    .child(senderRoom)
                    .child("messages")
                    .child(""+new Date().getTime());
            UploadTask uploadTask = reference.putBytes(imageBytes);
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Log.e("UploadComplete", "Image successfully uploaded URL: %s");
                    final Task<Uri> downloadUrl1 = taskSnapshot.getStorage().getDownloadUrl();
                    downloadUrl1.addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Uri downloadUrl = uri;

                            myMessage = new Message(date.getTime(),downloadUrl.toString(), senderUid) ;
                            String randomKey = database.getReference().push().getKey();


                            //ye tab use karo jB TUM chaho k dobara ye lines execute ho to
                            //databsde update ho
                            HashMap<String, Object> lastMsgObj = new HashMap<>();
                            lastMsgObj.put("lastMsg", "photo");
                            lastMsgObj.put("lastMsgTime", date.getTime());

                            database.getReference().child("chats").child(senderRoom).updateChildren(lastMsgObj);
                            database.getReference().child("chats").child(receiverRoom).updateChildren(lastMsgObj);

                            database.getReference().child("chats")
                                    .child(senderRoom)
                                    .child("messages")
                                    .child(randomKey).
                                    setValue(myMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //if succesfuuly uploaded in database
                                    //to ye message receiver k room me bhi bhej do
                                    database.getReference().child("chats")
                                            .child(receiverRoom)
                                            .child("messages")
                                            .child(randomKey)
                                            .setValue(myMessage).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //if succesfuuly uploaded in database
                                            //to ye message receiver k room me bhi bhej do

                                        }
                                    });

                                }
                            });
                        }
                    });

                }
            });
        }

    }

    // create image file method used in above function
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        String storageDir = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES) + "/picupload";
        File dir = new File(storageDir);
        if (!dir.exists())
            dir.mkdir();

        File image = new File(storageDir + "/" + imageFileName + ".jpg");

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        Log.e("photo path = " , mCurrentPhotoPath+dir.exists());

        return image;
    }

    // to check whether camera is present or not
    private boolean hasCamera()
    {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }
}
