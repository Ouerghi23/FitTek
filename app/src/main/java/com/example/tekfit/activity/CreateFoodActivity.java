package com.example.tekfit.activity;

// Importations nécessaires pour les composants Android et Firebase
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tekfit.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateFoodActivity extends AppCompatActivity
{
    // Déclaration des éléments de l'interface et de Firebase
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDatabaseReference, foodDatabaseReference;

    private EditText foodName, servingSize, servingSizeUnit, calories, carbs, fat, protein;
    private TextView warning;
    private Button createFoodbtn;

    private ProgressDialog loadingbar;

    private String currentUserID;
    String intentFrom;  // Pour stocker la provenance de l’intent (FoodList ou Journal)

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_food); // Définit la vue

        // Initialisation de la toolbar
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create Food"); // Titre de la toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Affiche bouton retour
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Cache l’image de profil utilisateur de la toolbar
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Récupération de la valeur passée via l’intent (provenance de la page précédente)
        Intent intent = getIntent();
        intentFrom = intent.getExtras().getString("intentFrom");

        // Initialisation de Firebase Auth et Références Realtime Database
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid(); // ID utilisateur actuel
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);
        foodDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Foods"); // Référence à la collection des aliments

        // Initialisation des vues
        warning = (TextView) findViewById(R.id.create_food_warning);
        warning.setVisibility(View.INVISIBLE); // Cache l’avertissement initialement

        foodName = (EditText) findViewById(R.id.create_food_name);
        servingSize = (EditText) findViewById(R.id.create_food_servingSize);
        servingSizeUnit = (EditText) findViewById(R.id.create_food_servingSizeUnit);
        calories = (EditText) findViewById(R.id.create_food_calories);
        carbs = (EditText) findViewById(R.id.create_food_carbs);
        fat = (EditText) findViewById(R.id.create_food_fat);
        protein = (EditText) findViewById(R.id.create_food_protien);
        createFoodbtn = (Button) findViewById(R.id.create_food_button);

        loadingbar = new ProgressDialog(this); // Boîte de dialogue de chargement

        // Événement au clic sur le bouton
        createFoodbtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                CreateNewFood(); // Lance la création d’un nouvel aliment
            }
        });
    }

    private void CreateNewFood()
    {
        // Récupère les données saisies par l’utilisateur
        String foodNameString = foodName.getText().toString();
        String servingSizeString = servingSize.getText().toString();
        String servingSizeUnitString = servingSizeUnit.getText().toString();
        String caloriesString = calories.getText().toString();
        String carbsString = carbs.getText().toString();
        String fatString = fat.getText().toString();
        String proteinString = protein.getText().toString();

        // Vérifie si un champ est vide
        if(TextUtils.isEmpty(foodNameString)  || TextUtils.isEmpty(servingSizeString) ||
                TextUtils.isEmpty(servingSizeUnitString) || TextUtils.isEmpty(caloriesString) ||
                TextUtils.isEmpty(carbsString) || TextUtils.isEmpty(fatString) || TextUtils.isEmpty(proteinString))
        {
            warning.setVisibility(View.VISIBLE); // Affiche un message d’erreur
        }
        else
        {
            warning.setVisibility(View.INVISIBLE); // Cache l’erreur

            // Configuration du message de chargement
            loadingbar = new ProgressDialog(this);
            String ProgressDialogMessage = "Creating Food...";
            SpannableString spannableMessage = new SpannableString(ProgressDialogMessage);
            spannableMessage.setSpan(new RelativeSizeSpan(1.3f), 0, spannableMessage.length(), 0);
            loadingbar.setMessage(spannableMessage);
            loadingbar.show(); // Affiche la boîte de chargement
            loadingbar.setCanceledOnTouchOutside(false);
            loadingbar.setCancelable(false);

            // Prépare une map contenant les informations de l’aliment
            HashMap usermap = new HashMap();
            usermap.put("foodname", foodNameString);
            usermap.put("foodsearchkeyword", foodNameString.toLowerCase()); // Pour recherche
            usermap.put("foodservingsize", servingSizeString);
            usermap.put("foodservingsizeunit", servingSizeUnitString);
            usermap.put("foodcalories", caloriesString);
            usermap.put("foodcarbs", carbsString);
            usermap.put("foodfat", fatString);
            usermap.put("foodprotein", proteinString);
            usermap.put("foodcreator", currentUserID); // ID de l’utilisateur créateur

            // Génère un nom unique pour l'aliment à partir de la date et l’heure
            Calendar calendar1 = Calendar.getInstance();
            SimpleDateFormat currenDate = new SimpleDateFormat("yyyyMMdd");
            String CurrentDate = currenDate.format(calendar1.getTime());

            Calendar calendar2 = Calendar.getInstance();
            SimpleDateFormat currenTime = new SimpleDateFormat("HHmmss");
            String CurrentTime = currenTime.format(calendar2.getTime());

            // Concatène nom + ID utilisateur + date + heure
            String foodRandomName = foodNameString + currentUserID + CurrentDate + CurrentTime;
            foodRandomName = foodRandomName.replace(" ", ""); // Supprime les espaces
            foodRandomName = foodRandomName.toLowerCase(); // Minuscule pour uniformité

            // Envoie les données dans Firebase Realtime Database
            foodDatabaseReference.child(foodRandomName).updateChildren(usermap).addOnCompleteListener(new OnCompleteListener()
            {
                @Override
                public void onComplete(@NonNull Task task)
                {
                    if(task.isSuccessful())
                    {
                        loadingbar.dismiss(); // Cache la boîte de chargement
                        Toast.makeText(CreateFoodActivity.this, "Food Create Successfully.", Toast.LENGTH_SHORT).show();
                        UserSendToFoodListPage(); // Redirige vers la liste des aliments
                    }
                    else
                    {
                        loadingbar.dismiss();
                        String errorMsg = task.getException().getMessage(); // Affiche l’erreur
                        warning.setText(errorMsg);
                    }
                }
            });
        }
    }

    // Redirige l’utilisateur vers la page de liste d’aliments
    private void UserSendToFoodListPage()
    {
        Intent foodListIntent = new Intent(CreateFoodActivity.this, FoodListActivity.class);
        foodListIntent.putExtra("intentFrom", intentFrom); // Passe le contexte d’origine
        startActivity(foodListIntent);
        finish(); // Ferme l'activité actuelle
    }

    // Gère le bouton retour dans la toolbar
    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed(); // Action de retour
        return true;
    }
}
