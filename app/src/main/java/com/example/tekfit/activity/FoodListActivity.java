package com.example.tekfit.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.example.tekfit.R;
import com.example.tekfit.model.Foods;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import de.hdodenhof.circleimageview.CircleImageView;

public class FoodListActivity extends AppCompatActivity {

    // Déclaration des vues et variables Firebase
    private Toolbar toolbar;
    private CircleImageView toolbarUserImage;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference foodDatabaseReference, userDatabaseReference;

    private EditText searchBar;

    private RecyclerView foodList;

    private FloatingActionButton addfoodbtn;

    // Variable pour stocker l'origine de l'intent (ex : ProfileMenu ou autre)
    String intentFrom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        // Configuration de la toolbar avec titre et bouton retour
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Foods");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Affiche la flèche retour
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // Masquer l'image utilisateur dans la toolbar
        toolbarUserImage = (CircleImageView) findViewById(R.id.toolbar_user_image);
        toolbarUserImage.setVisibility(View.GONE);

        // Initialisation FirebaseAuth et références vers les noeuds Firebase Realtime Database
        firebaseAuth = FirebaseAuth.getInstance();
        foodDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Foods");
        userDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

        // Récupération de l'intent et extraction de l'information "intentFrom"
        Intent intent = getIntent();
        intentFrom = intent.getExtras().getString("intentFrom");

        // Si l'intent ne vient pas de ProfileMenu, modifier le titre de la toolbar
        if(!intentFrom.equals("ProfileMenu")) {
            getSupportActionBar().setTitle("Select " + intentFrom);
        }

        // Initialisation du RecyclerView pour afficher la liste des aliments
        foodList = (RecyclerView)findViewById(R.id.food_list);
        foodList.setHasFixedSize(true); // Optimisation si la taille du RecyclerView ne change pas

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        // Ordre d'affichage (pas d'inversion, affichage normal)
        linearLayoutManager.setReverseLayout(false);
        linearLayoutManager.setStackFromEnd(false);
        foodList.setLayoutManager(linearLayoutManager);

        // Initialisation de la barre de recherche
        searchBar = (EditText)findViewById(R.id.nutrition_search_bar);
        String searchBarInput = searchBar.getText().toString().toLowerCase();
        DisplayAllFoodsNutrition(searchBarInput); // Afficher les aliments selon la recherche initiale (vide ici)

        // Ecoute les modifications de texte dans la barre de recherche
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Pas utilisé ici
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Quand le texte change, mettre à jour la liste des aliments affichés
                String searchBarInput = searchBar.getText().toString().toLowerCase();
                DisplayAllFoodsNutrition(searchBarInput);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Pas utilisé ici
            }
        });

        // Initialisation et gestion du clic sur le bouton flottant pour ajouter un nouvel aliment
        addfoodbtn = (FloatingActionButton)findViewById(R.id.nutrition_add_new_food_button);
        addfoodbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserSendToCreateFoodPage();
            }
        });
    }

    /**
     * Affiche la liste des aliments correspondant au texte recherché dans la barre de recherche
     * @param searchBarInput texte à rechercher dans les aliments
     */
    private void DisplayAllFoodsNutrition(String searchBarInput) {
        // Requête Firebase pour chercher les aliments dont le champ "foodsearchkeyword" commence par searchBarInput
        Query searchFoodQuery = foodDatabaseReference.orderByChild("foodsearchkeyword")
                .startAt(searchBarInput)
                .endAt(searchBarInput + "\uf8ff");

        // Options pour le FirebaseRecyclerAdapter, avec la requête et la classe modèle Foods
        FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<Foods>()
                .setQuery(searchFoodQuery, Foods.class)
                .build();

        // Création de l'adaptateur RecyclerView qui affichera les données Firebase en temps réel
        FirebaseRecyclerAdapter<Foods, FoodNutritionViewHolder> firebaseRecyclerAdapter =
                new FirebaseRecyclerAdapter<Foods, FoodNutritionViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final FoodNutritionViewHolder foodNutritionViewHolder, int i, @NonNull Foods foodNutrition) {
                        // Récupération de l'ID de l'aliment à cette position
                        final String FoodID = getRef(i).getKey();

                        // Affichage du nom de l'aliment si non vide
                        String tempFoodName = foodNutrition.getFoodname();
                        if(!TextUtils.isEmpty(tempFoodName)) {
                            foodNutritionViewHolder.layoutfoodname.setText(tempFoodName);
                        }

                        // Affichage des calories si non vide
                        String tempCalories = foodNutrition.getFoodcalories();
                        if(!TextUtils.isEmpty(tempCalories)) {
                            foodNutritionViewHolder.layoutcalories.setText(tempCalories + "cal");
                        }

                        // Clic sur un item : envoie vers l'activité d'ajout/modification d'aliment avec l'ID de l'aliment sélectionné
                        foodNutritionViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                SendUserToAddFoodPage(FoodID);
                            }
                        });
                    }

                    @NonNull
                    @Override
                    public FoodNutritionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        // Inflater le layout XML d'un item alimentaire
                        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.food_layout, parent, false);
                        FoodNutritionViewHolder viewHolder = new FoodNutritionViewHolder(view);
                        return viewHolder;
                    }
                };

        // Appliquer l'adaptateur au RecyclerView
        foodList.setAdapter(firebaseRecyclerAdapter);
        // Démarrer l'écoute des données Firebase
        firebaseRecyclerAdapter.startListening();
    }

    /**
     * Envoie l'utilisateur vers la page d'ajout/modification d'un aliment, en passant les informations d'origine et l'ID de l'aliment
     * @param foodID ID de l'aliment sélectionné
     */
    private void SendUserToAddFoodPage(String foodID) {
        Intent addFoodIntent = new Intent(FoodListActivity.this, AddFoodActivity.class);
        addFoodIntent.putExtra("intentFrom", intentFrom);
        addFoodIntent.putExtra("intentFoodID", foodID);
        startActivity(addFoodIntent);
    }

    /**
     * ViewHolder pour les éléments du RecyclerView affichant le nom et les calories d'un aliment
     */
    public static class FoodNutritionViewHolder extends RecyclerView.ViewHolder {
        TextView layoutfoodname, layoutcalories;

        public FoodNutritionViewHolder(@NonNull View itemView) {
            super(itemView);

            layoutfoodname = (TextView)itemView.findViewById(R.id.food_name);
            layoutcalories = (TextView)itemView.findViewById(R.id.food_calories);
        }
    }

    /**
     * Envoie l'utilisateur vers la page de création d'un nouvel aliment
     */
    private void UserSendToCreateFoodPage() {
        Intent createFoodIntent = new Intent(FoodListActivity.this, CreateFoodActivity.class);
        createFoodIntent.putExtra("intentFrom", intentFrom);
        startActivity(createFoodIntent);
    }

    /**
     * Gestion du clic sur la flèche retour de la toolbar : revient à l'activité précédente
     */
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
