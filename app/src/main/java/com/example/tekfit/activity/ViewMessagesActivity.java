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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ViewMessagesActivity extends AppCompatActivity
{
    // Déclaration des composants UI
    private Toolbar toolbar; // Barre d'outils en haut de l'activité
    private CircleImageView toolbarUserImage; // Image de profil dans la toolbar

    // Références Firebase
    private FirebaseAuth firebaseAuth; // Authentification Firebase
    private DatabaseReference userDatabaseReference, messagesDatabaseReference; // Références aux bases de données

    // Composants pour l'envoi de messages
    private RecyclerView msgList; // Liste des messages
    private EditText msgInput; // Champ de saisie des messages
    private ImageButton sendMsgBtn; // Bouton d'envoi

    // Variables pour stocker les informations utilisateur
    String intentUserImage, intentUsername; // Image et nom de l'utilisateur avec qui on discute
    String currentUserID, intentUserID; // ID de l'utilisateur courant et de l'interlocuteur

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_messages); // Lie le layout à l'activité

        /* Configuration de la toolbar */
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(""); // Titre vide au départ (sera rempli plus tard)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Active le bouton retour
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        /* Initialisation de l'image utilisateur dans la toolbar */
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);

        // Récupération de l'ID de l'utilisateur avec qui on discute
        Intent intent = getIntent();
        intentUserID = intent.getExtras().getString("intentUserID");

        // Initialisation de Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid(); // ID de l'utilisateur connecté
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users"); // Référence à la table Users
        messagesDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Messages"); // Référence à la table Messages

        // Initialisation des composants UI
        msgInput = findViewById(R.id.view_msg_msg_input); // Champ de saisie
        sendMsgBtn = findViewById(R.id.view_msg_msg_send_button); // Bouton d'envoi

        // Configuration du RecyclerView pour afficher les messages
        msgList = (RecyclerView) findViewById(R.id.messages_list);
        msgList.setNestedScrollingEnabled(false); // Désactive le scroll imbriqué
        msgList.setHasFixedSize(true); // Optimisation pour liste de taille fixe

        // Configuration du layout manager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        /* Configure l'ordre d'affichage (du plus ancien au plus récent) */
        linearLayoutManager.setReverseLayout(false);
        linearLayoutManager.setStackFromEnd(true); // Affiche automatiquement les nouveaux messages
        msgList.setLayoutManager(linearLayoutManager);

        // Gestion du clic sur le bouton d'envoi
        sendMsgBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SendMessage(); // Envoie le message
            }
        });

        // Gestion du clic sur l'image de profil dans la toolbar
        toolbarUserImage.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                UserSendToProfilePage(); // Redirige vers le profil de l'utilisateur
            }
        });

        // Récupération des détails de l'utilisateur avec qui on discute
        RetrieveIntentUserDetails();

        // Chargement de tous les messages
        RetrieveAllMessages();
    }

    // Méthode pour charger et afficher les messages depuis Firebase
    private void RetrieveAllMessages()
    {
        // Requête pour récupérer les messages entre les deux utilisateurs
        Query query = messagesDatabaseReference.child(currentUserID).child(intentUserID);

        // Configuration des options pour le FirebaseRecyclerAdapter
        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Messages>()
                .setQuery(query, Messages.class)
                .build();

        // Création de l'adapter pour le RecyclerView
        FirebaseRecyclerAdapter<Messages, MessageViewHolder> adapter = new FirebaseRecyclerAdapter<Messages, MessageViewHolder>(options)
        {
            @Override
            protected void onBindViewHolder(@NonNull MessageViewHolder messageViewHolder, int i, @NonNull Messages messages)
            {
                // Cache les deux côtés (émetteur/récepteur) par défaut
                messageViewHolder.receiverSide.setVisibility(View.GONE);
                messageViewHolder.senderSide.setVisibility(View.GONE);

                String receiver = messages.getReceiver();
                String sender = messages.getSender();

                // Affiche le message à droite si c'est l'utilisateur courant qui l'a envoyé
                if(sender.equals(currentUserID))
                {
                    messageViewHolder.senderSide.setVisibility(View.VISIBLE);
                    messageViewHolder.senderMsg.setText(messages.getMessage());
                    messageViewHolder.senderMsgTime.setText(messages.getTime());
                }

                // Affiche le message à gauche si c'est l'interlocuteur qui l'a envoyé
                if(receiver.equals(currentUserID))
                {
                    messageViewHolder.receiverSide.setVisibility(View.VISIBLE);
                    messageViewHolder.receiverMsg.setText(messages.getMessage());
                    messageViewHolder.receiverMsgTime.setText(messages.getTime());
                }
            }

            @NonNull
            @Override
            public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                // Création d'une nouvelle vue pour chaque message
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_layout,parent,false);
                MessageViewHolder messageViewHolder = new MessageViewHolder(view);
                return messageViewHolder;
            }
        };

        // Liaison de l'adapter au RecyclerView
        msgList.setAdapter(adapter);
        adapter.startListening(); // Démarre l'écoute des nouveaux messages
    }

    // ViewHolder pour les éléments de la liste des messages
    public static class MessageViewHolder extends RecyclerView.ViewHolder
    {
        // Déclaration des vues du layout
        TextView receiverMsg, receiverMsgTime, senderMsg, senderMsgTime;
        LinearLayout receiverSide, senderSide;

        public MessageViewHolder(@NonNull View itemView)
        {
            super(itemView);

            // Initialisation des vues
            receiverMsg = itemView.findViewById(R.id.message_layout_receiver_message);
            receiverSide = itemView.findViewById(R.id.message_layout_receiver_side);
            receiverMsgTime = itemView.findViewById(R.id.message_layout_receiver_message_time);
            senderMsg = itemView.findViewById(R.id.message_layout_sender_message);
            senderSide = itemView.findViewById(R.id.message_layout_sender_side);
            senderMsgTime = itemView.findViewById(R.id.message_layout_sender_message_time);
        }
    }

    // Méthode pour envoyer un nouveau message
    private void SendMessage()
    {
        if(!TextUtils.isEmpty(msgInput.getText().toString()))
        {
            /* Récupération de la date et heure actuelles */
            Calendar calendar = Calendar.getInstance();

            // Formatage pour l'affichage
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy");
            String currentDate = simpleDateFormat.format(calendar.getTime());
            SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("hh:mm aa");
            String currentTime = simpleTimeFormat.format(calendar.getTime());

            // Formatage pour l'ID unique du message
            SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyyMMdd");
            String date = simpleDateFormat2.format(calendar.getTime());
            SimpleDateFormat simpleTimeFormat2 = new SimpleDateFormat("HHmmss");
            String time = simpleTimeFormat2.format(calendar.getTime());
            String randomMsgID = date + time; // ID unique basé sur la date/heure

            // Enregistrement du message dans la conversation de l'émetteur
            HashMap msgMap1 = new HashMap();
            msgMap1.put("sender", currentUserID);
            msgMap1.put("receiver", intentUserID);
            msgMap1.put("message", msgInput.getText().toString());
            msgMap1.put("time", currentDate+" • "+currentTime);
            messagesDatabaseReference.child(currentUserID).child(intentUserID).child(randomMsgID).updateChildren(msgMap1);

            // Enregistrement du message dans la conversation du récepteur
            HashMap msgMap2 = new HashMap();
            msgMap2.put("sender", currentUserID);
            msgMap2.put("receiver", intentUserID);
            msgMap2.put("message", msgInput.getText().toString());
            msgMap2.put("time", currentDate+" • "+currentTime);
            messagesDatabaseReference.child(intentUserID).child(currentUserID).child(randomMsgID).updateChildren(msgMap2);

            // Réinitialisation du champ de saisie
            msgInput.setText("");
        }
    }

    // Méthode pour récupérer les détails de l'interlocuteur
    private void RetrieveIntentUserDetails()
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
                        intentUsername = dataSnapshot.child("username").getValue().toString();
                        if(!intentUsername.isEmpty())
                        {
                            getSupportActionBar().setTitle(intentUsername); // Met à jour le titre de la toolbar
                        }
                    }

                    // Récupération de l'image de profil
                    if(dataSnapshot.hasChild("userimage"))
                    {
                        intentUserImage = dataSnapshot.child("userimage").getValue().toString();
                        if(!intentUserImage.isEmpty())
                        {
                            Picasso.get().load(intentUserImage)
                                    .placeholder(R.drawable.profile_image_placeholder)
                                    .into(toolbarUserImage); // Charge l'image dans la toolbar
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError)
            {
                // Gestion des erreurs
            }
        });
    }

    // Méthode pour rediriger vers le profil de l'utilisateur
    private void UserSendToProfilePage()
    {
        Intent mainIntent = new Intent(ViewMessagesActivity.this, MainActivity.class);
        mainIntent.putExtra("intentFrom", "ViewAnotherUserProfile");
        mainIntent.putExtra("intentUserID", intentUserID);
        startActivity(mainIntent);
    }

    /* Gestion du bouton retour dans la toolbar */
    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed(); // Appelle la méthode standard de retour
        return true;
    }
}