package com.example.tekfit.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tekfit.R;

public class WelcomeActivity extends AppCompatActivity
{
    // Déclaration des composants UI
    private Button signUpBtn, loginBtn; // Boutons d'inscription et de connexion
    private TextView text1, text2; // Textes de bienvenue
    private ImageView backgroundImage; // Image de fond

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome); // Lie le layout XML à cette activité

        // Initialisation des vues
        text1 = (TextView) findViewById(R.id.text1); // Texte principal
        text2 = (TextView) findViewById(R.id.text2); // Sous-texte
        backgroundImage = (ImageView) findViewById(R.id.welcome_background_image); // Image de fond

        /* Animation de l'image de fond */
        Animation imageAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.welcome_image_animation);
        backgroundImage.startAnimation(imageAnimation);

        /* Animations pour les textes */
        Animation text1Animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.welcome_text1_animation);
        text1.startAnimation(text1Animation); // Animation pour le texte principal

        Animation text2Animation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.welcome_text2_animation);
        text2.startAnimation(text2Animation); // Animation pour le sous-texte

        // Initialisation des boutons
        signUpBtn = (Button)findViewById(R.id.welcome_signup_button); // Bouton d'inscription
        loginBtn = (Button)findViewById(R.id.register_facebook_button); // Bouton de connexion

        /* Gestion du clic sur le bouton d'inscription */
        signUpBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                SendUserToAccountTypePage(); // Redirige vers la page de choix de type de compte
            }
        });

        /* Gestion du clic sur le bouton de connexion */
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendUserToLoginPage(); // Redirige vers la page de connexion
            }
        });
    }

    /* Redirection vers la page de connexion */
    private void SendUserToLoginPage()
    {
        Intent loginIntent = new Intent(WelcomeActivity.this, LoginActivity.class);
        startActivity(loginIntent); // Lance l'activité de connexion
    }

    /* Redirection vers la page de choix de type de compte */
    private void SendUserToAccountTypePage()
    {
        Intent accountTypeIntent = new Intent(WelcomeActivity.this, AccountTypeActivity.class);
        startActivity(accountTypeIntent); // Lance l'activité de choix de type de compte
    }
}