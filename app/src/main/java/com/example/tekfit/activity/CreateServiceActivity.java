package com.example.tekfit.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.tekfit.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateServiceActivity extends AppCompatActivity
{
    // Déclaration des variables UI
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    // Firebase Auth et références aux bases de données / stockage
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDatabaseReference, postDatabaseReference;
    private StorageReference storageReference;

    // Boutons image pour choisir les images du service
    private ImageButton firstImage, secondImage, thirdImage;

    // Champs de saisie texte
    private EditText serviceTitle, serviceSummaryDescription, serviceFullDescription, serviceSearchKeyword;

    // TextViews pour afficher les erreurs de validation
    private TextView serviceTitleError, serviceSummaryDescriptionError, serviceFullDescriptionError, serviceSearchKeywordError, serviceImageError;

    // Bouton de soumission du formulaire
    private Button submitButton;

    // Uri des images sélectionnées
    private Uri firstImageUri, secondImageUri, thirdImageUri;

    // Variables pour stocker les valeurs et infos
    String currentUserID, imageType, serviceTitleValue, serviceSummaryDescriptionValue, serviceFullDescriptionValue, serviceSearchKeywordValue,
            currentTime, currentDate, serviceRandomName, firstImageDownloadURL, secondImageDownloadURL, thirdImageDownloadURL;

    // Constante pour l'intent galerie
    private static final int Gallery_Pick = 1;

    // Barre de chargement affichée pendant l'upload
    private ProgressDialog loadingbar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_service);

        /* Initialisation de la toolbar avec titre et bouton retour */
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create Service");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        /* On masque l'image utilisateur dans la toolbar */
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        /* Initialisation Firebase */
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid(); // Récupération ID utilisateur courant
        storageReference = FirebaseStorage.getInstance().getReference(); // Référence stockage Firebase
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID); // Réf base utilisateur
        postDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Services"); // Réf base services

        /* Initialisation des éléments UI du formulaire */
        serviceTitle = (EditText) findViewById(R.id.create_service_service_title);
        serviceSummaryDescription = (EditText) findViewById(R.id.create_service_service_summary_description);
        serviceFullDescription = (EditText) findViewById(R.id.create_service_service_full_description);
        serviceSearchKeyword = (EditText) findViewById(R.id.create_service_service_search_keyword);

        firstImage = (ImageButton) findViewById(R.id.create_service_service_first_image);
        secondImage = (ImageButton) findViewById(R.id.create_service_service_second_image);
        thirdImage = (ImageButton) findViewById(R.id.create_service_service_third_image);

        submitButton = (Button) findViewById(R.id.create_service_service_submit_button);

        /* Initialisation des TextView d'erreurs */
        serviceTitleError = (TextView) findViewById(R.id.create_service_service_title_error);
        serviceSummaryDescriptionError = (TextView) findViewById(R.id.create_service_service_summary_description_error);
        serviceFullDescriptionError = (TextView) findViewById(R.id.create_service_service_full_description_error);
        serviceSearchKeywordError = (TextView) findViewById(R.id.create_service_service_search_keyword_error);
        serviceImageError = (TextView) findViewById(R.id.create_service_service_image_error);

        loadingbar = new ProgressDialog(this);

        /* Listener clic sur la première image */
        firstImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                imageType = "first"; // on définit le type d'image
                OpenGallery(); // ouvre la galerie pour sélection
            }
        });

        /* Listener clic sur la deuxième image */
        secondImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                imageType = "second";
                OpenGallery();
            }
        });

        /* Listener clic sur la troisième image */
        thirdImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                imageType = "third";
                OpenGallery();
            }
        });

        /* Listener clic sur le bouton soumettre */
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                ValidateServiceInformation(); // Validation des champs avant envoi
            }
        });

    }

    /* Méthode qui valide les informations du formulaire */
    private void ValidateServiceInformation()
    {
        // Récupération des valeurs saisies
        serviceTitleValue = serviceTitle.getText().toString();
        serviceSummaryDescriptionValue = serviceSummaryDescription.getText().toString();
        serviceFullDescriptionValue = serviceFullDescription.getText().toString();
        serviceSearchKeywordValue = serviceSearchKeyword.getText().toString().toLowerCase();

        // Affiche les erreurs si champs vides
        if(TextUtils.isEmpty(serviceTitleValue))
        {
            serviceTitleError.setVisibility(View.VISIBLE);
        }

        if(TextUtils.isEmpty(serviceSummaryDescriptionValue))
        {
            serviceSummaryDescriptionError.setVisibility(View.VISIBLE);
        }

        if(TextUtils.isEmpty(serviceFullDescriptionValue))
        {
            serviceFullDescriptionError.setVisibility(View.VISIBLE);
        }

        if(TextUtils.isEmpty(serviceSearchKeywordValue))
        {
            serviceSearchKeywordError.setVisibility(View.VISIBLE);
        }

        if(firstImageUri == null)
        {
            serviceImageError.setVisibility(View.VISIBLE);
        }

        // Si tous les champs sont valides et image sélectionnée
        if(!TextUtils.isEmpty(serviceTitleValue) && !TextUtils.isEmpty(serviceSummaryDescriptionValue) &&
                !TextUtils.isEmpty(serviceFullDescriptionValue) &&  !TextUtils.isEmpty(serviceSearchKeywordValue) && firstImageUri != null)
        {
            // Configuration de la barre de chargement
            loadingbar = new ProgressDialog(this);
            String ProgressDialogMessage="Submitting...";
            SpannableString spannableMessage=  new SpannableString(ProgressDialogMessage);
            spannableMessage.setSpan(new RelativeSizeSpan(1.3f), 0, spannableMessage.length(), 0);
            loadingbar.setMessage(spannableMessage);
            loadingbar.show();
            loadingbar.setCanceledOnTouchOutside(false);
            loadingbar.setCancelable(false);

            // Récupération de la date courante au format yyyyMMdd
            Calendar calendar1 = Calendar.getInstance();
            SimpleDateFormat currenDate = new SimpleDateFormat("yyyyMMdd");
            currentDate = currenDate.format(calendar1.getTime());

            // Récupération de l'heure courante au format HHmmss
            Calendar calendar2 = Calendar.getInstance();
            SimpleDateFormat currenTime = new SimpleDateFormat("HHmmss");
            currentTime = currenTime.format(calendar2.getTime());

            // Création d'un nom aléatoire unique basé sur date + heure
            serviceRandomName = currentDate + currentTime;

            // Upload de la première image puis récupération du lien
            StoreImageAndGetLink(firstImageUri, "first");

            // Si deuxième image sélectionnée, upload aussi
            if(secondImageUri != null)
            {
                StoreImageAndGetLink(secondImageUri, "second");
            }

            // Si troisième image sélectionnée, upload aussi
            if(thirdImageUri != null)
            {
                StoreImageAndGetLink(thirdImageUri, "third");
            }
        }
    }


    /* Méthode pour uploader une image sur Firebase Storage et récupérer son URL */
    private void StoreImageAndGetLink(Uri imageUri, final String number)
    {
        // Construction du chemin du fichier dans Firebase Storage
        final StorageReference filePath = storageReference.child("Service Images").child(imageUri.getLastPathSegment() + number + currentUserID + serviceRandomName + ".jpg");

        // Upload de l'image
        filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>()
        {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task)
            {
                if(task.isSuccessful())
                {
                    // Récupération de l'URL de téléchargement de l'image uploadée
                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>()
                    {
                        @Override
                        public void onSuccess(Uri uri)
                        {
                            // Enregistre l'URL selon le type d'image
                            if(number.equals("first"))
                            {
                                firstImageDownloadURL = uri.toString();
                            }

                            if(number.equals("second"))
                            {
                                secondImageDownloadURL = uri.toString();
                            }

                            if(number.equals("third"))
                            {
                                thirdImageDownloadURL = uri.toString();
                            }

                            // Sauvegarde les infos dans la base de données
                            SaveServiceInformationToDatabase();

                        }
                    });
                }
            }
        });
    }


    /* Méthode pour sauvegarder les informations du service dans la base de données Firebase */
    private void SaveServiceInformationToDatabase()
    {
        // Date de publication formatée
        Calendar calendar4 = Calendar.getInstance();
        SimpleDateFormat currenDate = new SimpleDateFormat("dd MMM yyyy");
        String servicePublishDate = currenDate.format(calendar4.getTime());

        // Heure de publication formatée
        Calendar calendar3 = Calendar.getInstance();
        SimpleDateFormat currenTime = new SimpleDateFormat("hh:mm aa");
        String servicePublishTime = currenTime.format(calendar3.getTime());

        // Création d'un HashMap avec les données du service
        HashMap serviceMap = new HashMap();
        serviceMap.put("userid", currentUserID);
        serviceMap.put("servicedate", servicePublishDate);
        serviceMap.put("servicetime", servicePublishTime);
        serviceMap.put("servicetitle", serviceTitleValue);
        serviceMap.put("servicesummarydescription", serviceSummaryDescriptionValue);
        serviceMap.put("servicefulldescription", serviceFullDescriptionValue);
        serviceMap.put("servicesearchkeyword", serviceSearchKeywordValue);
        serviceMap.put("servicefirstimage", firstImageDownloadURL);

        // Ajout conditionnel des images secondaires si présentes
        if(!TextUtils.isEmpty(secondImageDownloadURL))
        {
            serviceMap.put("servicesecondimage", secondImageDownloadURL);
        }

        if(!TextUtils.isEmpty(thirdImageDownloadURL))
        {
            serviceMap.put("servicethirdimage", thirdImageDownloadURL);
        }

        // Mise à jour des données dans la base Firebase sous une clé unique
        postDatabaseReference.child(serviceRandomName + currentUserID).updateChildren(serviceMap).addOnCompleteListener(new OnCompleteListener()
        {
            @Override
            public void onComplete(@NonNull Task task)
            {
                if(task.isSuccessful())
                {
                    // Fermer la barre de chargement
                    loadingbar.dismiss();
                    // Redirection vers la page des services
                    UserSendToServicePage();
                }
            }
        });

    }

    /* Redirige l'utilisateur vers la page des services */
    private void UserSendToServicePage()
    {
        Intent serviceIntent = new Intent(CreateServiceActivity.this, ServiceActivity.class);
        serviceIntent.putExtra("intentFrom", "MyProfile");
        serviceIntent.putExtra("intentUserID", currentUserID);
        startActivity(serviceIntent);
        finish(); // termine l'activité courante
    }


    /* Ouvre la galerie d'images pour que l'utilisateur sélectionne une image */
    private void OpenGallery()
    {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT); // ouvre un sélecteur de fichiers
        galleryIntent.setType("image/*"); // filtre pour les images uniquement
        startActivityForResult(galleryIntent, Gallery_Pick); // lance l'activité en attente du résultat
    }

    /* Gestion du résultat de la sélection d'image dans la galerie */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        // Vérifie qu'on revient bien de la galerie avec un résultat OK et des données non nulles
        if(requestCode == Gallery_Pick && resultCode == RESULT_OK && data != null)
        {
            // Récupération de l'URI selon le type d'image sélectionné
            if(imageType.equals("first"))
            {
                firstImageUri = data.getData();
                firstImage.setImageURI(firstImageUri); // affichage dans le bouton image
            }
            else if(imageType.equals("second"))
            {
                secondImageUri = data.getData();
                secondImage.setImageURI(secondImageUri);
            }
            else if(imageType.equals("third"))
            {
                thirdImageUri = data.getData();
                thirdImage.setImageURI(thirdImageUri);
            }
        }
    }

    /* Gestion du clic sur le bouton retour de la toolbar */
    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed(); // revient à l'activité précédente
        return true;
    }
}
