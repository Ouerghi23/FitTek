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
import android.widget.Button;
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

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class RequestActivity extends AppCompatActivity {

    // UI elements
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;
    private RecyclerView requestsList;

    // Firebase references
    private FirebaseAuth firebaseAuth;
    private DatabaseReference connectionsDatabaseReference, userDatabaseReference;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        // Initialisation de la toolbar
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Requests");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Masquer l'image utilisateur de la toolbar
        toolbarUserImage = findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Initialisation Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        connectionsDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Connections");

        // Configuration de la liste des demandes
        requestsList = findViewById(R.id.requests_list);
        requestsList.setNestedScrollingEnabled(false);
        requestsList.setHasFixedSize(true);

        // Affichage inversé (du plus récent au plus ancien)
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        requestsList.setLayoutManager(linearLayoutManager);

        // Chargement des demandes
        LoadRequests();
    }

    // Méthode pour charger les demandes de connexion
    private void LoadRequests() {
        // Requête Firebase pour obtenir les demandes avec status "respond"
        Query query = connectionsDatabaseReference.child(currentUserID)
                .orderByChild("connectionStatus")
                .equalTo("respond");

        // Configuration de l'adaptateur FirebaseRecycler
        FirebaseRecyclerOptions<Connections> options =
                new FirebaseRecyclerOptions.Builder<Connections>()
                        .setQuery(query, Connections.class)
                        .build();

        FirebaseRecyclerAdapter<Connections, RequestsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Connections, RequestsViewHolder>(options) {

                    @Override
                    protected void onBindViewHolder(@NonNull final RequestsViewHolder holder, int position, @NonNull Connections model) {
                        // ID de l'utilisateur ayant envoyé la demande
                        final String userID = getRef(position).getKey();

                        // Récupération des données utilisateur pour affichage
                        userDatabaseReference.child(userID).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    // Nom d'utilisateur
                                    if (snapshot.hasChild("username")) {
                                        String name = snapshot.child("username").getValue().toString();
                                        if (!TextUtils.isEmpty(name)) {
                                            holder.userName.setText(name);
                                        }
                                    }

                                    // Image utilisateur
                                    if (snapshot.hasChild("userimage")) {
                                        String image = snapshot.child("userimage").getValue().toString();
                                        if (!TextUtils.isEmpty(image)) {
                                            Picasso.get().load(image)
                                                    .placeholder(R.drawable.default_user_image)
                                                    .into(holder.userImage);
                                        }
                                    }
                                }

                                // Bouton d'acceptation
                                holder.acceptBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // Met à jour les statuts de connexion dans les deux directions
                                        HashMap<String, Object> map1 = new HashMap<>();
                                        map1.put("connectionStatus", "connected");
                                        connectionsDatabaseReference.child(userID).child(currentUserID).updateChildren(map1);

                                        HashMap<String, Object> map2 = new HashMap<>();
                                        map2.put("connectionStatus", "connected");
                                        connectionsDatabaseReference.child(currentUserID).child(userID).updateChildren(map2);
                                    }
                                });

                                // Bouton de refus
                                holder.rejectBtn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        // Supprime la connexion dans les deux directions
                                        connectionsDatabaseReference.child(currentUserID).child(userID).removeValue();
                                        connectionsDatabaseReference.child(userID).child(currentUserID).removeValue();
                                    }
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Gestion d’erreur (optionnelle ici)
                            }
                        });

                        // Cliquer sur le nom ou la photo ouvre le profil de l’utilisateur
                        View.OnClickListener profileListener = v -> UserSendToProfilePage(userID);
                        holder.userName.setOnClickListener(profileListener);
                        holder.userImage.setOnClickListener(profileListener);
                    }

                    @NonNull
                    @Override
                    public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.connection_respond_layout, parent, false);
                        return new RequestsViewHolder(view);
                    }
                };

        // Lancement de l'écoute des données Firebase
        requestsList.setAdapter(adapter);
        adapter.startListening();
    }

    // ViewHolder pour la liste des demandes
    public static class RequestsViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        Button acceptBtn, rejectBtn;
        CircleImageView userImage;

        public RequestsViewHolder(@NonNull View itemView) {
            super(itemView);

            userImage = itemView.findViewById(R.id.connection_respond_userimage);
            userName = itemView.findViewById(R.id.connection_respond_username);
            acceptBtn = itemView.findViewById(R.id.connection_respond_accept_button);
            rejectBtn = itemView.findViewById(R.id.connection_respond_reject_button);
        }}
    // Redirection vers la page de profil d’un autre utilisateur
    private void UserSendToProfilePage(String userID) {
        Intent intent = new Intent(RequestActivity.this, MainActivity.class);
        intent.putExtra("intentFrom", "ViewAnotherUserProfile");
        intent.putExtra("intentUserID", userID);
        startActivity(intent);
    }
    // Retour arrière via bouton toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
