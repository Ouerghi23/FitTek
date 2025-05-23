package com.example.tekfit.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tekfit.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class GoalsActivity extends AppCompatActivity {

    // Déclaration des vues et variables Firebase
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDatabaseReference, weightLossAchievementDatabaseReference;
    private ProgressDialog loadingBar;

    // Composants de l'interface liés aux objectifs
    private Button GoalSetButton;
    private TextView GoalCurrentWeight, GoalGoalWeight, GoalWeeklyGoalWeight, GoalStartWeight, GoalStartDate, GoalRemainingWeight,
            GoalCutCalories, GoalCompleteDetails, GoalTitle;
    private LinearLayout GoalCompleteContainer, GoalDetailsContainer;

    // Variables pour stocker les données utilisateur
    private String currentWeight = "", goalWeight = "", weeklyGoalWeight = "", startWeight, startDate, remainingWeight, cutCalories;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        // Configuration de la toolbar
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Goals");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Masquer l'image utilisateur dans la toolbar
        toolbarUserImage = findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Initialisation Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);
        weightLossAchievementDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Achievements").child(currentUserID);

        // Liaison des vues XML
        GoalCurrentWeight = findViewById(R.id.goal_current_weight);
        GoalGoalWeight = findViewById(R.id.goal_goal_weight);
        GoalWeeklyGoalWeight = findViewById(R.id.goal_goal_weight_per_week);
        GoalStartWeight = findViewById(R.id.goal_goal_start_weight);
        GoalStartDate = findViewById(R.id.goal_goal_start_date);
        GoalRemainingWeight = findViewById(R.id.goal_remaining_weight);
        GoalCutCalories = findViewById(R.id.goal_cut_calories);
        GoalSetButton = findViewById(R.id.goal_goal_set_button);
        GoalDetailsContainer = findViewById(R.id.goal_goal_details_container);
        GoalCompleteContainer = findViewById(R.id.goal_goal_complete_container);
        GoalCompleteContainer.setVisibility(View.GONE);
        GoalCompleteDetails = findViewById(R.id.goal_goal_complete_details);
        GoalTitle = findViewById(R.id.goal_goal_title);

        // Bouton pour définir ou changer un objectif
        GoalSetButton.setOnClickListener(v -> {
            if (currentWeight.isEmpty()) {
                Toast.makeText(GoalsActivity.this, "Please setup your profile for set goal", Toast.LENGTH_SHORT).show();
            } else {
                PopupSetGoalDialog();
            }
        });

        // Récupérer les données utilisateur depuis Firebase
        RetrieveUserData();
    }

    /**
     * Récupère les informations de l'utilisateur depuis Firebase et les affiche
     */
    private void RetrieveUserData() {
        userDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Récupération et affichage des poids et dates
                    if (dataSnapshot.hasChild("usergoalweight")) {
                        goalWeight = dataSnapshot.child("usergoalweight").getValue().toString();
                        if (!goalWeight.isEmpty()) {
                            GoalSetButton.setText("Change Goal");
                            GoalGoalWeight.setText(goalWeight + " KG");
                        }
                    }

                    if (dataSnapshot.hasChild("userstartweight")) {
                        startWeight = dataSnapshot.child("userstartweight").getValue().toString();
                        if (!startWeight.isEmpty()) {
                            GoalStartWeight.setText(startWeight + " KG");
                        }
                    }

                    if (dataSnapshot.hasChild("usergoalstartdate")) {
                        startDate = dataSnapshot.child("usergoalstartdate").getValue().toString();
                        if (!startDate.isEmpty()) {
                            GoalStartDate.setText(startDate);
                        }
                    }

                    if (dataSnapshot.hasChild("usercurrentweight")) {
                        currentWeight = dataSnapshot.child("usercurrentweight").getValue().toString();
                        if (!currentWeight.isEmpty()) {
                            GoalCurrentWeight.setText(currentWeight + " KG");
                        }
                    }

                    if (dataSnapshot.hasChild("userweeklygoalweight")) {
                        weeklyGoalWeight = dataSnapshot.child("userweeklygoalweight").getValue().toString();
                        if (!weeklyGoalWeight.isEmpty()) {
                            GoalWeeklyGoalWeight.setText(weeklyGoalWeight + " KG");
                        }
                    }

                    // Calcul du poids restant et des calories à réduire
                    if (!goalWeight.isEmpty()) {
                        remainingWeight = String.format(Locale.US, "%.1f", (Double.parseDouble(currentWeight) - Double.parseDouble(goalWeight)));
                        GoalRemainingWeight.setText(remainingWeight + " KG");

                        cutCalories = String.format(Locale.US, "%.0f", ((Double.parseDouble(weeklyGoalWeight) * 3500) / 0.45));
                        GoalCutCalories.setText(cutCalories + " Per Week");
                    } else {
                        GoalDetailsContainer.setVisibility(View.GONE);
                        GoalCompleteContainer.setVisibility(View.VISIBLE);
                        GoalTitle.setText("Weight Loss Goal");
                        GoalCompleteDetails.setText("Set a goal and earn points.");
                    }

                    // Vérifier le statut des réalisations
                    weightLossAchievementDatabaseReference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                String goalStatus = "", achievementStatus = "", points = "0";

                                if (dataSnapshot.hasChild("weightLossGoalStatus")) {
                                    goalStatus = dataSnapshot.child("weightLossGoalStatus").getValue().toString();
                                }
                                if (dataSnapshot.hasChild("weightLossAchievementStatus")) {
                                    achievementStatus = dataSnapshot.child("weightLossAchievementStatus").getValue().toString();
                                }
                                if (dataSnapshot.hasChild("points")) {
                                    points = dataSnapshot.child("points").getValue().toString();
                                }

                                if (goalStatus.equals("true") && achievementStatus.equals("true")) {
                                    GoalTitle.setText("Achievement");
                                    GoalDetailsContainer.setVisibility(View.GONE);
                                    GoalCompleteContainer.setVisibility(View.VISIBLE);
                                    GoalCompleteDetails.setText("Congratulations, You have achieved your previous weight loss goal." +
                                            "\n\n Start Weight : " + startWeight + " KG" +
                                            "\n Start Date : " + startDate +
                                            "\n Goal Weight : " + goalWeight + " KG");

                                    GoalSetButton.setText("Set New Goal");
                                } else {
                                    GoalDetailsContainer.setVisibility(View.VISIBLE);
                                    GoalCompleteContainer.setVisibility(View.GONE);
                                    GoalSetButton.setText("Change Goal");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Erreur silencieuse
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Erreur silencieuse
            }
        });
    }

    /**
     * Affiche un popup pour permettre à l'utilisateur de définir son objectif
     */
    private void PopupSetGoalDialog() {
        final Dialog setGoalDialog = new Dialog(this);
        setGoalDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setGoalDialog.setContentView(R.layout.set_goal_layout);
        setGoalDialog.setTitle("Set Goal Dialog");
        setGoalDialog.show();
        Window window = setGoalDialog.getWindow();
        window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        final EditText weightInput = setGoalDialog.findViewById(R.id.set_goal_goal_weight_input);
        final EditText weeklyWeightInput = setGoalDialog.findViewById(R.id.set_goal_goal_weight_per_week_input);
        final TextView error = setGoalDialog.findViewById(R.id.set_goal_dialog_error);
        error.setVisibility(View.GONE);

        // Préremplir les champs si données déjà présentes
        if (!goalWeight.isEmpty()) weightInput.setText(goalWeight);
        if (!weeklyGoalWeight.isEmpty()) weeklyWeightInput.setText(weeklyGoalWeight);

        // Bouton annuler
        Button cancelBtn = setGoalDialog.findViewById(R.id.set_goal_dialog_cancel_button);
        cancelBtn.setOnClickListener(v -> setGoalDialog.dismiss());

        // Bouton soumettre
        Button submitBtn = setGoalDialog.findViewById(R.id.set_goal_dialog_submit_button);
        submitBtn.setOnClickListener(v -> {
            String DialogWeight = weightInput.getText().toString();
            String DialogWeeklyWeight = weeklyWeightInput.getText().toString();

            // Vérification des champs
            if (DialogWeight.isEmpty() || DialogWeeklyWeight.isEmpty()) {
                error.setText("Above fields can not be empty.");
                error.setVisibility(View.VISIBLE);
            } else if (Double.parseDouble(DialogWeight) >= Double.parseDouble(currentWeight)) {
                error.setText("Goal weight should be smaller than current weight.");
                error.setVisibility(View.VISIBLE);
            } else if (Double.parseDouble(DialogWeeklyWeight) > 1) {
                error.setText("Weekly Goal weight should be equal to 1 KG or smaller than 1 KG.");
                error.setVisibility(View.VISIBLE);
            } else {
                setGoalDialog.dismiss();

                // Afficher la barre de chargement
                loadingBar = new ProgressDialog(GoalsActivity.this);
                String ProgressDialogMessage = "Setting Goal...";
                SpannableString spannableMessage = new SpannableString(ProgressDialogMessage);
                spannableMessage.setSpan(new RelativeSizeSpan(1.3f), 0, spannableMessage.length(), 0);
                loadingBar.setMessage(spannableMessage);
                loadingBar.show();
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.setCancelable(false);

                // Date actuelle
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy");
                String currentDate = simpleDateFormat.format(calendar.getTime());

                // Mise à jour des données utilisateur
                HashMap<String, Object> userMap = new HashMap<>();
                userMap.put("usergoalweight", DialogWeight);
                userMap.put("userstartweight", currentWeight);
                userMap.put("usergoalstartdate", currentDate);
                userMap.put("userweeklygoalweight", DialogWeeklyWeight);
                userDatabaseReference.updateChildren(userMap);

                // Mise à jour des réalisations
                HashMap<String, Object> achievementMap = new HashMap<>();
                achievementMap.put("weightLossGoalStatus", "true");
                achievementMap.put("weightLossAchievementStatus", "false");
                weightLossAchievementDatabaseReference.updateChildren(achievementMap);

                // Masquer la barre de chargement après délai
                new Handler().postDelayed(() -> loadingBar.cancel(), 500);
            }
        });
    }

    /**
     * Gère l'action du bouton retour dans la toolbar
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
