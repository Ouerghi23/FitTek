package com.example.tekfit.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tekfit.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


public class CreatePostActivity extends AppCompatActivity
{

    // Barre d'outils en haut de l'écran
    private Toolbar mainToolBar;
    private CircleImageView  toolbarUserImage;

    // Boîte de dialogue pour afficher la progression (chargement)
    private ProgressDialog loadingbar;

    // Image utilisateur dans la page de création
    private CircleImageView userImage;
    private TextView userName;

    // Images du post (jusqu'à 3)
    private ImageView firstImage, secondImage, thirdImage;
    // Zone de texte pour la description du post
    private EditText postDescription;
    // Boutons flottants pour ajouter une image et pour finaliser le post
    private FloatingActionButton addImageButton, postCompleteButton;

    // Constante pour ouvrir la galerie d'images
    private static final int Gallery_Pick = 1;

    // URI des images sélectionnées
    private Uri firstImageUri, secondImageUri, thirdImageUri;

    // Variables pour stocker les informations du post et utilisateur
    private String retrieveUserName, retrieveUserImage, imageType, description="", saveCurrentDate, saveCurrentTime, postRandomName,
            firstImageDownloadUrl, secondImageDownloadUrl, thirdImageDownloadUrl;

    // Variables pour modifier un post existant
    private String editPostDescription, editPostFirstImage, editPostSecondImage, editPostThirdImage;

    // Références Firebase pour le stockage et la base de données
    private StorageReference storageReference;
    private DatabaseReference userDatabaseReference, postDatabaseReference;

    // Authentification Firebase
    private FirebaseAuth firebaseAuth;

    // ID utilisateur courant
    String currentUserID;

    // Variables pour gérer la provenance de l'intent (création ou modification)
    String intentFrom, intentPostID;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Initialisation Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        storageReference = FirebaseStorage.getInstance().getReference();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        postDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Posts");

        // Configuration de la toolbar avec titre et bouton retour
        mainToolBar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mainToolBar);
        getSupportActionBar().setTitle("Create Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Image utilisateur dans la toolbar (cachée ici)
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Récupération de l'intent pour savoir si on crée ou modifie un post
        Intent intent = getIntent();
        intentFrom = intent.getExtras().getString("intentFrom");
        if(intentFrom.equals("FeedFragmentToEditPost") || intentFrom.equals("ProfileActivityToEditPost"))
        {
            intentPostID = intent.getExtras().getString("intentPostID");
        }

        // Liaison des vues XML avec les variables Java
        userImage = (CircleImageView) findViewById(R.id.createpost_profile_image);
        userName = (TextView) findViewById(R.id.createpost_username);

        postDescription = (EditText) findViewById(R.id.createpost_description_text);
        addImageButton = (FloatingActionButton) findViewById(R.id.createpost_add_button);
        postCompleteButton = (FloatingActionButton) findViewById(R.id.createpost_post_complete_button);
        firstImage = (ImageView) findViewById(R.id.createpost_first_image);
        secondImage = (ImageView) findViewById(R.id.createpost_second_image);
        thirdImage = (ImageView) findViewById(R.id.createpost_third_image);
        loadingbar = new ProgressDialog(this);

        // Si on vient modifier un post, on ajuste l'interface
        if(intentFrom.equals("FeedFragmentToEditPost") || intentFrom.equals("ProfileActivityToEditPost"))
        {
            getSupportActionBar().setTitle("Edit Post");
            addImageButton.setVisibility(View.GONE); // pas d'ajout d'image en mode édition

            if(!TextUtils.isEmpty(intentPostID))
            {
                LoadPostDetailsToEditPage(); // charger les détails du post existant
            }
        }

        // Si on vient de la page profil pour créer un post, ouvrir clavier automatiquement
        if(intentFrom.equals("ProfileActivityToCreatePost"))
        {
            postDescription.requestFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(postDescription, InputMethodManager.SHOW_IMPLICIT);
        }

        // Charger les infos utilisateur dans l'interface
        SetUserDetails();

        // Action du bouton "poster" : selon le cas, valider et sauvegarder le post ou la modification
        postCompleteButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(intentFrom.equals("FeedFragmentToEditPost") || intentFrom.equals("ProfileActivityToEditPost"))
                {
                    ValidateAndSaveEditedPost();
                }
                else
                {
                    ValidatePostInformation();
                }
            }
        });

        // Action du bouton pour ajouter une image : ouvrir la galerie si possible, max 3 images
        addImageButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(firstImageUri == null)
                {
                    imageType = "first";
                    OpenGallery();
                }
                else if(secondImageUri == null)
                {
                    imageType = "second";
                    OpenGallery();
                }
                else if(thirdImageUri == null)
                {
                    imageType = "third";
                    OpenGallery();
                }
                else
                {
                    Toast.makeText(CreatePostActivity.this, "You can only add a maximum of 3 photos.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    // Charger les informations de l'utilisateur courant (image, nom) depuis Firebase Realtime Database
    private void SetUserDetails()
    {
        userDatabaseReference.child(currentUserID).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    if(dataSnapshot.hasChild("userimage"))
                    {
                        retrieveUserImage = dataSnapshot.child("userimage").getValue().toString();
                        if(!TextUtils.isEmpty(retrieveUserImage))
                        {
                            Picasso.get().load(retrieveUserImage).placeholder(R.drawable.default_user_image).into(userImage);
                        }
                    }
                    if(dataSnapshot.hasChild("username"))
                    {
                        retrieveUserName = dataSnapshot.child("username").getValue().toString();
                        if(!TextUtils.isEmpty(retrieveUserName))
                        {
                            userName.setText(retrieveUserName);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
                // En cas d'erreur lors de la lecture Firebase
            }
        });

    }


    // Valide et sauvegarde la modification d'un post existant
    private void ValidateAndSaveEditedPost()
    {
        editPostDescription = postDescription.getText().toString();

        // Si post vide (pas d'images ni description)
        if(TextUtils.isEmpty(editPostFirstImage) && TextUtils.isEmpty(editPostSecondImage) && TextUtils.isEmpty(editPostThirdImage)
                && TextUtils.isEmpty(postDescription.getText().toString().trim()))
        {
            Toast.makeText(this, "Post is empty", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // Afficher barre de chargement "Saving..."
            loadingbar = new ProgressDialog(this);
            String ProgressDialogMessage="Saving...";
            SpannableString spannableMessage=  new SpannableString(ProgressDialogMessage);
            spannableMessage.setSpan(new RelativeSizeSpan(1.3f), 0, spannableMessage.length(), 0);
            loadingbar.setMessage(spannableMessage);
            loadingbar.show();
            loadingbar.setCanceledOnTouchOutside(false);
            loadingbar.setCancelable(false);

            // Mise à jour de la description du post dans la base de données
            HashMap postMap = new HashMap();
            postMap.put("postdescription",editPostDescription);

            postDatabaseReference.child(intentPostID).updateChildren(postMap).addOnCompleteListener(new OnCompleteListener()
            {
                @Override
                public void onComplete(@NonNull Task task)
                {
                    if(task.isSuccessful())
                    {
                        // Redirection selon provenance
                        if(intentFrom.equals("FeedFragmentToEditPost"))
                        {
                            SendUserToMainPage();
                        }
                        else
                        {
                            SendUserToProfilePage();
                        }
                        Toast.makeText(CreatePostActivity.this, "Post is updated succesfully.", Toast.LENGTH_SHORT).show();
                        loadingbar.dismiss();
                    }
                    else
                    {
                        String msg = task.getException().getMessage();
                        Toast.makeText(CreatePostActivity.this, "Error : "+msg, Toast.LENGTH_SHORT).show();
                        loadingbar.dismiss();
                    }
                }
            });
        }
    }


    // Charger les détails d'un post à modifier dans l'interface
    private void LoadPostDetailsToEditPage()
    {
        postDatabaseReference.child(intentPostID).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    editPostDescription = dataSnapshot.child("postdescription").getValue().toString();

                    // Récupérer les URLs des images
                    editPostFirstImage = "";
                    editPostSecondImage = "";
                    editPostThirdImage = "";

                    if(dataSnapshot.hasChild("firstimage"))
                    {
                        editPostFirstImage = dataSnapshot.child("firstimage").getValue().toString();
                        Picasso.get().load(editPostFirstImage).into(firstImage);
                    }
                    if(dataSnapshot.hasChild("secondimage"))
                    {
                        editPostSecondImage = dataSnapshot.child("secondimage").getValue().toString();
                        Picasso.get().load(editPostSecondImage).into(secondImage);
                    }
                    if(dataSnapshot.hasChild("thirdimage"))
                    {
                        editPostThirdImage = dataSnapshot.child("thirdimage").getValue().toString();
                        Picasso.get().load(editPostThirdImage).into(thirdImage);
                    }

                    // Remplir la description
                    postDescription.setText(editPostDescription);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {

            }
        });
    }


    // Validation des informations du post avant de sauvegarder un nouveau post
    private void ValidatePostInformation()
    {
        description = postDescription.getText().toString();

        // Vérifier qu'il y ait au moins une image ou une description
        if(firstImageUri == null && secondImageUri == null && thirdImageUri == null && TextUtils.isEmpty(description.trim()))
        {
            Toast.makeText(this, "Post is empty", Toast.LENGTH_SHORT).show();
        }
        else
        {
            // Sauvegarder la date et l'heure actuelle
            Calendar calForDate = Calendar.getInstance();
            SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
            saveCurrentDate = currentDate.format(calForDate.getTime());

            SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
            saveCurrentTime = currentTime.format(calForDate.getTime());

            // Création d'un identifiant aléatoire pour le post
            postRandomName = currentUserID + saveCurrentDate + saveCurrentTime;

            // Affichage barre de progression "Posting..."
            loadingbar = new ProgressDialog(this);
            String ProgressDialogMessage="Posting...";
            SpannableString spannableMessage=  new SpannableString(ProgressDialogMessage);
            spannableMessage.setSpan(new RelativeSizeSpan(1.3f), 0, spannableMessage.length(), 0);
            loadingbar.setMessage(spannableMessage);
            loadingbar.show();
            loadingbar.setCanceledOnTouchOutside(false);
            loadingbar.setCancelable(false);

            // En fonction du nombre d'images, on les upload dans Firebase Storage puis on enregistre les URLs dans la base de données
            if(firstImageUri != null && secondImageUri != null && thirdImageUri != null)
            {
                UploadFirstImage();
            }
            else if(firstImageUri != null && secondImageUri != null)
            {
                UploadFirstImage();
            }
            else if(firstImageUri != null)
            {
                UploadFirstImage();
            }
            else
            {
                // Pas d'image, seulement description
                SavePostInfoToDatabase();
            }
        }
    }


    // Ouvrir la galerie pour choisir une image
    private void OpenGallery()
    {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, Gallery_Pick);
    }


    // Recevoir le résultat de la sélection d'image dans la galerie
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == Gallery_Pick && resultCode == RESULT_OK && data != null)
        {
            Uri imageUri = data.getData();

            if(imageType.equals("first"))
            {
                firstImageUri = imageUri;
                firstImage.setImageURI(firstImageUri);
            }
            else if(imageType.equals("second"))
            {
                secondImageUri = imageUri;
                secondImage.setImageURI(secondImageUri);
            }
            else if(imageType.equals("third"))
            {
                thirdImageUri = imageUri;
                thirdImage.setImageURI(thirdImageUri);
            }
        }
    }


    // Upload de la première image vers Firebase Storage
    private void UploadFirstImage()
    {
        StorageReference filePath = storageReference.child("Post Images").child(firstImageUri.getLastPathSegment() + postRandomName + ".jpg");

        filePath.putFile(firstImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
        {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                // Récupérer l'URL de téléchargement de la première image
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                {
                    @Override
                    public void onSuccess(Uri uri)
                    {
                        firstImageDownloadUrl = uri.toString();

                        // En fonction du nombre d'images, poursuivre l'upload
                        if(secondImageUri != null)
                        {
                            UploadSecondImage();
                        }
                        else
                        {
                            secondImageDownloadUrl = "";
                            thirdImageDownloadUrl = "";
                            SavePostInfoToDatabase();
                        }
                    }
                });
            }
        });
    }


    // Upload de la deuxième image
    private void UploadSecondImage()
    {
        StorageReference filePath = storageReference.child("Post Images").child(secondImageUri.getLastPathSegment() + postRandomName + ".jpg");

        filePath.putFile(secondImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
        {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                {
                    @Override
                    public void onSuccess(Uri uri)
                    {
                        secondImageDownloadUrl = uri.toString();

                        if(thirdImageUri != null)
                        {
                            UploadThirdImage();
                        }
                        else
                        {
                            thirdImageDownloadUrl = "";
                            SavePostInfoToDatabase();
                        }
                    }
                });
            }
        });
    }


    // Upload de la troisième image
    private void UploadThirdImage()
    {
        StorageReference filePath = storageReference.child("Post Images").child(thirdImageUri.getLastPathSegment() + postRandomName + ".jpg");

        filePath.putFile(thirdImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>()
        {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot)
            {
                taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                {
                    @Override
                    public void onSuccess(Uri uri)
                    {
                        thirdImageDownloadUrl = uri.toString();
                        SavePostInfoToDatabase();
                    }
                });
            }
        });
    }


    // Enregistrer toutes les informations du post (images, description, date, utilisateur) dans Firebase Realtime Database
    private void SavePostInfoToDatabase()
    {
        HashMap postMap = new HashMap();

        postMap.put("userid", currentUserID);
        postMap.put("postdescription", description);
        postMap.put("date", saveCurrentDate);
        postMap.put("time", saveCurrentTime);
        postMap.put("postrandomname", postRandomName);

        if(firstImageDownloadUrl != null && !firstImageDownloadUrl.isEmpty())
        {
            postMap.put("firstimage", firstImageDownloadUrl);
        }
        if(secondImageDownloadUrl != null && !secondImageDownloadUrl.isEmpty())
        {
            postMap.put("secondimage", secondImageDownloadUrl);
        }
        if(thirdImageDownloadUrl != null && !thirdImageDownloadUrl.isEmpty())
        {
            postMap.put("thirdimage", thirdImageDownloadUrl);
        }

        // Sauvegarder dans la base de données sous l'id postRandomName
        postDatabaseReference.child(postRandomName).updateChildren(postMap).addOnCompleteListener(new OnCompleteListener()
        {
            @Override
            public void onComplete(@NonNull Task task)
            {
                if(task.isSuccessful())
                {
                    loadingbar.dismiss();
                    Toast.makeText(CreatePostActivity.this, "Post published successfully!", Toast.LENGTH_SHORT).show();
                    SendUserToMainPage();
                }
                else
                {
                    loadingbar.dismiss();
                    String message = task.getException().getMessage();
                    Toast.makeText(CreatePostActivity.this, "Error occurred: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    // Redirection vers la page principale (ex: fil d'actualité)
    private void SendUserToMainPage()
    {
        Intent mainIntent = new Intent(CreatePostActivity.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }


    // Redirection vers la page profil
    private void SendUserToProfilePage()
    {
        Intent profileIntent = new Intent(CreatePostActivity.this, PostActivity.class);
        profileIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(profileIntent);
        finish();
    }

}
