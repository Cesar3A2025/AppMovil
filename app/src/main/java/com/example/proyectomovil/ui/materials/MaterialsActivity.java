package com.example.proyectomovil.ui.materials;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.proyectomovil.R;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.data.repository.MaterialsRepository;
import com.example.proyectomovil.domain.models.Materials;
import com.example.proyectomovil.utils.Result;

import java.util.ArrayList;
import java.util.List;

public class MaterialsActivity extends AppCompatActivity {

    // Mantengo los nombres originales
    private static final String BASE_API = ApiRoutes.MATERIALS;
    private static final String BASE_IMAGES = ApiRoutes.BASE.endsWith("/api")
            ? ApiRoutes.BASE.substring(0, ApiRoutes.BASE.length() - 4) + "/"
            : ApiRoutes.BASE + "/";

    private RecyclerView rv;
    private SwipeRefreshLayout swipe;
    private SearchView searchView;
    private Spinner spnClasif, spnAptitud, spnCategoria;
    private final List<Materials> fullList = new ArrayList<>();
    private final List<Materials> filteredList = new ArrayList<>();

    private MaterialsAdapter adapter;

    private final MaterialsRepository repo = new MaterialsRepository();

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
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // al cambiar filtros -> pedir al servidor
                swipe.setRefreshing(true);
                fetchMaterials();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        spnClasif.setOnItemSelectedListener(listener);
        spnAptitud.setOnItemSelectedListener(listener);
        spnCategoria.setOnItemSelectedListener(listener);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) {
                swipe.setRefreshing(true);
                fetchMaterials(); // buscar en servidor
                return true;
            }
            @Override public boolean onQueryTextChange(String newText) {
                // Si prefieres búsqueda “en vivo” al teclear:
                // swipe.setRefreshing(true);
                // fetchMaterials();
                // Por ahora, filtramos en cliente mientras se escribe:
                applyFiltersAndShow();
                return true;
            }
        });
    }

    // Llama al repositorio enviando los filtros como query params (según tu MaterialController@index)
    private void fetchMaterials() {
        String query = searchView.getQuery() != null ? searchView.getQuery().toString().trim() : "";
        String clasif = (String) spnClasif.getSelectedItem();
        String apt = (String) spnAptitud.getSelectedItem();
        String cat = (String) spnCategoria.getSelectedItem();

        // Normalizamos “todos/todas” -> null para no enviar ese filtro
        String qClasif = (clasif != null && !"todos".equalsIgnoreCase(clasif)) ? clasif : null;
        String qApt    = (apt != null && !"todas".equalsIgnoreCase(apt)) ? apt : null;
        String qCat    = (cat != null && !"todas".equalsIgnoreCase(cat)) ? cat : null;

        // Puedes ajustar perPage/sort/dir si quieres (aquí traemos bastante para evitar paginación en cliente)
        int perPage = 200;
        String sort = "created_at";
        String dir  = "desc";

        repo.getAllFiltered(query, qClasif, qApt, qCat, perPage, sort, dir, result -> runOnUiThread(() -> {
            swipe.setRefreshing(false);
            if (!result.isOk()) {
                Toast.makeText(MaterialsActivity.this, result.error, Toast.LENGTH_SHORT).show();
                return;
            }
            List<Materials> list = result.data != null ? result.data : new ArrayList<>();
            fullList.clear();
            fullList.addAll(list);
            applyFiltersAndShow(); // aún aplicamos filtro local por si el usuario va escribiendo
        }));
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
