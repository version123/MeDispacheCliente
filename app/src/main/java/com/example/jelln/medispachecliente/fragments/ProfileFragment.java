package com.example.jelln.medispachecliente.fragments;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.jelln.medispachecliente.R;
import com.example.jelln.medispachecliente.model.Usuarios;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;


public class ProfileFragment extends Fragment {
    CircleImageView image_profile;
    TextView username;
    DatabaseReference referece;
    FirebaseUser user;

    CircleImageView attnome;
    ImageButton cancelar, atualizar;
    EditText nomemudar;
    StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUrl;
    private StorageTask uploadTask;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        image_profile = view.findViewById(R.id.profile_image);
        username = view.findViewById(R.id.username);
        storageReference = FirebaseStorage.getInstance().getReference("uploads");
        attnome = view.findViewById(R.id.atualizarnome);
        cancelar = view.findViewById(R.id.botaocancela);
        atualizar = view.findViewById(R.id.botaoatt);
        nomemudar = view.findViewById(R.id.mudarnome);
        nomemudar.setVisibility(View.GONE);
        cancelar.setVisibility(View.GONE);
        atualizar.setVisibility(View.GONE);

        user = FirebaseAuth.getInstance().getCurrentUser();
        referece = FirebaseDatabase.getInstance().getReference("User").child(user.getUid());
        referece.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Usuarios u = dataSnapshot.getValue(Usuarios.class);
                username.setText(u.getName());
                if (u.getImageUrl() == null || getContext() == null) {
                    image_profile.setImageResource(R.drawable.ic_launcher_background);
                } else {
                    Glide.with(getContext()).load(u.getImageUrl()).into(image_profile);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        attnome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aparecer();
            }
        });

        cancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nomemudar.setVisibility(View.GONE);
                cancelar.setVisibility(View.GONE);
                atualizar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Operação cancelada", Toast.LENGTH_SHORT).show();

            }
        });
        atualizar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nomepego = nomemudar.getText().toString().trim();
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("User").child(user.getUid());
                HashMap<String, Object> hashMap = new HashMap<>();
                hashMap.put("name", nomepego);
                hashMap.put("search", nomepego.toLowerCase());
                reference.updateChildren(hashMap);
                nomemudar.setVisibility(View.GONE);
                cancelar.setVisibility(View.GONE);
                atualizar.setVisibility(View.GONE);
                username.setText(nomepego);
                Toast.makeText(getContext(), "Nome de usuário alterado", Toast.LENGTH_SHORT).show();
            }
        });
        image_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });
        return view;
    }

    private void aparecer() {
        nomemudar.setVisibility(View.VISIBLE);
        cancelar.setVisibility(View.VISIBLE);
        atualizar.setVisibility(View.VISIBLE);

    }

    private void openImage() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }


    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContext().getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage() {
        final ProgressDialog pd = new ProgressDialog(getContext());
        pd.setMessage("Upando");
        pd.show();

        if (imageUrl != null) {
            final StorageReference fileReference = storageReference.child(System.currentTimeMillis()
                    + "." + getFileExtension(imageUrl));

            uploadTask = fileReference.putFile(imageUrl);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();
                        referece = FirebaseDatabase.getInstance().getReference("User").child(user.getUid());
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("imageUrl", mUri);
                        referece.updateChildren(map);
                        pd.dismiss();
                    } else {

                        Toast.makeText(getContext(), "Failed", Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        } else {
            Toast.makeText(getContext(), "Imagem não Selecionada", Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null & data.getData() != null) {
                imageUrl = data.getData();

                if(uploadTask != null && uploadTask.isInProgress()){
                    Toast.makeText(getContext(), "Envio em progresso", Toast.LENGTH_SHORT).show();
                }else{
                    uploadImage();
                }
        }

    }
}