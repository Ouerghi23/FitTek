package com.example.tekfit.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import de.hdodenhof.circleimageview.CircleImageView;

public class AddWorkoutActivity extends AppCompatActivity
{

    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference workoutDiaryDatabaseReference;

    private TextView workoutName, workoutCalories, workoutDuration;
    private ImageButton durationMinusBtn, durationAddBtn;
    private Button addBtn;

    private ImageView workoutImage;

    private String totalCalories="0",totalDuration="0", calories="0", duration="0";

    private String currentUserID, intentFrom, currentDate;

    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_workout);

        /* Configuration de la toolbar avec titre et bouton retour */
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Add Workout");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        /* Masquer l'image utilisateur dans la toolbar */
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        /* Récupérer la date courante au format "dd-MMM-yyyy" */
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat currenDate = new SimpleDateFormat("dd-MMM-yyyy");
        currentDate = currenDate.format(calendar.getTime());

        /* Initialiser FirebaseAuth et récupérer l'ID de l'utilisateur connecté */
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();

        /* Récupération des vues depuis le layout */
        workoutImage = (ImageView) findViewById(R.id.add_workout_image);
        workoutName = (TextView) findViewById(R.id.add_workout_name);
        workoutCalories = (TextView) findViewById(R.id.add_workout_calories);
        workoutDuration = (TextView) findViewById(R.id.add_workout_duration);
        durationMinusBtn = (ImageButton) findViewById(R.id.add_workout_duration_minus_button);
        durationAddBtn = (ImageButton) findViewById(R.id.add_workout_duration_add_button);
        addBtn = (Button) findViewById(R.id.add_workout_add_button);

        /* Récupérer l'information depuis l'intent sur quel type d'entraînement ajouter */
        Intent intent = getIntent();
        intentFrom = intent.getExtras().getString("intentFrom");

        if(!intentFrom.isEmpty())
        {
            /* Référence à la base de données Firebase pour cet utilisateur, date et type d'entraînement */
            workoutDiaryDatabaseReference = FirebaseDatabase.getInstance().getReference()
                    .child("WorkoutDiaries")
                    .child(currentUserID)
                    .child(currentDate)
                    .child(intentFrom);

            /* Récupérer les calories et durée totales déjà enregistrées */
            RetrieveTotalCalories();

            /* Charger l'interface selon le type d'entraînement choisi */
            LoadAddWorkoutPage(intentFrom);

            /* Gestion du clic sur le bouton ajouter pour enregistrer les données */
            addBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    AddUserWorkoutToDiary();
                }
            });
        }
    }

    /* Méthode pour récupérer les calories et durée totales enregistrées dans la base de données */
    private void RetrieveTotalCalories()
    {
        workoutDiaryDatabaseReference.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.exists())
                {
                    /* Si le noeud "calories" existe, on met à jour totalCalories */
                    if(dataSnapshot.hasChild("calories"))
                    {
                        totalCalories = dataSnapshot.child("calories").getValue().toString();
                    }
                    /* Si le noeud "duration" existe, on met à jour totalDuration */
                    if(dataSnapshot.hasChild("duration"))
                    {
                        totalDuration = dataSnapshot.child("duration").getValue().toString();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
                // Gestion d'erreur éventuellement à implémenter ici
            }
        });
    }

    /* Charge l'interface d'ajout selon le type d'entraînement sélectionné */
    private void LoadAddWorkoutPage(String workoutType)
    {
        /* Pour chaque type d'entraînement on définit les calories/minutes, l'image, et on configure les boutons +/- */

        if(workoutType.equals("Walking"))
        {
            calories = "4"; // Calories brûlées par minute pour la marche
            duration = "1"; // Durée initiale en minutes

            workoutImage.setImageDrawable(null);
            workoutImage.setBackgroundResource(R.drawable.walking_icon);
            workoutName.setText("Walking");
            workoutCalories.setText(calories+" Calories");
            workoutDuration.setText(duration+" min");

            // Bouton pour diminuer la durée (minimum 1 minute)
            durationMinusBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(Integer.parseInt(duration) > 1)
                    {
                        duration = String.valueOf(Integer.parseInt(duration) - 1);
                        calories = String.valueOf(Integer.parseInt(calories) - 4);
                        workoutDuration.setText(duration+" min");
                        workoutCalories.setText(calories+" Calories");
                    }
                }
            });

            // Bouton pour augmenter la durée
            durationAddBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    duration = String.valueOf(Integer.parseInt(duration) + 1);
                    calories = String.valueOf(Integer.parseInt(calories) + 4);
                    workoutDuration.setText(duration+" min");
                    workoutCalories.setText(calories+" Calories");
                }
            });
        }

        // Même logique pour les autres types d'entraînement avec leurs valeurs spécifiques

        if(workoutType.equals("Running"))
        {
            calories = "9"; // Calories/min pour la course
            duration = "1";

            workoutImage.setImageDrawable(null);
            workoutImage.setBackgroundResource(R.drawable.running_icon);
            workoutName.setText("Running");
            workoutCalories.setText(calories+" Calories");
            workoutDuration.setText(duration+" min");

            durationMinusBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(Integer.parseInt(duration) > 1)
                    {
                        duration = String.valueOf(Integer.parseInt(duration) - 1);
                        calories = String.valueOf(Integer.parseInt(calories) - 9);
                        workoutDuration.setText(duration+" min");
                        workoutCalories.setText(calories+" Calories");
                    }
                }
            });

            durationAddBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    duration = String.valueOf(Integer.parseInt(duration) + 1);
                    calories = String.valueOf(Integer.parseInt(calories) + 9);
                    workoutDuration.setText(duration+" min");
                    workoutCalories.setText(calories+" Calories");
                }
            });
        }

        if(workoutType.equals("Cycling"))
        {
            calories = "7"; // Calories/min pour le vélo
            duration = "1";

            workoutImage.setImageDrawable(null);
            workoutImage.setBackgroundResource(R.drawable.cycling_icon);
            workoutName.setText("Cycling");
            workoutCalories.setText(calories+" Calories");
            workoutDuration.setText(duration+" min");

            durationMinusBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(Integer.parseInt(duration) > 1)
                    {
                        duration = String.valueOf(Integer.parseInt(duration) - 1);
                        calories = String.valueOf(Integer.parseInt(calories) - 7);
                        workoutDuration.setText(duration+" min");
                        workoutCalories.setText(calories+" Calories");
                    }
                }
            });

            durationAddBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    duration = String.valueOf(Integer.parseInt(duration) + 1);
                    calories = String.valueOf(Integer.parseInt(calories) + 7);
                    workoutDuration.setText(duration+" min");
                    workoutCalories.setText(calories+" Calories");
                }
            });
        }

        if(workoutType.equals("Hiking"))
        {
            calories = "6"; // Calories/min pour la randonnée
            duration = "1";

            workoutImage.setImageDrawable(null);
            workoutImage.setBackgroundResource(R.drawable.hiking_icon);
            workoutName.setText("Hiking");
            workoutCalories.setText(calories+" Calories");
            workoutDuration.setText(duration+" min");

            durationMinusBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(Integer.parseInt(duration) > 1)
                    {
                        duration = String.valueOf(Integer.parseInt(duration) - 1);
                        calories = String.valueOf(Integer.parseInt(calories) - 6);
                        workoutDuration.setText(duration+" min");
                        workoutCalories.setText(calories+" Calories");
                    }
                }
            });

            durationAddBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    duration = String.valueOf(Integer.parseInt(duration) + 1);
                    calories = String.valueOf(Integer.parseInt(calories) + 6);
                    workoutDuration.setText(duration+" min");
                    workoutCalories.setText(calories+" Calories");
                }
            });
        }

        if(workoutType.equals("Swimming"))
        {
            calories = "11"; // Calories/min pour la natation
            duration = "1";

            workoutImage.setImageDrawable(null);
            workoutImage.setBackgroundResource(R.drawable.swimming_icon);
            workoutName.setText("Swimming");
            workoutCalories.setText(calories+" Calories");
            workoutDuration.setText(duration+" min");

            durationMinusBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    if(Integer.parseInt(duration) > 1)
                    {
                        duration = String.valueOf(Integer.parseInt(duration) - 1);
                        calories = String.valueOf(Integer.parseInt(calories) - 11);
                        workoutDuration.setText(duration+" min");
                        workoutCalories.setText(calories+" Calories");
                    }
                }
            });

            durationAddBtn.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    duration = String.valueOf(Integer.parseInt(duration) + 1);
                    calories = String.valueOf(Integer.parseInt(calories) + 11);
                    workoutDuration.setText(duration+" min");
                    workoutCalories.setText(calories+" Calories");
                }
            });
        }
    }

    /* Méthode pour ajouter l'entraînement de l'utilisateur dans la base de données */
    private void AddUserWorkoutToDiary()
    {
        /* Afficher une barre de progression personnalisée */
        loadingBar = new ProgressDialog(this);
        String ProgressDialogMessage="Adding Workout...";
        SpannableString spannableMessage=  new SpannableString(ProgressDialogMessage);
        spannableMessage.setSpan(new RelativeSizeSpan(1.3f), 0, spannableMessage.length(), 0);
        loadingBar.setMessage(spannableMessage);
        loadingBar.show();
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.setCancelable(false);

        /* Calculer les nouvelles totaux de calories et durée */
        totalCalories = String.valueOf(Integer.parseInt(totalCalories) + Integer.parseInt(calories));
        totalDuration = String.valueOf(Integer.parseInt(totalDuration) + Integer.parseInt(duration));

        /* Créer une map pour mettre à jour les valeurs dans Firebase */
        HashMap workoutMap = new HashMap();
        workoutMap.put("calories", totalCalories);
        workoutMap.put("duration", totalDuration);

        /* Mise à jour dans la base de données Firebase */
        workoutDiaryDatabaseReference.updateChildren(workoutMap).addOnCompleteListener(new OnCompleteListener()
        {
            @Override
            public void onComplete(@NonNull Task task)
            {
                if(task.isSuccessful())
                {
                    loadingBar.cancel();
                    UserSendToDiaryPage();  // Rediriger vers la page du journal
                }
                else
                {
                    loadingBar.dismiss();
                    String msg = task.getException().getMessage();
                    Toast.makeText(AddWorkoutActivity.this, "Error"+msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /* Méthode pour rediriger l'utilisateur vers la page du journal d'entraînements */
    private void UserSendToDiaryPage()
    {
        Intent diaryIntent = new Intent(AddWorkoutActivity.this, DiaryActivity.class);
        diaryIntent.putExtra("intentFrom", "AddFoodActivity");
        startActivity(diaryIntent);
        finish();
    }

    /* Gestion du clic sur le bouton retour de la toolbar */
    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed();
        return true;
    }
}
