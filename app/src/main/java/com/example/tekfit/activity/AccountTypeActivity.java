package com.example.tekfit.activity;
// Déclaration du package où se trouve cette classe

import androidx.appcompat.app.AppCompatActivity;
// Import de la classe AppCompatActivity pour utiliser les fonctionnalités de compatibilité

import androidx.appcompat.widget.Toolbar;
// Import de la classe Toolbar d'AndroidX pour la barre d'outils personnalisée

import android.content.Intent;
// Import de la classe Intent pour gérer la navigation entre activités

import android.graphics.PorterDuff;
// Import pour manipuler les filtres de couleur sur les drawables

import android.graphics.drawable.ColorDrawable;
// Import pour définir une couleur en tant que drawable (fond)

import android.os.Bundle;
// Import de la classe Bundle pour récupérer les données passées entre activités

import android.view.View;
// Import pour gérer les vues (éléments graphiques)

import android.widget.AdapterView;
// Import pour gérer les événements sur les adapters (ex: Spinner)

import android.widget.ArrayAdapter;
// Import pour adapter un tableau de données à une vue (ex: Spinner)

import android.widget.Button;
// Import pour utiliser le composant Button

import android.widget.RadioButton;
// Import pour utiliser les boutons radio (choix exclusif)

import android.widget.Spinner;
// Import pour utiliser un Spinner (menu déroulant)

import android.widget.TextView;
// Import pour utiliser le composant TextView

import com.example.tekfit.R;
// Import du fichier R qui référence toutes les ressources (layouts, couleurs, etc)

import de.hdodenhof.circleimageview.CircleImageView;
// Import d'une bibliothèque tierce pour afficher une image ronde (photo de profil)

public class AccountTypeActivity extends AppCompatActivity
// Déclaration de la classe AccountTypeActivity qui hérite de AppCompatActivity

{

    private Toolbar toolbar;
    // Déclaration de la variable toolbar de type Toolbar

    private CircleImageView toolbarUserImage;
    // Variable pour l'image ronde dans la toolbar

    private RadioButton buyAServiceBtn, sellAServiceBtn;
    // Variables pour les deux boutons radio : acheter ou vendre un service

    private Spinner position;
    // Variable pour le Spinner (menu déroulant) de la position

    private TextView errorText, loginBtn;
    // Variables pour le TextView d'erreur et le bouton de connexion (qui est un TextView)

    private Button nextBtn;
    // Variable pour le bouton "Suivant"

    private String strPosition, strAccountType="";
    // Variables pour stocker les choix de l'utilisateur (type de compte et position)

    @Override
    // Annotation indiquant que cette méthode redéfinit une méthode parente

    protected void onCreate(Bundle savedInstanceState)
    // Méthode appelée à la création de l'activité

    {
        super.onCreate(savedInstanceState);
        // Appelle la méthode parente onCreate

        setContentView(R.layout.activity_account_type);
        // Définit le layout XML à utiliser pour cette activité

        /* Adding tool bar with title and hiding user image */
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        // Récupère la toolbar définie dans le layout

        setSupportActionBar(toolbar);
        // Définit cette toolbar comme barre d'action de l'activité

        getSupportActionBar().setTitle("Account Type");
        // Définit le titre de la toolbar

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Active le bouton retour dans la toolbar

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        // Affiche le bouton home (retour)

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.LightTextColor)));
        // Définit la couleur de fond de la toolbar

        toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.PrimaryTextColor), PorterDuff.Mode.SRC_ATOP);
        // Change la couleur de l'icône de navigation (flèche retour)

        toolbar.setTitleTextColor(getResources().getColor(R.color.PrimaryTextColor));
        // Définit la couleur du texte du titre

        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        // Récupère l'image utilisateur dans la toolbar

        toolbarUserImage.setVisibility(View.GONE);
        // Cache cette image (pas visible)

        buyAServiceBtn = (RadioButton) findViewById(R.id.account_type_buy_a_service_button);
        // Récupère le bouton radio "Acheter un service"

        sellAServiceBtn = (RadioButton) findViewById(R.id.account_type_sell_a_service_button);
        // Récupère le bouton radio "Vendre un service"

        errorText = (TextView) findViewById(R.id.account_type_error_text);
        // Récupère le TextView pour afficher les erreurs

        errorText.setVisibility(View.INVISIBLE);
        // Cache ce TextView par défaut

        nextBtn = (Button) findViewById(R.id.account_type_next_button);
        // Récupère le bouton "Suivant"

        loginBtn = (TextView) findViewById(R.id.account_type_login_button);
        // Récupère le TextView qui sert de bouton pour se connecter

        position = findViewById(R.id.account_type_position);
        // Récupère le Spinner de la position

        position.setVisibility(View.GONE);
        // Cache le Spinner par défaut

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,R.array.position, android.R.layout.simple_spinner_item);
        // Crée un adaptateur à partir du tableau de chaînes défini dans les ressources (array position)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Définit le layout utilisé pour le menu déroulant du spinner

        position.setAdapter(adapter);
        // Associe l'adaptateur au Spinner

        position.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                // Définit le comportement lors de la sélection d'un item dans le spinner

        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            // Méthode appelée quand un élément est sélectionné

            {
                strPosition = parent.getItemAtPosition(position).toString();
                // Stocke le texte sélectionné dans la variable strPosition
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            // Méthode appelée quand rien n'est sélectionné (vide ici)

            {

            }
        });



        /* button one click action */
        buyAServiceBtn.setOnClickListener(new View.OnClickListener()
                // Définit le comportement au clic sur le bouton "Acheter un service"

        {
            @Override
            public void onClick(View v)
            // Action au clic

            {
                strAccountType = "BUY A SERVICE";
                // Définit le type de compte à "Acheter un service"

                buyAServiceBtn.setTextColor(getResources().getColor(R.color.colorPrimary));
                // Change la couleur du texte du bouton sélectionné

                sellAServiceBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));
                // Change la couleur du texte du bouton non sélectionné

                sellAServiceBtn.setChecked(false);
                // Désactive le bouton radio "Vendre un service"

                position.setVisibility(View.GONE);
                // Cache le spinner de position car non nécessaire pour acheteur

                errorText.setVisibility(View.INVISIBLE);
                // Cache le message d'erreur s'il était visible
            }
        });

        /* button two click action */
        sellAServiceBtn.setOnClickListener(new View.OnClickListener()
                // Définit le comportement au clic sur le bouton "Vendre un service"

        {
            @Override
            public void onClick(View v)
            // Action au clic

            {
                strAccountType = "SELL A SERVICE";
                // Définit le type de compte à "Vendre un service"

                sellAServiceBtn.setTextColor(getResources().getColor(R.color.colorPrimary));
                // Change la couleur du texte du bouton sélectionné

                buyAServiceBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));
                // Change la couleur du texte du bouton non sélectionné

                buyAServiceBtn.setChecked(false);
                // Désactive le bouton radio "Acheter un service"

                position.setVisibility(View.VISIBLE);
                // Affiche le spinner pour sélectionner la position

                errorText.setVisibility(View.INVISIBLE);
                // Cache le message d'erreur s'il était visible
            }
        });


        nextBtn.setOnClickListener(new View.OnClickListener()
                // Définit le comportement au clic sur le bouton "Suivant"

        {
            @Override
            public void onClick(View v)
            // Action au clic

            {
                NextButtonActions();
                // Appelle la méthode pour gérer la logique quand on clique sur suivant
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener()
                // Définit le comportement au clic sur le bouton "Se connecter"

        {
            @Override
            public void onClick(View v)
            // Action au clic

            {
                UserSendToLoginPage();
                // Appelle la méthode pour envoyer l'utilisateur vers la page de connexion
            }
        });

    }



    private void NextButtonActions()
    // Méthode privée pour gérer la logique du bouton suivant

    {
        if(strAccountType.isEmpty())
        // Si aucun type de compte n'est sélectionné

        {
            errorText.setText("Please select the account type.");
            // Affiche un message d'erreur

            errorText.setVisibility(View.VISIBLE);
            // Rend visible le message d'erreur
        }

        if(strAccountType.equals("BUY A SERVICE"))
        // Si l'utilisateur veut acheter un service

        {
            UserSendToRegistrationPage("BUY A SERVICE","null");
            // Envoie vers la page d'inscription en tant qu'acheteur, position "null"
        }

        if(strAccountType.equals("SELL A SERVICE"))
        // Si l'utilisateur veut vendre un service

        {
            if(strPosition.isEmpty() || strPosition.equals("Select Your Position"))
            // Si aucune position n'est sélectionnée ou la valeur par défaut

            {
                errorText.setText("Please select your position.");
                // Affiche un message d'erreur

                errorText.setVisibility(View.VISIBLE);
                // Rend visible le message d'erreur
            }
            else
            // Si une position est sélectionnée

            {
                UserSendToRegistrationPage("SELL A SERVICE",strPosition);
                // Envoie vers la page d'inscription avec le type vendeur et la position choisie
            }
        }
    }



    private void UserSendToLoginPage()
    // Méthode pour rediriger l'utilisateur vers la page de connexion

    {
        Intent loginIntent = new Intent(AccountTypeActivity.this, LoginActivity.class);
        // Crée un intent pour lancer LoginActivity

        startActivity(loginIntent);
        // Lance l'activité LoginActivity

        finish();
        // Termine l'activité courante pour ne pas revenir en arrière
    }


    /* redirect to registration page as a buyer */
    private void UserSendToRegistrationPage(String accountType, String position)
    // Méthode pour rediriger vers la page de création de compte avec paramètres

    {
        Intent createAccountIntent = new Intent(AccountTypeActivity.this, CreateAccountActivity.class);
        // Crée un intent pour lancer CreateAccountActivity

        createAccountIntent.putExtra("AccountType", accountType);
        // Passe le type de compte en extra

        createAccountIntent.putExtra("Position", position);
        // Passe la position en extra

        startActivity(createAccountIntent);
        // Lance l'activité CreateAccountActivity
    }


    /* toolbar back button click action */
    @Override
    // Redéfinition d'une méthode parente

    public boolean onSupportNavigateUp()
    // Méthode appelée quand on clique sur la flèche retour de la toolbar

    {
        onBackPressed();
        // Simule le bouton physique retour

        return true;
        // Indique que l'action a été gérée
    }

}
