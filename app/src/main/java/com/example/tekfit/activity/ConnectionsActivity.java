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
import android.widget.TextView;

import com.example.tekfit.model.Connections;
import com.example.tekfit.R;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConnectionsActivity extends AppCompatActivity
{
    // Barre d'outils en haut de l'écran
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    // Références Firebase pour l'authentification et la base de données
    private FirebaseAuth firebaseAuth;
    private DatabaseReference connectionsDatabaseReference, userDatabaseReference;

    // RecyclerView pour afficher la liste des connexions
    private RecyclerView connectionsList;

    // ID de l'utilisateur actuellement connecté
    private String currentUserID;

    // Variables pour gérer les intents entrants
    String intentFrom, intentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connections);

        // Initialisation de la toolbar et définition du titre
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Connections");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);  // Affiche le bouton retour
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Image utilisateur dans la toolbar (masquée ici)
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Récupération des données passées via intent
        Intent intent = getIntent();
        intentFrom = intent.getExtras().getString("intentFrom");
        if(intentFrom.equals("OtherUserProfile"))
        {
            // Si on vient du profil d'un autre utilisateur, récupérer son ID
            intentUserID = intent.getExtras().getString("intentUserID");
        }

        // Initialisation Firebase Auth et Database
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        connectionsDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Connections");

        // Initialisation du RecyclerView pour afficher les connexions
        connectionsList = (RecyclerView) findViewById(R.id.connections_list);
        connectionsList.setNestedScrollingEnabled(false);
        connectionsList.setHasFixedSize(true);

        // Configuration du layout manager pour RecyclerView (ordre inversé)
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        connectionsList.setLayoutManager(linearLayoutManager);

        // Chargement des connexions et détails utilisateur selon le contexte de l'intent
        if(intentFrom.equals("OtherUserProfile") && !currentUserID.equals(intentUserID))
        {
            LoadConnections(intentUserID);
            GetUserDetails(intentUserID);
        }
        else
        {
            LoadConnections(currentUserID);
            GetUserDetails(currentUserID);
        }
    }

    /**
     * Récupère et affiche le nom de l'utilisateur dans la toolbar
     * @param userID ID de l'utilisateur dont on veut récupérer le nom
     */
    private void GetUserDetails(String userID)
    {
        userDatabaseReference.child(userID).addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
            {
                if(dataSnapshot.hasChild("username"))
                {
                    String retrieveUserName = dataSnapshot.child("username").getValue().toString();
                    if(intentFrom.equals("OtherUserProfile"))
                    {
                        if(!TextUtils.isEmpty(retrieveUserName))
                        {
                            // Récupère le prénom pour le titre (avant le premier espace)
                            String arr[] = retrieveUserName.split(" ", 2);
                            getSupportActionBar().setTitle(arr[0]+"'s"+" Connections");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
                // Gestion d'erreur non implémentée
            }
        });
    }

    /**
     * Charge et affiche la liste des connexions d'un utilisateur donné
     * @param userID ID de l'utilisateur dont on veut afficher les connexions
     */
    private void LoadConnections(String userID)
    {
        // Query pour récupérer uniquement les connexions dont le statut est "connected"
        Query query = connectionsDatabaseReference.child(userID).orderByChild("connectionStatus").equalTo("connected");

        // Options pour FirebaseRecyclerAdapter avec la classe Connections
        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Connections>()
                .setQuery(query, Connections.class)
                .build();

        // Adapter Firebase pour relier les données Firebase à la RecyclerView
        FirebaseRecyclerAdapter<Connections,ConnectionsViewHolder> adapter = new FirebaseRecyclerAdapter<Connections, ConnectionsViewHolder>(options)
        {
            @Override
            protected void onBindViewHolder(@NonNull final ConnectionsViewHolder connectionsViewHolder, int i, @NonNull Connections connections)
            {
                // Récupération de l'ID utilisateur de la connexion actuelle dans la liste
                final String userID = getRef(i).getKey();

                // Récupérer les détails de l'utilisateur (nom, image) depuis la base Firebase
                userDatabaseReference.child(userID).addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                    {
                        if(dataSnapshot.exists())
                        {
                            if(dataSnapshot.hasChild("username"))
                            {
                                String name = dataSnapshot.child("username").getValue().toString();
                                if(!TextUtils.isEmpty(name))
                                {
                                    connectionsViewHolder.userName.setText(name);
                                }
                            }

                            if(dataSnapshot.hasChild("userimage"))
                            {
                                String image = dataSnapshot.child("userimage").getValue().toString();
                                if(!TextUtils.isEmpty(image))
                                {
                                    // Charge l'image utilisateur avec Picasso
                                    Picasso.get()
                                            .load(image)
                                            .placeholder(R.drawable.default_user_image) // image par défaut
                                            .into(connectionsViewHolder.userImage);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError)
                    {
                        // Gestion d'erreur non implémentée
                    }
                });

                // Définition du clic sur un item : redirection vers la page profil de l'utilisateur
                connectionsViewHolder.itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        UserSendToProfilePage(userID);
                    }
                });
            }

            @NonNull
            @Override
            public ConnectionsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                // Inflater le layout XML de chaque item de la liste
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_layout,parent,false);
                ConnectionsViewHolder connectionsViewHolder = new ConnectionsViewHolder(view);
                return connectionsViewHolder;
            }
        };

        // Affecter l'adapter au RecyclerView et commencer à écouter les changements
        connectionsList.setAdapter(adapter);
        adapter.startListening();
    }

    /**
     * ViewHolder pour RecyclerView : représente un item (une connexion)
     */
    public static class ConnectionsViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName;
        CircleImageView userImage;

        public ConnectionsViewHolder(@NonNull View itemView)
        {
            super(itemView);

            userImage = (CircleImageView) itemView.findViewById(R.id.connection_userimage);
            userName = (TextView) itemView.findViewById(R.id.connection_username);
        }
    }

    /**
     * Méthode pour envoyer l'utilisateur vers la page profil d'un autre utilisateur
     * @param userID ID de l'utilisateur dont on veut afficher le profil
     */
    private void UserSendToProfilePage(String userID)
    {
        Intent mainIntent = new Intent(ConnectionsActivity.this, MainActivity.class);
        mainIntent.putExtra("intentFrom", "ViewAnotherUserProfile");
        mainIntent.putExtra("intentUserID", userID);
        startActivity(mainIntent);
    }

    /**
     * Gestion du clic sur le bouton retour dans la toolbar
     */
    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed();  // revient à l'activité précédente
        return true;
    }
}
