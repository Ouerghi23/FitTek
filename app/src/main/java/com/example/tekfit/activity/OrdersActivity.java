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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tekfit.R;
import com.example.tekfit.model.Orders;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class OrdersActivity extends AppCompatActivity {

    // Déclaration des vues et des références Firebase
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference servicesDatabaseReference, ordersDatabaseReference, userDatabaseReference, serviceRatingDatabaseReference;

    private RecyclerView orderList;

    String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orders);

        // Configuration de la toolbar
        toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Orders");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Image utilisateur masquée dans la toolbar
        toolbarUserImage = findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Initialisation de Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        currentUserID = firebaseAuth.getCurrentUser().getUid();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        ordersDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Orders");
        servicesDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Services");
        serviceRatingDatabaseReference = FirebaseDatabase.getInstance().getReference().child("ServiceRatings");

        // Initialisation de la RecyclerView
        orderList = findViewById(R.id.order_list);
        orderList.setNestedScrollingEnabled(false);
        orderList.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true); // Derniers éléments en haut
        linearLayoutManager.setStackFromEnd(true);
        orderList.setLayoutManager(linearLayoutManager);

        // Chargement des commandes
        LoadAllOrders();
    }

    // Charge les commandes de l'utilisateur courant
    private void LoadAllOrders() {
        // Filtrer les commandes de l'utilisateur actuel
        Query query = ordersDatabaseReference.orderByChild("buyer").equalTo(currentUserID);

        FirebaseRecyclerOptions<Orders> options = new FirebaseRecyclerOptions.Builder<Orders>()
                .setQuery(query, Orders.class)
                .build();

        FirebaseRecyclerAdapter<Orders, OrdersViewHolder> adapter = new FirebaseRecyclerAdapter<Orders, OrdersViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final OrdersViewHolder ordersViewHolder, int i, @NonNull Orders orders) {
                final String orderID = getRef(i).getKey();
                final String serviceID = orders.getServiceID();
                final String orderStatus = orders.getOrderStatus();

                // Affichage selon le statut de la commande
                if (orderStatus.equals("pending") || orderStatus.equals("accepted") ||
                        orderStatus.equals("rejected") || orderStatus.equals("finalized") ||
                        orderStatus.equals("finalizedWithRatings") || orderStatus.equals("cancelled")) {

                    ordersViewHolder.ServiceRatingContainer.setVisibility(View.GONE);

                    switch (orderStatus) {
                        case "pending":
                            ordersViewHolder.orderStatus.setText("Order Status : Pending");
                            break;
                        case "accepted":
                            ordersViewHolder.orderStatus.setText("Order Status : Accepted by Seller");
                            break;
                        case "rejected":
                            ordersViewHolder.orderStatus.setText("Order Status : Rejected by Seller");
                            break;
                        case "finalized":
                            ordersViewHolder.orderStatus.setText("Order Status :  Finalized");
                            ordersViewHolder.ServiceRatingContainer.setVisibility(View.VISIBLE);
                            break;
                        case "finalizedWithRatings":
                            ordersViewHolder.orderStatus.setText("Order Status :  Finalized and, Rated by You");
                            break;
                        case "cancelled":
                            ordersViewHolder.orderStatus.setText("Order Status : Cancelled by You");
                            break;
                    }

                    // Affichage date et heure
                    ordersViewHolder.orderDate.setText("Order Time : " + orders.getDate());
                    ordersViewHolder.orderTime.setText(" • " + orders.getTime());

                    // Chargement des détails du service
                    if (!serviceID.isEmpty()) {
                        servicesDatabaseReference.child(serviceID).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    if (dataSnapshot.hasChild("servicetitle")) {
                                        ordersViewHolder.serviceTitle.setText("Service : " + dataSnapshot.child("servicetitle").getValue(String.class));
                                    }

                                    if (dataSnapshot.hasChild("servicefirstimage")) {
                                        Picasso.get()
                                                .load(dataSnapshot.child("servicefirstimage").getValue(String.class))
                                                .placeholder(R.drawable.default_image)
                                                .into(ordersViewHolder.serviceImage);
                                    }

                                    if (dataSnapshot.hasChild("servicesummarydescription")) {
                                        ordersViewHolder.serviceSummary.setText(dataSnapshot.child("servicesummarydescription").getValue(String.class));
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Gestion des erreurs
                            }
                        });
                    }
                } else {
                    // Masquer les commandes supprimées ou inconnues
                    ordersViewHolder.itemView.setVisibility(View.GONE);
                    ordersViewHolder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                }

                // Configuration des boutons selon le statut
                // (logique répétitive refactorable)
                switch (orderStatus) {
                    case "pending":
                        configureButtons(ordersViewHolder, true, true, "Cancel Order", R.color.WarningTextColor);
                        break;
                    case "accepted":
                    case "rejected":
                    case "finalized":
                    case "finalizedWithRatings":
                        configureButtons(ordersViewHolder, true, false, "Cancel Order", R.color.PrimaryTextColor);
                        break;
                    case "cancelled":
                        configureButtons(ordersViewHolder, true, false, "Cancelled", R.color.PrimaryTextColor);
                        break;
                }

                // Bouton "View Service"
                ordersViewHolder.viewServiceBtn.setOnClickListener(v -> {
                    Intent viewServiceIntent = new Intent(OrdersActivity.this, ViewServiceActivity.class);
                    viewServiceIntent.putExtra("intentPurpose", "OrdersActivity");
                    viewServiceIntent.putExtra("intentServiceID", serviceID);
                    startActivity(viewServiceIntent);
                });

                // Bouton "Cancel Order"
                ordersViewHolder.cancelOrderBtn.setOnClickListener(v -> {
                    if (orderStatus.equals("pending")) {
                        HashMap<String, Object> orderMap = new HashMap<>();
                        orderMap.put("orderStatus", "cancelled");
                        ordersDatabaseReference.child(orderID).updateChildren(orderMap);
                    }
                });

                // Bouton "Submit Rating"
                ordersViewHolder.ServiceRatingSubmitBtn.setOnClickListener(v -> {
                    float ratingValue = ordersViewHolder.ServiceBuyerRating.getRating();

                    HashMap<String, Object> ratingMap = new HashMap<>();
                    ratingMap.put("ratingValue", String.valueOf(ratingValue));
                    ratingMap.put("serviceID", serviceID);
                    serviceRatingDatabaseReference.child(orderID).updateChildren(ratingMap);

                    HashMap<String, Object> orderMap = new HashMap<>();
                    orderMap.put("orderStatus", "finalizedWithRatings");
                    ordersDatabaseReference.child(orderID).updateChildren(orderMap);

                    Toast.makeText(OrdersActivity.this, "Rating submitted", Toast.LENGTH_SHORT).show();
                });
            }

            // Crée la vue de chaque commande (ViewHolder)
            @NonNull
            @Override
            public OrdersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.order_layout, parent, false);
                return new OrdersViewHolder(view);
            }
        };

        orderList.setAdapter(adapter);
        adapter.startListening();
    }

    // Méthode utilitaire pour configurer l’état des boutons
    private void configureButtons(OrdersViewHolder holder, boolean viewEnabled, boolean cancelEnabled, String cancelText, int cancelColor) {
        holder.viewServiceBtn.setEnabled(viewEnabled);
        holder.viewServiceBtn.setAlpha(viewEnabled ? 1f : 0.5f);
        holder.viewServiceBtn.setText("View Service");
        holder.viewServiceBtn.setTextColor(getResources().getColor(R.color.PrimaryTextColor));

        holder.cancelOrderBtn.setText(cancelText);
        holder.cancelOrderBtn.setEnabled(cancelEnabled);
        holder.cancelOrderBtn.setAlpha(cancelEnabled ? 1f : 0.5f);
        holder.cancelOrderBtn.setTextColor(getResources().getColor(cancelColor));
    }

    // ViewHolder contenant toutes les vues de l’élément de commande
    public static class OrdersViewHolder extends RecyclerView.ViewHolder {
        TextView serviceTitle, serviceSummary, orderDate, orderTime, orderStatus;
        Button viewServiceBtn, cancelOrderBtn, ServiceRatingSubmitBtn;
        ImageView serviceImage;
        LinearLayout ServiceRatingContainer;
        RatingBar ServiceBuyerRating;

        public OrdersViewHolder(@NonNull View itemView) {
            super(itemView);

            serviceTitle = itemView.findViewById(R.id.order_service_title);
            serviceImage = itemView.findViewById(R.id.order_service_first_image);
            serviceSummary = itemView.findViewById(R.id.order_service_summary_description);
            orderDate = itemView.findViewById(R.id.order_order_date);
            orderTime = itemView.findViewById(R.id.order_order_time);
            orderStatus = itemView.findViewById(R.id.order_order_status);
            viewServiceBtn = itemView.findViewById(R.id.order_service_view_button);
            cancelOrderBtn = itemView.findViewById(R.id.order_service_cancel_button);
            ServiceRatingContainer = itemView.findViewById(R.id.order_service_rating_container);
            ServiceBuyerRating = itemView.findViewById(R.id.order_service__buyer_rating_bar);
            ServiceRatingSubmitBtn = itemView.findViewById(R.id.order_service__rating_submit_button);
        }
    }

    // Action bouton retour de la toolbar
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
