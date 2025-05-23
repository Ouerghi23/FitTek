package com.example.tekfit.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.tekfit.R;
import com.example.tekfit.model.Messages;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessagesActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDatabaseReference, messagesDatabaseReference;

    private RecyclerView chatList;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        // Configuration de la toolbar avec le titre et le bouton de retour
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Messages");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Masquer l'image utilisateur dans la toolbar
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Initialisation de Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();

        // Références aux bases de données Firebase
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        messagesDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Messages").child(currentUserID);

        // Configuration de la RecyclerView
        chatList = (RecyclerView) findViewById(R.id.chat_list);
        chatList.setNestedScrollingEnabled(false);
        chatList.setHasFixedSize(true);

        // Affiche les messages du plus récent au plus ancien
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        chatList.setLayoutManager(linearLayoutManager);

        // Récupère tous les chats
        RetrieveAllChats();
    }

    /**
     * Méthode pour récupérer tous les chats de l'utilisateur actuel
     * et les afficher dans la RecyclerView via FirebaseRecyclerAdapter.
     */
    private void RetrieveAllChats() {
        // Configuration des options pour le RecyclerAdapter
        FirebaseRecyclerOptions<Messages> options = new FirebaseRecyclerOptions.Builder<Messages>()
                .setQuery(messagesDatabaseReference, Messages.class)
                .build();

        // Adapter Firebase pour l'affichage des messages
        FirebaseRecyclerAdapter<Messages, ChatViewHolder> adapter = new FirebaseRecyclerAdapter<Messages, ChatViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatViewHolder chatViewHolder, int i, @NonNull Messages messages) {
                final String userID = getRef(i).getKey(); // Récupère l'ID de l'utilisateur

                if (!userID.isEmpty()) {
                    // Récupération des infos de l'utilisateur (nom et image)
                    userDatabaseReference.child(userID).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                if (dataSnapshot.hasChild("username")) {
                                    String name = dataSnapshot.child("username").getValue().toString();
                                    if (!name.isEmpty()) {
                                        chatViewHolder.userName.setText(name);
                                    }
                                }

                                if (dataSnapshot.hasChild("userimage")) {
                                    String image = dataSnapshot.child("userimage").getValue().toString();
                                    if (!image.isEmpty()) {
                                        Picasso.get().load(image).placeholder(R.drawable.default_user_image).into(chatViewHolder.userImage);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Gérer les erreurs ici si besoin
                        }
                    });
                }

                // Action au clic sur un élément de la liste des messages
                chatViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Lancer l'activité de discussion avec l'utilisateur sélectionné
                        Intent viewMessagesIntent = new Intent(getApplication(), ViewMessagesActivity.class);
                        viewMessagesIntent.putExtra("intentUserID", userID);
                        startActivity(viewMessagesIntent);
                    }
                });
            }

            @NonNull
            @Override
            public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                // Création de la vue pour chaque élément de la liste
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_layout, parent, false);
                return new ChatViewHolder(view);
            }
        };

        // Attache l'adapter à la RecyclerView et commence à écouter les données
        chatList.setAdapter(adapter);
        adapter.startListening();
    }

    /**
     * ViewHolder personnalisé pour les éléments de la liste de discussion
     */
    public static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        CircleImageView userImage;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            userImage = (CircleImageView) itemView.findViewById(R.id.connection_userimage);
            userName = (TextView) itemView.findViewById(R.id.connection_username);
        }
    }

    /**
     * Action pour le bouton de retour dans la toolbar
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
