package com.example.tekfit.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.tekfit.fragment.FeedFragment;
import com.example.tekfit.fragment.HomeFragment;
import com.example.tekfit.fragment.ProfileFragment;
import com.example.tekfit.fragment.ServicesFragment;
import com.example.tekfit.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
{
    // Déclaration des éléments d'interface et Firebase
    private Toolbar mToolBar;
    private CircleImageView toolbarUserImage;
    private ImageButton toolBarProfileEditBtn;
    private ImageView toolbarLogo, toolbarLogoText;

    private BottomNavigationView bottomNavigationView;

    private String intentFrom, intentUserID;
    private String currentUserID;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDatabaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation de FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        // Récupération des données de l'intent
        Intent intent = getIntent();
        intentFrom = intent.getExtras().getString("intentFrom");

        // Initialisation de la barre de navigation du bas
        bottomNavigationView = findViewById(R.id.bottom_navigation_bar);

        // Initialisation des éléments du toolbar
        toolbarLogo = findViewById(R.id.toolbar_logo);
        toolbarLogo.setVisibility(View.VISIBLE);
        toolbarLogoText = findViewById(R.id.toolbar_logo_text);
        toolbarLogoText.setVisibility(View.INVISIBLE);
        toolbarUserImage = findViewById(R.id.toolbar_user_image);
        toolBarProfileEditBtn = findViewById(R.id.toolbar_profile_edit_button);

        // Rediriger vers la page de configuration lors du clic sur l’icône d’édition
        toolBarProfileEditBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UserSendToSetupPage();
            }
        });

        // Configuration du Toolbar sans titre
        mToolBar = findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolBar);
        getSupportActionBar().setTitle("");

        // Chargement initial du bon fragment selon l'origine de l'intent
        if ((savedInstanceState == null) && (currentUser != null))
        {
            if (intentFrom.equals("CreatePostActivity"))
            {
                // Rediriger vers le fragment Feed après création d’un post
                bottomNavigationView.getMenu().getItem(1).setChecked(true);
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.main_frame, new FeedFragment());
                fragmentTransaction.commit();
            }
            else if (intentFrom.equals("ViewAnotherUserProfile"))
            {
                // Rediriger vers le profil d’un autre utilisateur
                intentUserID = intent.getExtras().getString("intentUserID");
                currentUserID = firebaseAuth.getCurrentUser().getUid();

                bottomNavigationView.getMenu().getItem(3).setChecked(true);
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.main_frame, new ProfileFragment());
                fragmentTransaction.commit();

                toolbarUserImage.setVisibility(View.GONE);
                toolBarProfileEditBtn.setVisibility(View.VISIBLE);

                // Si ce n'est pas le profil de l'utilisateur actuel, masquer les boutons
                if (!currentUserID.equals(intentUserID))
                {
                    bottomNavigationView.setVisibility(View.GONE);
                    toolbarUserImage.setVisibility(View.GONE);
                    toolBarProfileEditBtn.setVisibility(View.GONE);
                    getSupportActionBar().setTitle("Profile");
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                    getSupportActionBar().setDisplayShowHomeEnabled(true);
                    toolbarLogo.setVisibility(View.GONE);
                    toolbarLogoText.setVisibility(View.GONE);
                }
            }
            else if (intentFrom.equals("ProfileFragment"))
            {
                // Revenir sur le profil après modification
                bottomNavigationView.getMenu().getItem(3).setChecked(true);
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.main_frame, new ProfileFragment());
                fragmentTransaction.commit();

                toolbarUserImage.setVisibility(View.GONE);
                toolBarProfileEditBtn.setVisibility(View.VISIBLE);
            }
            else
            {
                // Cas par défaut : redirection vers HomeFragment
                bottomNavigationView.getMenu().getItem(0).setChecked(true);
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.main_frame, new HomeFragment());
                fragmentTransaction.commit();

                bottomNavigationView.setVisibility(View.VISIBLE);
            }
        }

        // Si utilisateur connecté, afficher son image dans le toolbar
        if (currentUser != null)
        {
            SetToolbarUserImage();
        }

        // Gestion du clic sur les éléments de la barre de navigation inférieure
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
            {
                int id = menuItem.getItemId();

                if (id == R.id.bottom_nav_home)
                {
                    bottomNavigationView.getMenu().getItem(0).setChecked(true);
                    loadFragment(new HomeFragment());
                    toolbarUserImage.setVisibility(View.VISIBLE);
                    toolBarProfileEditBtn.setVisibility(View.GONE);
                }

                if (id == R.id.bottom_nav_feed)
                {
                    bottomNavigationView.getMenu().getItem(1).setChecked(true);
                    loadFragment(new FeedFragment());
                    toolbarUserImage.setVisibility(View.VISIBLE);
                    toolBarProfileEditBtn.setVisibility(View.GONE);
                }

                if (id == R.id.bottom_nav_services)
                {
                    bottomNavigationView.getMenu().getItem(2).setChecked(true);
                    loadFragment(new ServicesFragment());
                    toolbarUserImage.setVisibility(View.VISIBLE);
                    toolBarProfileEditBtn.setVisibility(View.GONE);
                }

                if (id == R.id.bottom_nav_profile)
                {
                    bottomNavigationView.getMenu().getItem(3).setChecked(true);
                    loadFragment(new ProfileFragment());
                    toolbarUserImage.setVisibility(View.GONE);
                    toolBarProfileEditBtn.setVisibility(View.VISIBLE);
                }

                return false;
            }
        });

        // Si l'utilisateur clique sur sa photo, on le redirige vers le profil
        toolbarUserImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                bottomNavigationView.getMenu().getItem(3).setChecked(true);
                loadFragment(new ProfileFragment());
                toolbarUserImage.setVisibility(View.GONE);
                toolBarProfileEditBtn.setVisibility(View.VISIBLE);
            }
        });

    }

    // Redirection vers la page de configuration du profil
    private void UserSendToSetupPage()
    {
        Intent SetupIntent = new Intent(MainActivity.this, SetupActivity.class);
        SetupIntent.putExtra("IntentFrom", "ProfileFragment");
        startActivity(SetupIntent);
    }

    // Vérifie à chaque lancement si l'utilisateur est connecté
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null)
        {
            UserSendToWelcomePage();
        }
    }

    // Charge la photo de profil de l'utilisateur dans la toolbar
    private void SetToolbarUserImage()
    {
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(currentUserID);

        userDatabaseReference.addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists() && dataSnapshot.hasChild("userimage"))
                {
                    String image = dataSnapshot.child("userimage").getValue().toString();
                    if (!image.equals(""))
                    {
                        Picasso.get().load(image).placeholder(R.drawable.default_user_image).into(toolbarUserImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    // Rediriger l'utilisateur non connecté vers la page de bienvenue
    private void UserSendToWelcomePage()
    {
        Intent welcomeIntent = new Intent(MainActivity.this, WelcomeActivity.class);
        welcomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(welcomeIntent);
        finish();
    }

    // Gestion du bouton retour dans la toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Fonction utilitaire pour charger un fragment
    private void loadFragment(androidx.fragment.app.Fragment fragment)
    {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.main_frame, fragment);
        fragmentTransaction.commit();
    }
}
