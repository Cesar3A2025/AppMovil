package com.example.proyectomovil.ui.materials;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.proyectomovil.domain.models.Materials;
import com.example.proyectomovil.R;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MaterialsActivity extends AppCompatActivity {

    private static final String BASE_API = "http://10.0.2.2/composta_esp33/public/api/materials";
    private static final String BASE_IMAGES = "http://10.0.2.2/composta_esp33/public/";
    private RecyclerView rv;
    private SwipeRefreshLayout swipe;
    private SearchView searchView;
    private Spinner spnClasif, spnAptitud, spnCategoria;
    private final List<Materials> fullList = new ArrayList<>();
    private final List<Materials> filteredList = new ArrayList<>();

    private MaterialsAdapter adapter;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_materials);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Referencias UI
        rv = findViewById(R.id.rvMaterials);
        swipe = findViewById(R.id.swipeRefresh);
        searchView = findViewById(R.id.searchView);
        spnClasif = findViewById(R.id.spnClasificacion);
        spnAptitud = findViewById(R.id.spnAptitud);
        spnCategoria = findViewById(R.id.spnCategoria);

        // Recycler + Adapter
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MaterialsAdapter(this, filteredList, BASE_IMAGES);
        rv.setAdapter(adapter);
        // Spinners (valores por defecto usados de BD)
        setupSpinners();
        // Search
        setupSearch();

        // Pull-to-refresh
        swipe.setOnRefreshListener(this::fetchMaterials);

        // Cargar datos
        swipe.setRefreshing(true);
        fetchMaterials();

    }

    private void setupSpinners() {
        String[] clasificaciones = new String[]{"todos", "verde", "marron", "no_compostable"};
        String[] aptitudes = new String[]{"todas", "casero", "industrial", "no_recomendado"};
        String[] categorias = new String[]{"todas", "alimentos", "jardin", "papel_carton", "otros"};

        spnClasif.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, clasificaciones));
        spnAptitud.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, aptitudes));
        spnCategoria.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categorias));

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { applyFiltersAndShow(); }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        spnClasif.setOnItemSelectedListener(listener);
        spnAptitud.setOnItemSelectedListener(listener);
        spnCategoria.setOnItemSelectedListener(listener);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { applyFiltersAndShow(); return true; }
            @Override public boolean onQueryTextChange(String newText) { applyFiltersAndShow(); return true; }
        });
    }

    private void fetchMaterials() {
        Request req = new Request.Builder().url(BASE_API).get().build();
        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    swipe.setRefreshing(false);
                    Toast.makeText(MaterialsActivity.this, "Error de red", Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        swipe.setRefreshing(false);
                        Toast.makeText(MaterialsActivity.this, "Respuesta no válida", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                try {
                    List<Materials> list = Materials.parseList(body);
                    runOnUiThread(() -> {
                        swipe.setRefreshing(false);
                        fullList.clear();
                        fullList.addAll(list);
                        applyFiltersAndShow();
                    });
                } catch (JSONException e) {
                    runOnUiThread(() -> {
                        swipe.setRefreshing(false);
                        Toast.makeText(MaterialsActivity.this, "Error al parsear datos", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void applyFiltersAndShow() {
        String query = searchView.getQuery() != null ? searchView.getQuery().toString().trim() : "";
        String clasif = (String) spnClasif.getSelectedItem();
        String apt = (String) spnAptitud.getSelectedItem();
        String cat = (String) spnCategoria.getSelectedItem();

        filteredList.clear();
        for (Materials m : fullList) {
            boolean qOk = query.isEmpty() || (m.getName() != null && m.getName().toLowerCase().contains(query.toLowerCase()));
            boolean cOk = (clasif == null || "todos".equalsIgnoreCase(clasif)) || clasif.equalsIgnoreCase(m.getClasification());
            boolean aOk = (apt == null || "todas".equalsIgnoreCase(apt)) || apt.equalsIgnoreCase(m.getAptitude());
            boolean tOk = (cat == null || "todas".equalsIgnoreCase(cat)) || cat.equalsIgnoreCase(m.getTypeCategory());
            if (qOk && cOk && aOk && tOk) {
                filteredList.add(m);
            }
        }
        adapter.updateData(new ArrayList<>(filteredList));
    }
}