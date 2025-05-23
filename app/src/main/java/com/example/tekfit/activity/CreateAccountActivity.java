// Déclaration du package de cette classe
package com.example.tekfit.activity;

// Importation des classes nécessaires
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.example.tekfit.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class CreateAccountActivity extends AppCompatActivity
{
    // Déclaration des composants de l'interface utilisateur et des variables Firebase
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDatabaseReference;

    private ProgressDialog loadingBar;

    // Champs du formulaire
    private TextInputLayout createAccountAccountType, createAccountPosition, createAccountUsername, createAccountEmail, createAccountPassword, createAccountConfirmPassword;

    private Button createAccountBtn;

    // Données utilisateur
    private String userAccountType, userPosition, username, userEmail, userPassword, userConfirmPassword;

    String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account); // Lien avec le fichier XML de l’UI

        // Configuration de la barre d’outils
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Couleur et style de la toolbar
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.LightTextColor)));
        toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.PrimaryTextColor), PorterDuff.Mode.SRC_ATOP);
        toolbar.setTitleTextColor(getResources().getColor(R.color.PrimaryTextColor));

        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE); // Masquer l’image utilisateur

        // Initialisation de FirebaseAuth et de la barre de chargement
        firebaseAuth = FirebaseAuth.getInstance();
        loadingBar = new ProgressDialog(this);

        // Liaison des champs de texte et du bouton à l’interface utilisateur
        createAccountAccountType = findViewById(R.id.create_account_account_type);
        createAccountPosition = findViewById(R.id.create_account_position);
        createAccountEmail = findViewById(R.id.create_account_email);
        createAccountUsername = findViewById(R.id.create_account_username);
        createAccountPassword = findViewById(R.id.create_account_password);
        createAccountConfirmPassword = findViewById(R.id.create_account_confirm_password);
        createAccountBtn = findViewById(R.id.register_create_account_button);

        // Récupération des données passées via l’intent
        Intent intent = getIntent();
        userAccountType = intent.getExtras().getString("AccountType");
        userPosition = intent.getExtras().getString("Position");

        // Adapter l’UI selon le type de compte
        if(userAccountType.equals("BUY A SERVICE")) {
            createAccountPosition.setVisibility(View.GONE);
            createAccountAccountType.getEditText().setText("FIND A SERVICE");
        }

        if(userAccountType.equals("SELL A SERVICE")) {
            createAccountPosition.getEditText().setText(userPosition);
            createAccountAccountType.getEditText().setText("BECOME A SELLER");
        }

        // Action du bouton "Create Account"
        createAccountBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                CreateNewAccount(); // Appelle la méthode de création de compte
            }
        });
    }

    // Méthode pour valider les champs et créer un nouveau compte utilisateur
    private void CreateNewAccount() {
        // Récupération des valeurs des champs
        username = createAccountUsername.getEditText().getText().toString();
        userEmail = createAccountEmail.getEditText().getText().toString();
        userPassword = createAccountPassword.getEditText().getText().toString();
        userConfirmPassword = createAccountConfirmPassword.getEditText().getText().toString();

        // Validation des champs un par un
        if(TextUtils.isEmpty(username)) {
            createAccountUsername.setError("Please enter username.");
        } else {
            createAccountUsername.setError(null);
        }

        if(TextUtils.isEmpty(userEmail)) {
            createAccountEmail.setError("Please enter email.");
        } else {
            createAccountEmail.setError(null);
        }

        if(TextUtils.isEmpty(userPassword)) {
            createAccountPassword.setError("Please enter password.");
        } else {
            createAccountPassword.setError(null);
        }

        if(TextUtils.isEmpty(userConfirmPassword)) {
            createAccountConfirmPassword.setError("Please confirm the password.");
        } else {
            createAccountConfirmPassword.setError(null);
        }

        // Vérifie que les mots de passe correspondent
        if(!TextUtils.isEmpty(userPassword) && !TextUtils.isEmpty(userConfirmPassword)) {
            if (!userPassword.equals(userConfirmPassword)) {
                createAccountConfirmPassword.setError("Please doesn't match.");
            } else {
                createAccountConfirmPassword.setError(null);
            }
        }

        // Si tous les champs sont valides, créer le compte Firebase
        if(!TextUtils.isEmpty(username) && !TextUtils.isEmpty(userEmail) && !TextUtils.isEmpty(userPassword)
                && !TextUtils.isEmpty(userConfirmPassword) && userPassword.equals(userConfirmPassword)) {

            // Afficher une barre de chargement
            loadingBar = new ProgressDialog(this);
            String ProgressDialogMessage="Creating Account...";
            SpannableString spannableMessage=  new SpannableString(ProgressDialogMessage);
            spannableMessage.setSpan(new RelativeSizeSpan(1.3f), 0, spannableMessage.length(), 0);
            loadingBar.setMessage(spannableMessage);
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.setCancelable(false);

            // Création du compte Firebase avec email et mot de passe
            firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                AddOtherDetailsToDatabase(); // Ajoute les infos supplémentaires
                            } else {
                                // Réinitialiser les champs mot de passe
                                createAccountPassword.getEditText().setText("");
                                createAccountConfirmPassword.getEditText().setText("");
                                loadingBar.dismiss();

                                // Affichage d’un message d’erreur
                                String msg = task.getException().getMessage();
                                final Dialog errorDialog = new Dialog(CreateAccountActivity.this);
                                errorDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                errorDialog.setContentView(R.layout.error_layout);
                                errorDialog.setTitle("Error Window");
                                errorDialog.show();
                                errorDialog.setCanceledOnTouchOutside(false);

                                TextView error = errorDialog.findViewById(R.id.error_dialog_error_message);
                                error.setText(msg);

                                Button cancelBtn = errorDialog.findViewById(R.id.error_dialog_cancel_button);
                                cancelBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        errorDialog.cancel();
                                    }
                                });
                            }
                        }
                    });
        }
    }

    // Méthode pour sauvegarder les détails utilisateur dans Firebase Realtime Database
    private void AddOtherDetailsToDatabase() {
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);

        // Date d'inscription formatée
        String joinedDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());

        // Données utilisateur à sauvegarder
        HashMap usermap = new HashMap();
        usermap.put("useraccounttype", userAccountType);
        if(userAccountType.equals("SELL A SERVICE")) {
            usermap.put("userposition", userPosition);
        }
        usermap.put("usersearchkeyword", username.toLowerCase());
        usermap.put("username", username);
        usermap.put("useremail", userEmail);
        usermap.put("userjoineddate", joinedDate);

        // Mise à jour des données dans la base Firebase
        userDatabaseReference.updateChildren(usermap).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if(task.isSuccessful()) {
                    loadingBar.dismiss();
                    SendUserToSetupPage(); // Redirige vers l'écran de configuration
                } else {
                    // Réinitialisation des mots de passe
                    createAccountPassword.getEditText().setText("");
                    createAccountConfirmPassword.getEditText().setText("");
                    loadingBar.dismiss();

                    // Affichage d’un message d’erreur
                    String msg2 = task.getException().getMessage();
                    final Dialog errordialog = new Dialog(CreateAccountActivity.this);
                    errordialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    errordialog.setContentView(R.layout.error_layout);
                    errordialog.setTitle("Error Window");
                    errordialog.show();
                    errordialog.setCanceledOnTouchOutside(false);

                    TextView error = errordialog.findViewById(R.id.error_dialog_error_message);
                    error.setText(msg2);

                    Button cancelBtn = errordialog.findViewById(R.id.error_dialog_cancel_button);
                    cancelBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            errordialog.cancel();
                        }
                    });
                }
            }
        });
    }

    // Méthode pour rediriger vers la page SetupActivity
    private void SendUserToSetupPage() {
        Intent setupIntent = new Intent(CreateAccountActivity.this, SetupActivity.class);
        setupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Nettoyer l’historique
        setupIntent.putExtra("IntentFrom", "CreateAccountActivity"); // Indique la provenance
        startActivity(setupIntent);
        finish();
    }

    // Gère le clic sur le bouton retour dans la toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Retour à l’écran précédent
        return true;
    }
}
