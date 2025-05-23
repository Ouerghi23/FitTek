package com.example.tekfit.activity;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class LoginActivity extends AppCompatActivity {

    // Déclaration des éléments de l'interface utilisateur
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;
    private FirebaseAuth mAuth;

    private TextInputLayout loginInputEmail, loginInputPassword;
    private Button loginBtn;
    private TextView createAccountBtn;

    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Configuration de la barre d'outils
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Welcome Back");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.LightTextColor)));
        toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.PrimaryTextColor), PorterDuff.Mode.SRC_ATOP);
        toolbar.setTitleTextColor(getResources().getColor(R.color.PrimaryTextColor));

        // Masquer l'image utilisateur de la toolbar
        toolbarUserImage = findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Initialiser FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Initialisation des champs de saisie
        loginInputEmail = findViewById(R.id.login_email);
        loginInputPassword = findViewById(R.id.login_password);

        // Initialisation des boutons
        createAccountBtn = findViewById(R.id.login_create_new_account_button);
        loginBtn = findViewById(R.id.login_login_button);

        // Action de connexion
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AllowingUserToLog();
            }
        });

        // Redirection vers la création de compte
        createAccountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserSendToAccountTypePage();
            }
        });
    }

    /**
     * Méthode de connexion de l'utilisateur
     * Vérifie les champs, affiche un ProgressDialog, puis tente la connexion via FirebaseAuth
     */
    private void AllowingUserToLog() {
        String email = loginInputEmail.getEditText().getText().toString().trim();
        String password = loginInputPassword.getEditText().getText().toString().trim();

        // Validation des champs
        if (email.isEmpty()) {
            loginInputEmail.setError("Please enter email address!");
        } else {
            loginInputEmail.setError(null);
        }

        if (password.isEmpty()) {
            loginInputPassword.setError("Please enter password!");
        } else {
            loginInputPassword.setError(null);
        }

        // Si les champs sont valides
        if (!email.isEmpty() && !password.isEmpty()) {
            // Affichage du chargement
            loadingBar = new ProgressDialog(this);
            SpannableString spannableMessage = new SpannableString("Logging...");
            spannableMessage.setSpan(new RelativeSizeSpan(1.3f), 0, spannableMessage.length(), 0);
            loadingBar.setMessage(spannableMessage);
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(false);
            loadingBar.setCancelable(false);

            // Connexion via FirebaseAuth
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Succès → rediriger vers la page principale
                        SendUserToMainPage();
                    } else {
                        // Échec → afficher un message d'erreur
                        loadingBar.cancel();
                        loginInputPassword.getEditText().setText("");

                        String msg = task.getException().getMessage();

                        // Boîte de dialogue d'erreur
                        final Dialog errorDialog = new Dialog(LoginActivity.this);
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

    /**
     * Redirige l'utilisateur vers la page principale après une connexion réussie
     */
    private void SendUserToMainPage() {
        Intent mainIntent = new Intent(LoginActivity.this, MainActivity.class);
        // Effacer l'historique des activités pour empêcher de revenir en arrière
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mainIntent.putExtra("intentFrom", "LoginActivity");
        startActivity(mainIntent);
        finish();
    }

    /**
     * Redirige l'utilisateur vers la page de choix de type de compte
     */
    private void UserSendToAccountTypePage() {
        Intent accountTypeIntent = new Intent(LoginActivity.this, AccountTypeActivity.class);
        startActivity(accountTypeIntent);
        finish();
    }

    /**
     * Gère le clic sur le bouton "retour" dans la toolbar
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
