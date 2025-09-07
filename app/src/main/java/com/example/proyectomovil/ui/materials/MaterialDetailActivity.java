package com.example.proyectomovil.ui.materials;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.proyectomovil.R;

public class MaterialDetailActivity extends AppCompatActivity {

    TextView txtId, txtName, txtClasif, txtAptitud, txtAdvice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_material_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int id = getIntent().getIntExtra("id", -1);
        String name = getIntent().getStringExtra("name");
        String clasification = getIntent().getStringExtra("clasification");
        String aptitude = getIntent().getStringExtra("aptitude");


        TextView txtName = findViewById(R.id.txtName);
        TextView txtClasif = findViewById(R.id.txtClasif);
        TextView txtAptitud = findViewById(R.id.txtAptitud);
        TextView txtAdvice = findViewById(R.id.txtAdvice);

        txtName.setText(name);
        txtClasif.setText( clasification);
        txtAptitud.setText(aptitude);

        switch (id) {
            case 1:
                txtAdvice.setText("Residuos frescos como cáscaras de plátano, manzana, naranja, etc.\n" +
                        "Ventajas: Aportan nutrientes y humedad.\n" +
                        "Desventajas: En exceso generan malos olores.");
                break;
            case 2:
                txtAdvice.setText("Hojas, cáscaras, y tallos crudos de vegetales.\n" +
                        "Ventajas: Ricos en nitrógeno y se descomponen rápido.\n" +
                        "Desventajas: Pueden atraer moscas si no se cubren bien..");
                break;

            case 3:
                txtAdvice.setText("Hierba fresca rica en nitrógeno. Se debe mezclar bien para evitar malos olores.\n" +
                        "Ventajas: Gran fuente de nitrógeno.\n" +
                        "Desventajas: Se compacta y genera olor si se acumula.");
                break;
            case 4:
                txtAdvice.setText("Residuos de café usados. Aporta nitrógeno y mejora la estructura del compost.\n" +
                        "Ventajas: Enriquece y mejora la textura.\n" +
                        "Desventajas: En exceso puede acidificar.");
                break;
            case 5:
                txtAdvice.setText("Bolsas de té naturales (sin grapas) y hojas sueltas.\n" +
                        "Ventajas: Aporta nitrógeno y humedad.\n" +
                        "Desventajas: No usar si la bolsa tiene plástico.");
                break;
            case 6:
                txtAdvice.setText("Hojas caídas secas, ideales para aportar carbono.\n" +
                        "Ventajas: Equilibran humedad y aportan carbono.\n" +
                        "Desventajas: En exceso ralentizan el compostaje.");
                break;
            case 7:
                txtAdvice.setText("Cartón sin plastificar ni tinta a color. Se debe cortar o rasgar.\n" +
                        "Ventajas: Buen aporte de carbono y absorbe líquidos.\n" +
                        "Desventajas: Con tinta o plástico contamina.");
                break;
            case 8:
                txtAdvice.setText("Papel sin tinta o productos químicos. No debe estar muy aceitoso.\n" +
                        "Ventajas: Absorbe humedad y aporta carbono.\n" +
                        "Desventajas: No usar si está muy grasoso.");
                break;
            case 9:
                txtAdvice.setText("Polvo de madera sin tratar, excelente para absorber humedad.\n" +
                        "Ventajas: Absorbe líquidos y da estructura.\n" +
                        "Desventajas: La madera tratada es tóxica.");
                break;
            case 10:
                txtAdvice.setText("Ramas secas trituradas o cortadas para facilitar su descomposición.\n" +
                        "Ventajas: Mejoran la ventilación del compost.\n" +
                        "Desventajas: Se descomponen lentamente.");
                break;
            case 11:
                txtAdvice.setText("Dificultan el compostaje por su acidez. Usar en pequeñas cantidades.\n" +
                        "Ventajas: Aportan algunos nutrientes.\n" +
                        "Desventajas: En exceso acidifican el compost.");
                break;
            case 12:
                txtAdvice.setText("No se degrada fácilmente y puede contener químicos.\n" +
                        "Ventajas: Ninguna en compostaje.\n" +
                        "Desventajas: No se degrada y libera tóxicos.");
                break;
            case 13:
                txtAdvice.setText("Atraen animales y generan malos olores.\n" +
                        "Ventajas: Ninguna en compostaje casero.\n" +
                        "Desventajas: Mal olor y plagas.");
                break;
            case 14:
                txtAdvice.setText("Contiene patógenos que requieren compostaje industrial.\n" +
                        "Ventajas: Solo útil en procesos industriales.\n" +
                        "Desventajas: Contamina y es peligroso en casa.");
                break;

            case 15:
                txtAdvice.setText("Contiene patógenos que requieren compostaje industrial.\n" +
                        "Ventajas: Solo útil en procesos industriales.\n" +
                        "Desventajas: Contamina y es peligroso en casa.");
                break;
            default:
                txtAdvice.setText("Sin recomendaciones específicas para este material.");
                break;
        }
    }
}