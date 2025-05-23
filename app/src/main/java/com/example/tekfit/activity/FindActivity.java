package com.example.tekfit.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.tekfit.R;
import com.example.tekfit.model.UserServices;
import com.example.tekfit.model.Users;
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

public class FindActivity extends AppCompatActivity
{
    // Toolbar principal et image utilisateur dans la toolbar (cachée ici)
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    // Authentification Firebase et références aux bases de données Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userDatabaseReference, serviceDatabaseReference;

    // Spinner pour choisir le type de recherche et EditText pour la barre de recherche
    private Spinner searchType;
    private EditText searchBar;

    // RecyclerView pour afficher les résultats de recherche
    private RecyclerView searchResultList;

    // Variables pour stocker l'intention (FindUsers ou FindServices) et le type de recherche sélectionné
    String intentPurpose, strSearchType;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find);

        // Configuration de la toolbar avec titre et bouton retour
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Find");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Récupération et masquage de l'image utilisateur dans la toolbar (non utilisée ici)
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Récupération de l'intention envoyée par l'activité appelante
        Intent intent = getIntent();
        intentPurpose = intent.getExtras().getString("intentPurpose");

        // Initialisation Firebase Auth et références aux nœuds "Users" et "Services"
        firebaseAuth = FirebaseAuth.getInstance();
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users");
        serviceDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Services");

        // Configuration de la barre de recherche et affichage du clavier automatiquement
        searchBar = (EditText) findViewById(R.id.find_search_bar);
        searchBar.requestFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(searchBar, InputMethodManager.SHOW_IMPLICIT);

        // Configuration du spinner pour choisir le type de recherche (People ou Service)
        searchType = findViewById(R.id.find_search_type);
        final ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.searchType, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchType.setAdapter(adapter);

        // Listener sur la sélection du spinner pour adapter la barre de recherche
        searchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                strSearchType = parent.getItemAtPosition(position).toString();

                // Mise à jour de l'indice de recherche et du hint dans la barre de recherche
                if(strSearchType.equals("People"))
                {
                    searchBar.setText("");
                    searchBar.setHint("Search for a People");
                }

                if(strSearchType.equals("Service"))
                {
                    searchBar.setText("");
                    searchBar.setHint("Search for a Service");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent)
            {
                // Rien à faire si rien n'est sélectionné
            }
        });

        // Si l'intention est FindUsers, on sélectionne People par défaut, sinon Service
        if(intentPurpose.equals("FindUsers"))
        {
            strSearchType = "People";
            searchType.setSelection(0);
            searchBar.setHint("Search for a People");
        }
        if(intentPurpose.equals("FindServices"))
        {
            strSearchType = "Service";
            searchType.setSelection(1);
            searchBar.setHint("Search for a Service");
        }

        // Configuration du RecyclerView pour afficher les résultats
        searchResultList = (RecyclerView)findViewById(R.id.search_list);
        searchResultList.setNestedScrollingEnabled(false);
        searchResultList.setHasFixedSize(true);

        // Layout manager pour RecyclerView (liste verticale simple)
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(false);
        linearLayoutManager.setStackFromEnd(false);
        searchResultList.setLayoutManager(linearLayoutManager);

        // Ajout d'un TextWatcher sur la barre de recherche pour lancer la recherche à chaque modification
        searchBar.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                // Pas utilisé ici
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                // Pas utilisé ici
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                // Récupérer la saisie en minuscules
                String searchBarInput = searchBar.getText().toString().toLowerCase();

                if(searchBarInput.isEmpty())
                {
                    // Affiche la liste complète ou vide si la recherche est vide
                    DisplayUserSearchResult(searchBarInput);
                }
                else
                {
                    // Selon le type de recherche, afficher les résultats adaptés
                    if(strSearchType.equals("People"))
                    {
                        DisplayUserSearchResult(searchBarInput);
                    }
                    if(strSearchType.equals("Service"))
                    {
                        DisplayServiceSearchResult(searchBarInput);
                    }
                }
            }
        });
    }

    /**
     * Affiche les résultats de la recherche de services dans le RecyclerView
     * @param searchBarInput texte recherché dans la barre de recherche
     */
    private void DisplayServiceSearchResult(final String searchBarInput)
    {
        // Recherche dans Firebase sur la clé "servicesearchkeyword" avec filtrage par préfixe
        Query searchServiceQuery = serviceDatabaseReference.orderByChild("servicesearchkeyword").startAt(searchBarInput).endAt(searchBarInput + "\uf8ff");

        FirebaseRecyclerOptions options =
                new FirebaseRecyclerOptions.Builder<UserServices>()
                        .setQuery(searchServiceQuery, UserServices.class)
                        .build();

        FirebaseRecyclerAdapter<UserServices, ServiceViewHolder> adapter = new FirebaseRecyclerAdapter<UserServices, ServiceViewHolder>(options)
        {
            @Override
            protected void onBindViewHolder(@NonNull final ServiceViewHolder serviceViewHolder, int i, @NonNull UserServices userServices)
            {
                final String serviceID = getRef(i).getKey();

                if(!searchBarInput.isEmpty())
                {
                    // Remplir les vues avec les données du service
                    serviceViewHolder.serviceTitle.setText("Service : "+userServices.getServicetitle());
                    serviceViewHolder.serviceDate.setText(userServices.servicedate);
                    serviceViewHolder.serviceTime.setText(userServices.servicetime);
                    Picasso.get().load(userServices.getServicefirstimage()).into(serviceViewHolder.serviceFirstImage);
                    serviceViewHolder.serviceSummaryDescription.setText(userServices.servicesummarydescription);

                    // Récupérer les informations du propriétaire du service dans la base "Users"
                    userDatabaseReference.child(userServices.getUserid()).addValueEventListener(new ValueEventListener()
                    {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                        {
                            if(dataSnapshot.exists())
                            {
                                if(dataSnapshot.hasChild("username"))
                                {
                                    String username = dataSnapshot.child("username").getValue().toString();
                                    serviceViewHolder.serviceOwnerUsername.setText(username);
                                }

                                if(dataSnapshot.hasChild("userimage"))
                                {
                                    String userImage = dataSnapshot.child("userimage").getValue().toString();
                                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image_placeholder).into(serviceViewHolder.serviceOwnerImage);
                                }

                                if(dataSnapshot.hasChild("userposition"))
                                {
                                    String position = dataSnapshot.child("userposition").getValue().toString();
                                    serviceViewHolder.serviceOwnerPosition.setText(" • "+position);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError)
                        {
                            // Gérer les erreurs de la requête Firebase ici si besoin
                        }
                    });
                }
                else
                {
                    // Cacher l'item si la recherche est vide
                    serviceViewHolder.itemView.setVisibility(View.GONE);
                }

                // Clic sur un service ouvre l'activité de visualisation du service
                serviceViewHolder.itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent viewServiceIntent = new Intent(FindActivity.this, ViewServiceActivity.class);
                        viewServiceIntent.putExtra("intentPurpose", "ViewService");
                        viewServiceIntent.putExtra("intentServiceID", serviceID);
                        startActivity(viewServiceIntent);
                    }
                });
            }

            @NonNull
            @Override
            public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                // Inflate le layout de chaque item service
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.service_layout, parent, false);
                ServiceViewHolder serviceViewHolder = new ServiceViewHolder(view);
                return serviceViewHolder;
            }
        };

        // Affecter l'adapter au RecyclerView et démarrer l'écoute des données Firebase
        searchResultList.setAdapter(adapter);
        adapter.startListening();
    }

    // ViewHolder pour un élément service dans la liste
    public static class ServiceViewHolder extends RecyclerView.ViewHolder
    {
        TextView serviceTitle, serviceDate, serviceTime, serviceSummaryDescription, serviceOwnerUsername, serviceOwnerPosition;
        ImageView serviceFirstImage;
        CircleImageView serviceOwnerImage;

        public ServiceViewHolder(@NonNull View itemView)
        {
            super(itemView);

            // Liaison des vues du layout service_layout.xml
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


    /**
     * Affiche les résultats de la recherche d'utilisateurs dans le RecyclerView
     * @param searchBarInput texte recherché dans la barre de recherche
     */
    private void DisplayUserSearchResult(final String searchBarInput)
    {
        // Recherche dans Firebase sur la clé "usersearchkeyword" avec filtrage par préfixe
        Query searchUserQuery = userDatabaseReference.orderByChild("usersearchkeyword").startAt(searchBarInput).endAt(searchBarInput + "\uf8ff");

        FirebaseRecyclerOptions options =
                new FirebaseRecyclerOptions.Builder<Users>()
                        .setQuery(searchUserQuery, Users.class)
                        .build();

        FirebaseRecyclerAdapter<Users,UserViewHolder> adapter = new FirebaseRecyclerAdapter<Users, UserViewHolder>(options)
        {
            @Override
            protected void onBindViewHolder(@NonNull UserViewHolder userViewHolder, int i, @NonNull Users users)
            {
                final String userID = getRef(i).getKey();

                if(!searchBarInput.isEmpty())
                {
                    // Affichage du nom d'utilisateur
                    String username = users.getUsername();
                    userViewHolder.userName.setText(username);

                    // Chargement de l'image utilisateur avec Picasso si elle existe
                    String userImage = users.getUserimage();
                    if(!TextUtils.isEmpty(userImage))
                    {
                        Picasso.get().load(userImage).placeholder(R.drawable.default_user_image).into(userViewHolder.userImage);
                    }
                }
                else
                {
                    // Cacher l'item si la recherche est vide
                    userViewHolder.itemView.setVisibility(View.GONE);
                }

                // Clic sur un utilisateur ouvre son profil dans MainActivity
                userViewHolder.itemView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Intent profileIntent = new Intent(FindActivity.this, MainActivity.class);
                        profileIntent.putExtra("intentFrom", "ViewAnotherUserProfile");
                        profileIntent.putExtra("intentUserID", userID);
                        startActivity(profileIntent);
                    }
                });

            }

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                // Inflate le layout d'un utilisateur
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_layout, parent, false);
                UserViewHolder userViewHolder = new UserViewHolder(view);
                return userViewHolder;
            }
        };

        // Affecter l'adapter au RecyclerView et démarrer l'écoute des données Firebase
        searchResultList.setAdapter(adapter);
        adapter.startListening();
    }

    // ViewHolder pour un élément utilisateur dans la liste
    public static class UserViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName;
        CircleImageView userImage;

        public UserViewHolder(@NonNull View itemView)
        {
            super(itemView);

            // Liaison des vues du layout user_layout.xml
            userImage = (CircleImageView) itemView.findViewById(R.id.connection_userimage);
            userName = (TextView) itemView.findViewById(R.id.connection_username);
        }
    }

    /**
     * Gestion du clic sur le bouton retour de la toolbar
     */
    @Override
    public boolean onSupportNavigateUp()
    {
        onBackPressed();
        return true;
    }
}
