// Déclaration du package de l'activité
package com.example.tekfit.activity;

// Import des classes nécessaires pour l'activité
import androidx.appcompat.app.AppCompatActivity;  // Classe de base pour les activités compatibles avec l'ActionBar
import android.content.Intent;                     // Pour gérer les intentions (navigation entre activités)
import android.os.Bundle;                         // Pour gérer l'état sauvegardé de l'activité
import android.os.Handler;                        // Pour gérer des tâches différées (delayed)

import com.example.tekfit.R;                      // Import des ressources de l'application (layout, strings, etc.)
import com.google.firebase.auth.FirebaseAuth;    // Classe Firebase pour l'authentification
import com.google.firebase.auth.FirebaseUser;    // Classe représentant l'utilisateur authentifié

// Déclaration de la classe SplashActivity qui hérite d'AppCompatActivity
public class SplashActivity extends AppCompatActivity {

    // Méthode appelée lors de la création de l'activité
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  // Appel de la méthode parente pour initialiser l'activité

        setContentView(R.layout.activity_splash);  // Chargement du layout XML associé à cette activité

        // Initialisation explicite de Firebase pour éviter les erreurs liées à Firebase non initialisé
        com.google.firebase.FirebaseApp.initializeApp(this);

        // Obtention de l'instance FirebaseAuth pour gérer l'authentification
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        // Récupération de l'utilisateur actuellement connecté (null si personne n'est connecté)
        final FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        // Création d'un Handler pour exécuter une tâche différée (après 1 seconde)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Vérification si un utilisateur est connecté
                if(currentUser != null) {
                    // Si utilisateur connecté, création d'une intention pour aller vers MainActivity
                    Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                    // Ajout de flags pour démarrer une nouvelle tâche et effacer les anciennes
                    mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    // Ajout d'une information supplémentaire pour indiquer la provenance de l'intent
                    mainIntent.putExtra("intentFrom", "SplashActivity");
                    // Lancement de l'activité MainActivity
                    startActivity(mainIntent);
                    finish(); // ← AJOUTER CECI !

                } else {
                    // Si aucun utilisateur connecté, création d'une intention pour aller vers WelcomeActivity
                    Intent welcomeIntent = new Intent(SplashActivity.this, com.example.tekfit.activity.WelcomeActivity.class);
                    // Ajout des mêmes flags pour nettoyer la pile d'activités
                    welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    // Lancement de l'activité WelcomeActivity
                    startActivity(welcomeIntent);
                    // Fermeture de SplashActivity pour ne pas revenir en arrière dessus
                    finish();
                }
            }
        }, 1000);  // Durée du délai : 1000 millisecondes (1 seconde)
    }
}
