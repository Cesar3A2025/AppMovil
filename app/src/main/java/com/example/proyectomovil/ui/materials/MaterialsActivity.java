package com.example.proyectomovil.ui.materials;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.proyectomovil.R;
import com.example.proyectomovil.data.api.ApiRoutes;
import com.example.proyectomovil.data.repository.MaterialsRepository;
import com.example.proyectomovil.domain.models.Materials;

import java.util.ArrayList;
import java.util.List;
import com.example.proyectomovil.ui.base.BaseDrawerActivity;
import android.graphics.Color;

import android.graphics.PorterDuff;
import android.widget.EditText;
import android.widget.ImageView;

public class MaterialsActivity extends BaseDrawerActivity {

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
        // setContentView(R.layout.activity_materials);
        // Muestra activity_materials.xml DENTRO del drawer
        setContentWithDrawer(R.layout.activity_materials);
        setTitle("Materiales");
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
        // ✅ CONFIGURAR SEARCHVIEW INMEDIATAMENTE DESPUÉS DE FINDVIEWBYID
        setupSearchViewWhiteStyle();

        // RecyclerView + Adapter
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MaterialsAdapter(this, filteredList);
        rv.setAdapter(adapter);

        // Inicializar spinners
        setupSpinners();

        // Búsqueda
        setupSearch();

        // Pull-to-refresh
        swipe.setOnRefreshListener(this::fetchMaterials);

        // Primera carga
        swipe.setRefreshing(true);
        fetchMaterials();
    }

    private void setupSearchViewWhiteStyle() {
        try {
            // Método más agresivo - buscar por todos los elementos posibles
            searchView.setIconifiedByDefault(false);

            // Buscar el EditText por todos los IDs posibles
            EditText searchEditText = null;
            int[] possibleIds = {
                    androidx.appcompat.R.id.search_src_text,  // ID de AndroidX
                    getResources().getIdentifier("android:id/search_src_text", null, null)
            };

            for (int id : possibleIds) {
                View view = searchView.findViewById(id);
                if (view instanceof EditText) {
                    searchEditText = (EditText) view;
                    break;
                }
            }

            if (searchEditText != null) {
                searchEditText.setTextColor(Color.WHITE);
                searchEditText.setHintTextColor(Color.parseColor("#B3FFFFFF"));
                searchEditText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));
            }

            // 🔹 ICONO DE BÚSQUEDA (Lupa) - Múltiples formas
            int searchIconId = androidx.appcompat.R.id.search_mag_icon;
            ImageView searchIcon = searchView.findViewById(searchIconId);
            if (searchIcon != null) {
                searchIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            } else {
                // Intentar con el ID del sistema
                int systemSearchIconId = getResources().getIdentifier("android:id/search_mag_icon", null, null);
                ImageView systemSearchIcon = searchView.findViewById(systemSearchIconId);
                if (systemSearchIcon != null) {
                    systemSearchIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                }
            }

            // 🔹 ICONO DE CERRAR (X) - Múltiples formas
            int closeIconId = androidx.appcompat.R.id.search_close_btn;
            ImageView closeIcon = searchView.findViewById(closeIconId);
            if (closeIcon != null) {
                closeIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            } else {
                // Intentar con el ID del sistema
                int systemCloseIconId = getResources().getIdentifier("android:id/search_close_btn", null, null);
                ImageView systemCloseIcon = searchView.findViewById(systemCloseIconId);
                if (systemCloseIcon != null) {
                    systemCloseIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                }
            }

            // 🔹 ICONO DE VOZ (Opcional)
            int voiceIconId = androidx.appcompat.R.id.search_voice_btn;
            ImageView voiceIcon = searchView.findViewById(voiceIconId);
            if (voiceIcon != null) {
                voiceIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            } else {
                int systemVoiceIconId = getResources().getIdentifier("android:id/search_voice_btn", null, null);
                ImageView systemVoiceIcon = searchView.findViewById(systemVoiceIconId);
                if (systemVoiceIcon != null) {
                    systemVoiceIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                }
            }

            // 🔹 BUSCAR TODOS LOS ImageView Y APLICAR BLANCO
            List<ImageView> allIcons = findAllImageViews(searchView);
            for (ImageView icon : allIcons) {
                icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            }

            // Configurar iconos usando tint
            searchView.setIconifiedByDefault(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔹 MÉTODO AUXILIAR PARA ENCONTRAR TODOS LOS ImageView
    private List<ImageView> findAllImageViews(View view) {
        List<ImageView> imageViews = new ArrayList<>();
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof ImageView) {
                    imageViews.add((ImageView) child);
                } else if (child instanceof ViewGroup) {
                    imageViews.addAll(findAllImageViews(child));
                }
            }
        }
        return imageViews;
    }

    private void setupSpinners() {
        String[] clasificaciones = new String[]{"todos", "verde", "marron", "no_compostable"};
        String[] aptitudes = new String[]{"todas", "casero", "industrial", "no_recomendado"};
        String[] categorias = new String[]{"todas", "alimentos", "jardin", "papel_carton", "otros"};

        // Crear adapter personalizado con texto blanco
        ArrayAdapter<String> whiteAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                clasificaciones
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.WHITE);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.WHITE);
                    textView.setBackgroundColor(Color.parseColor("#FF424242")); // Fondo gris oscuro
                }
                return view;
            }
        };

        whiteAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Aplicar el adapter a todos los spinners
        spnClasif.setAdapter(whiteAdapter);

        // Crear nuevos adapters para los otros spinners (pueden reutilizar la misma lógica)
        ArrayAdapter<String> aptitudAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                aptitudes
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.WHITE);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.WHITE);
                    textView.setBackgroundColor(Color.parseColor("#FF424242"));
                }
                return view;
            }
        };
        aptitudAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnAptitud.setAdapter(aptitudAdapter);

        ArrayAdapter<String> categoriaAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                categorias
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.WHITE);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    TextView textView = (TextView) view;
                    textView.setTextColor(Color.WHITE);
                    textView.setBackgroundColor(Color.parseColor("#FF424242"));
                }
                return view;
            }
        };
        categoriaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnCategoria.setAdapter(categoriaAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Asegurar que el texto seleccionado sea blanco
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.WHITE);
                }
                swipe.setRefreshing(true);
                fetchMaterials();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };

        spnClasif.setOnItemSelectedListener(listener);
        spnAptitud.setOnItemSelectedListener(listener);
        spnCategoria.setOnItemSelectedListener(listener);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                swipe.setRefreshing(true);
                fetchMaterials();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFiltersAndShow(); // filtrado local
                return true;
            }
        });
    }
    private void fetchMaterials() {
        String query = searchView.getQuery() != null ? searchView.getQuery().toString().trim() : "";
        String clasif = (String) spnClasif.getSelectedItem();
        String apt = (String) spnAptitud.getSelectedItem();
        String cat = (String) spnCategoria.getSelectedItem();

        // Normalizar "todos/todas"
        String qClasif = (clasif != null && !"todos".equalsIgnoreCase(clasif)) ? clasif : null;
        String qApt    = (apt != null && !"todas".equalsIgnoreCase(apt)) ? apt : null;
        String qCat    = (cat != null && !"todas".equalsIgnoreCase(cat)) ? cat : null;

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
            applyFiltersAndShow();
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
