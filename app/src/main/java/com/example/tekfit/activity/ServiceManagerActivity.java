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
import android.widget.Button;
import android.widget.TextView;

import com.example.tekfit.R;
import com.example.tekfit.model.Orders;
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

public class ServiceManagerActivity extends AppCompatActivity
{
    // Déclaration des composants UI
    private Toolbar toolbar; // Barre d'outils en haut de l'activité
    private CircleImageView toolbarUserImage; // Image de profil dans la toolbar

    // Références Firebase
    private FirebaseAuth firebaseAuth; // Authentification Firebase
    private DatabaseReference servicesDatabaseReference, ordersDatabaseReference, userDatabaseReference; // Références aux bases de données

    // RecyclerView pour afficher les commandes de service
    private RecyclerView serviceRequestsList;

    // Variables pour stocker les informations utilisateur
    String currentUserID, intentFrom; // ID de l'utilisateur courant et origine de l'intent

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_manager); // Lie le layout à l'activité

        /* Configuration de la toolbar */
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Services Manager"); // Titre de l'activité
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Active le bouton retour
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        /* Cache l'image utilisateur dans la toolbar */
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Récupération des données passées via l'intent
        Intent intent = getIntent();
        intentFrom = intent.getExtras().getString("intentFrom");

        // Initialisation de Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid(); // ID de l'utilisateur connecté
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users"); // Référence à la table Users
        ordersDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Orders"); // Référence à la table Orders
        servicesDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Services"); // Référence à la table Services

        // Configuration du RecyclerView
        serviceRequestsList = (RecyclerView) findViewById(R.id.service_order_list);
        serviceRequestsList.setNestedScrollingEnabled(false); // Désactive le scroll imbriqué
        serviceRequestsList.setHasFixedSize(true); // Optimisation pour liste de taille fixe

        // Configuration du layout manager
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        /* Configure l'ordre d'affichage (du plus récent au plus ancien) */
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        serviceRequestsList.setLayoutManager(linearLayoutManager);

        // Charge les commandes de service
        LoadServiceOrders();
    }

    // Méthode pour charger les commandes de service depuis Firebase
    private void LoadServiceOrders()
    {
        // Requête pour récupérer les commandes où l'utilisateur courant est le vendeur
        Query query = ordersDatabaseReference.orderByChild("seller").equalTo(currentUserID);

        // Configuration des options pour le FirebaseRecyclerAdapter
        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Orders>()
                .setQuery(query, Orders.class)
                .build();

        // Création de l'adapter pour le RecyclerView
        FirebaseRecyclerAdapter<Orders,ServiceOrdersViewHolder> adapter = new FirebaseRecyclerAdapter<Orders, ServiceOrdersViewHolder>(options)
        {
            @Override
            protected void onBindViewHolder(@NonNull final ServiceOrdersViewHolder serviceOrdersViewHolder, int i, @NonNull final Orders orders)
            {
                /* Récupération de l'ID de la commande */
                final String orderID = getRef(i).getKey();

                // Gestion des différents statuts de commande
                final String orderStatus = orders.getOrderStatus();
                if(orderStatus.equals("pending") || orderStatus.equals("accepted") || orderStatus.equals("rejected") ||
                        orderStatus.equals("finalized")  || orderStatus.equals("finalizedWithRatings") || orderStatus.equals("cancelled"))
                {
                    // Affichage du statut de la commande
                    if(orderStatus.equals("pending")) { serviceOrdersViewHolder.orderStatus.setText("Order Status : Pending"); }
                    if(orderStatus.equals("accepted")) { serviceOrdersViewHolder.orderStatus.setText("Order Status : Accepted by You"); }
                    if(orderStatus.equals("rejected")) { serviceOrdersViewHolder.orderStatus.setText("Order Status : Rejected by You"); }
                    if(orderStatus.equals("finalized")) { serviceOrdersViewHolder.orderStatus.setText("Order Status : Finalized"); }
                    if(orderStatus.equals("finalizedWithRatings")) { serviceOrdersViewHolder.orderStatus.setText("Order Status : Finalized and, Rated by Buyer"); }
                    if(orderStatus.equals("cancelled")) { serviceOrdersViewHolder.orderStatus.setText("Order Status : Cancelled by Buyer"); }

                    /* Affichage de la date et heure de la commande */
                    String date = orders.getDate();
                    serviceOrdersViewHolder.orderDate.setText("Order Time : "+date);

                    String time = orders.getTime();
                    serviceOrdersViewHolder.orderTime.setText(" • "+time);

                    /* Récupération et affichage des détails de l'acheteur */
                    String buyer = orders.getBuyer();
                    if(!buyer.isEmpty())
                    {
                        userDatabaseReference.child(buyer).addValueEventListener(new ValueEventListener()
                        {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                            {
                                if(dataSnapshot.exists())
                                {
                                    // Chargement de l'image de profil
                                    if(dataSnapshot.hasChild("userimage"))
                                    {
                                        String userimage = dataSnapshot.child("userimage").getValue().toString();
                                        if(!userimage.isEmpty())
                                        {
                                            Picasso.get().load(userimage).placeholder(R.drawable.default_user_image).into(serviceOrdersViewHolder.userImage);
                                        }
                                    }

                                    // Affichage du nom d'utilisateur
                                    if(dataSnapshot.hasChild("username"))
                                    {
                                        String username = dataSnapshot.child("username").getValue().toString();
                                        if(!username.isEmpty())
                                        {
                                            serviceOrdersViewHolder.userName.setText(username);
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

                    /* Récupération et affichage des détails du service */
                    String serviceID = orders.getServiceID();
                    if(!serviceID.isEmpty())
                    {
                        servicesDatabaseReference.child(serviceID).addValueEventListener(new ValueEventListener()
                        {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                            {
                                if(dataSnapshot.exists())
                                {
                                    if(dataSnapshot.hasChild("servicetitle"))
                                    {
                                        String serviceTitle = dataSnapshot.child("servicetitle").getValue().toString();
                                        if(!serviceTitle.isEmpty())
                                        {
                                            serviceOrdersViewHolder.serviceTitle.setText("Service : "+serviceTitle);
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
                }
                else
                {
                    /* Masquage des commandes non valides */
                    serviceOrdersViewHolder.itemView.setVisibility(View.GONE);
                    serviceOrdersViewHolder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                }

                // Configuration des boutons en fonction du statut de la commande
                if(orderStatus.equals("pending"))
                {
                    // Bouton Accepter actif
                    serviceOrdersViewHolder.acceptBtn.setText("Accept");
                    serviceOrdersViewHolder.acceptBtn.setEnabled(true);
                    serviceOrdersViewHolder.acceptBtn.setAlpha(1f);
                    serviceOrdersViewHolder.acceptBtn.setTextColor(getResources().getColor(R.color.colorPrimary));

                    // Bouton Rejeter actif
                    serviceOrdersViewHolder.rejectBtn.setText("Reject");
                    serviceOrdersViewHolder.rejectBtn.setEnabled(true);
                    serviceOrdersViewHolder.rejectBtn.setAlpha(1f);
                    serviceOrdersViewHolder.rejectBtn.setTextColor(getResources().getColor(R.color.WarningTextColor));

                    // Bouton Finaliser inactif
                    serviceOrdersViewHolder.finalizeBtn.setText("Finalize the Order");
                    serviceOrdersViewHolder.finalizeBtn.setEnabled(false);
                    serviceOrdersViewHolder.finalizeBtn.setAlpha(.5f);
                    serviceOrdersViewHolder.finalizeBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));
                }

                if(orderStatus.equals("accepted"))
                {
                    // Bouton Accepter inactif (déjà accepté)
                    serviceOrdersViewHolder.acceptBtn.setText("Accepted");
                    serviceOrdersViewHolder.acceptBtn.setEnabled(false);
                    serviceOrdersViewHolder.acceptBtn.setAlpha(.5f);
                    serviceOrdersViewHolder.acceptBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));

                    // Bouton Rejeter inactif
                    serviceOrdersViewHolder.rejectBtn.setText("Reject");
                    serviceOrdersViewHolder.rejectBtn.setEnabled(false);
                    serviceOrdersViewHolder.rejectBtn.setAlpha(.5f);
                    serviceOrdersViewHolder.rejectBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));

                    // Bouton Finaliser actif
                    serviceOrdersViewHolder.finalizeBtn.setText("Finalize the Order");
                    serviceOrdersViewHolder.finalizeBtn.setEnabled(true);
                    serviceOrdersViewHolder.finalizeBtn.setAlpha(1f);
                    serviceOrdersViewHolder.finalizeBtn.setTextColor(getResources().getColor(R.color.colorPrimary));
                }

                if(orderStatus.equals("rejected"))
                {
                    // Tous les boutons inactifs pour une commande rejetée
                    serviceOrdersViewHolder.acceptBtn.setText("Accept");
                    serviceOrdersViewHolder.acceptBtn.setEnabled(false);
                    serviceOrdersViewHolder.acceptBtn.setAlpha(.5f);
                    serviceOrdersViewHolder.acceptBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));

                    serviceOrdersViewHolder.rejectBtn.setText("Rejected");
                    serviceOrdersViewHolder.rejectBtn.setEnabled(false);
                    serviceOrdersViewHolder.rejectBtn.setAlpha(.5f);
                    serviceOrdersViewHolder.rejectBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));

                    serviceOrdersViewHolder.finalizeBtn.setText("Finalize the Order");
                    serviceOrdersViewHolder.finalizeBtn.setEnabled(false);
                    serviceOrdersViewHolder.finalizeBtn.setAlpha(.5f);
                    serviceOrdersViewHolder.finalizeBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));
                }

                if(orderStatus.equals("finalized") || orderStatus.equals("finalizedWithRatings") || orderStatus.equals("cancelled"))
                {
                    // Tous les boutons inactifs pour une commande finalisée ou annulée
                    serviceOrdersViewHolder.acceptBtn.setText("Accepted");
                    serviceOrdersViewHolder.acceptBtn.setEnabled(false);
                    serviceOrdersViewHolder.acceptBtn.setAlpha(.5f);
                    serviceOrdersViewHolder.acceptBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));

                    serviceOrdersViewHolder.rejectBtn.setText("Reject");
                    serviceOrdersViewHolder.rejectBtn.setEnabled(false);
                    serviceOrdersViewHolder.rejectBtn.setAlpha(.5f);
                    serviceOrdersViewHolder.rejectBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));

                    serviceOrdersViewHolder.finalizeBtn.setText("Finalized the Order");
                    serviceOrdersViewHolder.finalizeBtn.setEnabled(false);
                    serviceOrdersViewHolder.finalizeBtn.setAlpha(.5f);
                    serviceOrdersViewHolder.finalizeBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));
                }

                // Gestion du clic sur le bouton Accepter
                serviceOrdersViewHolder.acceptBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        // Mise à jour du statut de la commande à "accepted"
                        HashMap orderMap = new HashMap();
                        orderMap.put("orderStatus", "accepted");
                        ordersDatabaseReference.child(orderID).updateChildren(orderMap);
                    }
                });

                // Gestion du clic sur le bouton Rejeter
                serviceOrdersViewHolder.rejectBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        if(orderStatus.equals("pending"))
                        {
                            // Mise à jour du statut de la commande à "rejected"
                            HashMap orderMap = new HashMap();
                            orderMap.put("orderStatus", "rejected");
                            ordersDatabaseReference.child(orderID).updateChildren(orderMap);
                        }
                    }
                });

                // Gestion du clic sur le bouton Finaliser
                serviceOrdersViewHolder.finalizeBtn.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        // Mise à jour du statut de la commande à "finalized"
                        HashMap orderMap = new HashMap();
                        orderMap.put("orderStatus", "finalized");
                        ordersDatabaseReference.child(orderID).updateChildren(orderMap);
                    }
                });

                // Gestion du clic sur l'image de l'utilisateur
                serviceOrdersViewHolder.userImage.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        // Redirection vers le profil de l'utilisateur
                        Intent mainIntent = new Intent(ServiceManagerActivity.this, MainActivity.class);
                        mainIntent.putExtra("intentFrom", "ViewAnotherUserProfile");
                        mainIntent.putExtra("intentUserID", orders.getBuyer());
                        startActivity(mainIntent);
                    }
                });

                // Gestion du clic sur le nom d'utilisateur
                serviceOrdersViewHolder.userName.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        // Redirection vers le profil de l'utilisateur
                        Intent mainIntent = new Intent(ServiceManagerActivity.this, MainActivity.class);
                        mainIntent.putExtra("intentFrom", "ViewAnotherUserProfile");
                        mainIntent.putExtra("intentUserID", orders.getBuyer());
                        startActivity(mainIntent);
                    }
                });

                // Gestion du clic sur le titre du service
                serviceOrdersViewHolder.serviceTitle.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        // Redirection vers la page de détail du service
                        Intent viewServiceIntent = new Intent(ServiceManagerActivity.this, ViewServiceActivity.class);
                        viewServiceIntent.putExtra("intentPurpose", "ViewService");
                        viewServiceIntent.putExtra("intentServiceID", orders.getServiceID());
                        startActivity(viewServiceIntent);
                    }
                });
            }

            @NonNull
            @Override
            public ServiceOrdersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                // Création d'une nouvelle vue pour chaque élément de la liste
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.service_orders_layout,parent,false);
                ServiceOrdersViewHolder serviceOrdersViewHolder = new ServiceOrdersViewHolder(view);
                return serviceOrdersViewHolder;
            }
        };

        // Liaison de l'adapter au RecyclerView
        serviceRequestsList.setAdapter(adapter);
        adapter.startListening(); // Démarre l'écoute des changements dans la base de données
    }

    // ViewHolder pour les éléments de la liste des commandes
    public static class ServiceOrdersViewHolder extends RecyclerView.ViewHolder
    {
        // Déclaration des vues du layout
        TextView serviceTitle, userName, orderDate, orderTime, orderStatus;
        Button acceptBtn, rejectBtn, finalizeBtn;
        CircleImageView userImage;

        public ServiceOrdersViewHolder(@NonNull View itemView)
        {
            super(itemView);

            // Initialisation des vues
            serviceTitle = itemView.findViewById(R.id.service_order_service_title);
            orderDate = itemView.findViewById(R.id.service_order_order_date);
            orderTime = itemView.findViewById(R.id.service_order_order_time);
            orderStatus = itemView.findViewById(R.id.service_order_order_status);
            userImage = (CircleImageView) itemView.findViewById(R.id.service_order_user_image);
            userName = (TextView) itemView.findViewById(R.id.service_order_username);
            acceptBtn = (Button) itemView.findViewById(R.id.service_order_accept_button);
            rejectBtn = (Button) itemView.findViewById(R.id.service_order_reject_button);
            finalizeBtn = (Button) itemView.findViewById(R.id.service_order_finalize_button);
        }
    }

    /* Gestion du bouton retour dans la toolbar */
    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed(); // Appelle la méthode standard de retour
        return true;
    }
}