package com.example.tekfit.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.tekfit.R;
import com.example.tekfit.model.UserServices;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ServiceActivity extends AppCompatActivity
{
    // Déclaration des variables de classe

        private Toolbar toolbar; // Barre d'outils en haut de l'activité
        private CircleImageView toolbarUserImage; // Image circulaire de l'utilisateur dans la toolbar

        private FirebaseAuth firebaseAuth; // Authentification Firebase
        private DatabaseReference userDatabaseReference, serviceDatabaseReference; // Références aux bases de données Firebase

        private String username, userPosition, userImage; // Informations sur l'utilisateur

        String currentUserID, intentFrom, intentUserID; // ID utilisateur et informations sur l'intent

        private FloatingActionButton createServicesButton; // Bouton flottant pour créer un service

        private RecyclerView serviceList; // Liste des services (RecyclerView)

        @Override
        protected void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_service); // Lie le layout XML à cette activité


            /* Initialisation de la toolbar avec titre et bouton de retour */
            toolbar = (Toolbar) findViewById(R.id.main_toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle("Services"); // Titre de la toolbar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Active le bouton retour
            getSupportActionBar().setDisplayShowHomeEnabled(true); // Affiche le bouton retour


            /* Cache l'image utilisateur dans la toolbar */
            toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
            toolbarUserImage.setVisibility(View.GONE);


            // Récupère les données passées via l'intent
            Intent intent = getIntent();
            intentFrom = intent.getExtras().getString("intentFrom");
            if(intentFrom.equals("ViewAnotherUserProfile") || intentFrom.equals("MyProfile"))
            {
                intentUserID = intent.getExtras().getString("intentUserID");
            }


            // Initialisation de Firebase
            firebaseAuth = FirebaseAuth.getInstance();
            currentUserID = firebaseAuth.getCurrentUser().getUid(); // ID de l'utilisateur connecté
            userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users"); // Référence à la table Users
            serviceDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Services"); // Référence à la table Services

            // Configuration du RecyclerView pour afficher la liste des services
            serviceList = (RecyclerView)findViewById(R.id.services_services_list);
            serviceList.setNestedScrollingEnabled(false); // Désactive le scroll imbriqué

            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
            /* Configure l'ordre d'affichage des services (du plus récent au plus ancien) */
            linearLayoutManager.setReverseLayout(true);
            linearLayoutManager.setStackFromEnd(true);
            serviceList.setLayoutManager(linearLayoutManager);


            // Configuration du bouton flottant pour créer un service
            createServicesButton = (FloatingActionButton) findViewById(R.id.service_create_service_button);
            // Cache le bouton si l'utilisateur courant n'est pas celui dont on voit les services
            if(!currentUserID.equals(intentUserID))
            {
                createServicesButton.setVisibility(View.GONE);
            }
            createServicesButton.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    UserSendToCreateServicePage(); // Ouvre l'activité de création de service
                }
            });

            DisplayUserAllServices(); // Affiche tous les services de l'utilisateur
            RetrieveUserDetails(); // Récupère les détails de l'utilisateur
        }


        // Récupère les informations de l'utilisateur depuis Firebase
        private void RetrieveUserDetails()
        {
            userDatabaseReference.child(intentUserID).addValueEventListener(new ValueEventListener()
            {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    if(dataSnapshot.exists())
                    {
                        // Récupération du nom d'utilisateur
                        if(dataSnapshot.hasChild("username"))
                        {
                            username = dataSnapshot.child("username").getValue().toString();
                            if(intentFrom.equals("ViewAnotherUserProfile"))
                            {
                                if(!TextUtils.isEmpty(username))
                                {
                                    /* Met à jour le titre de la toolbar avec le prénom de l'utilisateur */
                                    String arr[] = username.split(" ", 2);
                                    getSupportActionBar().setTitle(arr[0]+"'s"+" Services");
                                }
                            }
                        }

                        // Récupération de la position de l'utilisateur
                        if(dataSnapshot.hasChild("userposition"))
                        {
                            userPosition = dataSnapshot.child("userposition").getValue().toString();
                        }

                        // Récupération de l'image de profil de l'utilisateur
                        if(dataSnapshot.hasChild("userimage"))
                        {
                            userImage = dataSnapshot.child("userimage").getValue().toString();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError)
                {
                    // Gestion des erreurs de lecture de la base de données
                }
            });
        }


        // Affiche tous les services de l'utilisateur dans le RecyclerView
        private void DisplayUserAllServices()
        {
            // Requête pour récupérer les services de l'utilisateur spécifique
            Query currentUserServices = serviceDatabaseReference.orderByChild("userid").equalTo(intentUserID);

            // Configuration des options pour le FirebaseRecyclerAdapter
            FirebaseRecyclerOptions options =
                    new FirebaseRecyclerOptions.Builder<UserServices>()
                            .setQuery(currentUserServices, UserServices.class)
                            .build();

            // Création de l'adapter pour le RecyclerView
            FirebaseRecyclerAdapter<UserServices, ServiceViewHolder> adapter = new FirebaseRecyclerAdapter<UserServices, ServiceViewHolder>(options)
            {
                @Override
                protected void onBindViewHolder(@NonNull final ServiceViewHolder serviceViewHolder, int i, @NonNull UserServices userServices)
                {
                    final String serviceID = getRef(i).getKey(); // ID du service

                    // Remplissage des vues avec les données du service
                    serviceViewHolder.serviceTitle.setText("Service : "+userServices.getServicetitle());
                    serviceViewHolder.serviceDate.setText(userServices.servicedate);
                    serviceViewHolder.serviceTime.setText(userServices.servicetime);
                    Picasso.get().load(userServices.getServicefirstimage()).into(serviceViewHolder.serviceFirstImage);
                    serviceViewHolder.serviceSummaryDescription.setText(userServices.servicesummarydescription);

                    // Affichage des informations de l'utilisateur
                    if(!username.isEmpty())
                    {
                        serviceViewHolder.serviceOwnerUsername.setText(username);
                    }

                    if(!userImage.isEmpty())
                    {
                        Picasso.get().load(userImage).placeholder(R.drawable.profile_image_placeholder).into(serviceViewHolder.serviceOwnerImage);
                    }

                    if(!userPosition.isEmpty())
                    {
                        serviceViewHolder.serviceOwnerPosition.setText(" • "+userPosition);
                    }

                    /* Gestion du clic sur un service pour voir ses détails */
                    serviceViewHolder.itemView.setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            Intent viewServiceIntent = new Intent(ServiceActivity.this, ViewServiceActivity.class);
                            viewServiceIntent.putExtra("intentPurpose", "MyServices");
                            viewServiceIntent.putExtra("intentServiceID", serviceID);
                            startActivity(viewServiceIntent);
                        }
                    });
                }

                @NonNull
                @Override
                public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
                {
                    // Crée une nouvelle vue pour chaque élément de la liste
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.service_layout, parent, false);
                    ServiceViewHolder serviceViewHolder = new ServiceViewHolder(view);
                    return serviceViewHolder;
                }
            };

            serviceList.setAdapter(adapter); // Attache l'adapter au RecyclerView
            adapter.startListening(); // Démarre l'écoute des changements dans la base de données
        }


        // ViewHolder pour les éléments de la liste de services
        public static class ServiceViewHolder extends RecyclerView.ViewHolder
        {
            TextView serviceTitle, serviceDate, serviceTime, serviceSummaryDescription, serviceOwnerUsername, serviceOwnerPosition;
            ImageView serviceFirstImage;
            CircleImageView serviceOwnerImage;

            public ServiceViewHolder(@NonNull View itemView)
            {
                super(itemView);

                // Initialisation des vues du layout d'un service
                serviceTitle = (TextView) itemView.findViewById(R.id.service_title);
                serviceDate = (TextView) itemView.findViewById(R.id.service_date);
                serviceTime = (TextView) itemView.findViewById(R.id.service_time);
                serviceFirstImage = (ImageView) itemView.findViewById(R.id.service_first_image);
                serviceSummaryDescription = (TextView) itemView.findViewById(R.id.service_summary_description);
                serviceOwnerImage = (CircleImageView) itemView.findViewById(R.id.service_owner_image);
                serviceOwnerUsername = (TextView) itemView.findViewById(R.id.service_owner_username);
                serviceOwnerPosition = (TextView) itemView.findViewById(R.id.service_owner_position);
            }
        }


        // Ouvre l'activité de création de service
        private void UserSendToCreateServicePage()
        {
            Intent createServiceIntent = new Intent(ServiceActivity.this, CreateServiceActivity.class);
            startActivity(createServiceIntent);
        }


        /* Gestion du bouton retour dans la toolbar */
        @Override
        public boolean onSupportNavigateUp()
        {
            onBackPressed(); // Appelle la méthode standard de retour
            return true;
        }
    }