package com.example.tekfit.activity; // Déclare le package de cette classe

// Importation des bibliothèques Android et Firebase nécessaires
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tekfit.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddFoodActivity extends AppCompatActivity {

    // Déclaration des vues et variables nécessaires
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference diaryDatabaseReference, foodDatabaseReference, userDatabaseReference;

    private TextView foodName, servingSize, numberOfServings, calories, carbs, fat, protein, createdBy;
    private Button addbtn;
    private LinearLayout numberOfServingsContainer;

    // Variables pour stocker les données d’un aliment
    private String foodNameValue, servingSizeValue, numberOfSizeValue="1", servingSizeUnitValue, caloriesValue, carbsValue, fatValue, proteinValue, foodCreator, createdByValue;

    String intentFrom, intentFoodID;
    String currentUserID;

    private ProgressDialog loadingbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food); // Associe cette activité à son layout XML

        // Configuration de la barre d’outils
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add Food"); // Titre de la toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Affiche bouton retour
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Masquer l’image utilisateur de la toolbar
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Initialisation de Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        diaryDatabaseReference = FirebaseDatabase.getInstance().getReference().child("FoodDiaries");
        foodDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Foods");
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

        // Récupération des données de l’intent
        Intent intent = getIntent();
        intentFrom = intent.getExtras().getString("intentFrom");
        if (intentFrom.equals("Breakfast") || intentFrom.equals("Lunch") || intentFrom.equals("Dinner") || intentFrom.equals("Snack") || intentFrom.equals("ProfileMenu")) {
            intentFoodID = intent.getExtras().getString("intentFoodID");
        }

        // Association des vues XML
        foodName = (TextView) findViewById(R.id.addfood_foodname);
        servingSize = (TextView) findViewById(R.id.add_food_serving_size);
        numberOfServings = (TextView) findViewById(R.id.add_food_number_of_serving);
        calories = (TextView) findViewById(R.id.addfood_calories);
        carbs = (TextView) findViewById(R.id.addfood_carbs);
        fat = (TextView) findViewById(R.id.addfood_fat);
        protein = (TextView) findViewById(R.id.addfood_protein);
        createdBy = (TextView) findViewById(R.id.addfood_createdby);
        numberOfServingsContainer = findViewById(R.id.add_food_number_of_servings_container);

        // Configuration du bouton
        addbtn = (Button) findViewById(R.id.addfood_addbutton);
        if (!intentFrom.equals("ProfileMenu")) {
            addbtn.setText("Add to " + intentFrom); // Change texte du bouton selon le repas
        }

        loadingbar = new ProgressDialog(this); // Barre de chargement

        if (!TextUtils.isEmpty(intentFoodID)) {
            GetAndSetFoodDetails(); // Récupère et affiche les détails de l’aliment
        }

        // Écouteur bouton "Add"
        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddFoodToUserDiary(); // Ajoute l’aliment au journal alimentaire
            }
        });

        // Clique pour changer nombre de portions
        numberOfServingsContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupServingSizeEditDialog(); // Affiche un popup pour modifier les portions
            }
        });
    }

    // Affiche une boîte de dialogue pour modifier les portions
    private void PopupServingSizeEditDialog() {
        final Dialog numberOfServingEditDialog = new Dialog(this);
        numberOfServingEditDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        numberOfServingEditDialog.setContentView(R.layout.number_of_servings_edit_layout);
        numberOfServingEditDialog.setTitle("Number Of Serving Edit Window");
        numberOfServingEditDialog.show();
        Window servingSizeEditWindow = numberOfServingEditDialog.getWindow();
        servingSizeEditWindow.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        final EditText numberOfServingsInput = numberOfServingEditDialog.findViewById(R.id.number_of_servings_dialog_input);
        final TextView errorMsg = numberOfServingEditDialog.findViewById(R.id.number_of_servings_dialog_error);
        errorMsg.setVisibility(View.GONE);

        // Bouton Annuler
        Button cancelBtn = numberOfServingEditDialog.findViewById(R.id.number_of_servings_dialog_cancel_button);
        cancelBtn.setOnClickListener(v -> numberOfServingEditDialog.cancel());

        // Bouton Valider
        Button submitBtn = numberOfServingEditDialog.findViewById(R.id.number_of_servings_dialog_submit_button);
        submitBtn.setOnClickListener(v -> {
            if (TextUtils.isEmpty(numberOfServingsInput.getText().toString()) || Double.parseDouble(numberOfServingsInput.getText().toString()) <= 0) {
                errorMsg.setVisibility(View.VISIBLE);
            } else {
                numberOfSizeValue = numberOfServingsInput.getText().toString();
                numberOfServingEditDialog.cancel();
                NewValueCalculation(); // Recalcule les valeurs nutritionnelles
            }
        });
    }

    // Recalcule les valeurs nutritionnelles selon le nombre de portions
    private void NewValueCalculation() {
        numberOfServings.setText(numberOfSizeValue);

        String newCaloriesValue = String.format(Locale.US, "%.0f", (Double.parseDouble(numberOfSizeValue) * Double.parseDouble(caloriesValue)) / Double.parseDouble(servingSizeValue));
        calories.setText(newCaloriesValue + " Calories");

        String newCrabsValue = String.format(Locale.US, "%.1f", (Double.parseDouble(numberOfSizeValue) * Double.parseDouble(carbsValue)) / Double.parseDouble(servingSizeValue));
        carbs.setText(newCrabsValue + "g");

        String newFatValue = String.format(Locale.US, "%.1f", (Double.parseDouble(numberOfSizeValue) * Double.parseDouble(fatValue)) / Double.parseDouble(servingSizeValue));
        fat.setText(newFatValue + "g");

        String newProteinValue = String.format(Locale.US, "%.1f", (Double.parseDouble(numberOfSizeValue) * Double.parseDouble(proteinValue)) / Double.parseDouble(servingSizeValue));
        protein.setText(newProteinValue + "g");
    }

    // Récupère les informations d’un aliment dans Firebase
    private void GetAndSetFoodDetails() {
        foodDatabaseReference.child(intentFoodID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.hasChild("foodname")) {
                        foodNameValue = dataSnapshot.child("foodname").getValue().toString();
                        foodName.setText(foodNameValue);
                    }
                    if (dataSnapshot.hasChild("foodservingsize") && dataSnapshot.hasChild("foodservingsizeunit")) {
                        servingSizeValue = dataSnapshot.child("foodservingsize").getValue().toString();
                        servingSizeUnitValue = dataSnapshot.child("foodservingsizeunit").getValue().toString();
                        servingSize.setText(servingSizeValue + " " + servingSizeUnitValue);
                    }
                    if (dataSnapshot.hasChild("foodcalories")) {
                        caloriesValue = dataSnapshot.child("foodcalories").getValue().toString();
                        calories.setText(caloriesValue + " Calories");
                    }
                    if (dataSnapshot.hasChild("foodcarbs")) {
                        carbsValue = dataSnapshot.child("foodcarbs").getValue().toString();
                        carbs.setText(carbsValue + "g");
                    }
                    if (dataSnapshot.hasChild("foodfat")) {
                        fatValue = dataSnapshot.child("foodfat").getValue().toString();
                        fat.setText(fatValue + "g");
                    }
                    if (dataSnapshot.hasChild("foodprotein")) {
                        proteinValue = dataSnapshot.child("foodprotein").getValue().toString();
                        protein.setText(proteinValue + "g");
                    }
                    if (dataSnapshot.hasChild("foodcreator")) {
                        foodCreator = dataSnapshot.child("foodcreator").getValue().toString();
                        userDatabaseReference.child(foodCreator).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists() && dataSnapshot.hasChild("username")) {
                                    createdByValue = dataSnapshot.child("username").getValue().toString();
                                    createdBy.setText(createdByValue);
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {}
                        });
                    } else {
                        createdBy.setText("iFit Team"); // Par défaut
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    // Ajoute l’aliment au journal alimentaire de l’utilisateur
    private void AddFoodToUserDiary() {
        if (!intentFrom.equals("ProfileMenu")) {
            loadingbar = new ProgressDialog(this);
            SpannableString spannableMessage = new SpannableString("Adding Food...");
            spannableMessage.setSpan(new RelativeSizeSpan(1.3f), 0, spannableMessage.length(), 0);
            loadingbar.setMessage(spannableMessage);
            loadingbar.show();
            loadingbar.setCanceledOnTouchOutside(false);
            loadingbar.setCancelable(false);

            HashMap diaryMap = new HashMap();
            diaryMap.put("foodType", intentFrom.toLowerCase());
            diaryMap.put("numberOfServing", numberOfSizeValue);

            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat currenDate = new SimpleDateFormat("dd-MMM-yyyy");
            String currentDate = currenDate.format(calendar.getTime());

            // Mise à jour dans Firebase
            diaryDatabaseReference.child(currentUserID).child(currentDate).child(intentFoodID)
                    .updateChildren(diaryMap).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            UserSendToDiaryPage(); // Redirige vers le journal
                        } else {
                            loadingbar.dismiss();
                            String msg = task.getException().getMessage();
                            Toast.makeText(AddFoodActivity.this, "Error" + msg, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            OpenFoodSelectDialog(); // Affiche une boîte de sélection de type de repas
        }
    }

    // Affiche un menu pour choisir le type de repas
    private void OpenFoodSelectDialog() {
        final Dialog selectfooddialog = new Dialog(this);
        selectfooddialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        selectfooddialog.setContentView(R.layout.foodtype_select_menu_layout);
        selectfooddialog.setTitle("Select Food Window");
        selectfooddialog.show();

        // Ajoute aux différents repas
        selectfooddialog.findViewById(R.id.food_type_select_menu_breakfast).setOnClickListener(v -> {
            selectfooddialog.dismiss();
            intentFrom = "Breakfast";
            AddFoodToUserDiary();
        });
        selectfooddialog.findViewById(R.id.food_type_select_menu_lunch).setOnClickListener(v -> {
            selectfooddialog.dismiss();
            intentFrom = "Lunch";
            AddFoodToUserDiary();
        });
        selectfooddialog.findViewById(R.id.food_type_select_menu_dinner).setOnClickListener(v -> {
            selectfooddialog.dismiss();
            intentFrom = "Dinner";
            AddFoodToUserDiary();
        });
        selectfooddialog.findViewById(R.id.food_type_select_menu_snacks).setOnClickListener(v -> {
            selectfooddialog.dismiss();
            intentFrom = "Snack";
            AddFoodToUserDiary();
        });
    }

    // Redirige vers la page du journal
    private void UserSendToDiaryPage() {
        Intent diaryIntent = new Intent(AddFoodActivity.this, DiaryActivity.class);
        diaryIntent.putExtra("intentFrom", "AddFoodActivity");
        diaryIntent.putExtra("intentUserID", currentUserID);
        startActivity(diaryIntent);
        finish();
    }

    // Gère l’action du bouton retour
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
