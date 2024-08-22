package com.guilherme.projetoautavancada;

import static java.lang.String.format;
import static java.lang.String.valueOf;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.guilherme.mylibrary.CriptografarDados;
import com.guilherme.mylibrary.Region;
import com.guilherme.mylibrary.RestrictedRegion;
import com.guilherme.mylibrary.SubRegion;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AtividadePrincipal extends AppCompatActivity implements OnMapReadyCallback {

    // Constantes
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    // Componentes da Interface do Usuário
    private TextView latitudeTextView;
    private TextView longitudeTextView;
    private TextView userNameTextView;
    private TextView userIdTextView;
    private TextView labelsizeFila;
    private TextView notification;
    private TextView T1;
    private TextView T2;
    private TextView T3;
    private TextView T4;
    private TextView T5;
    private View X;
    private Button addRegionButton;
    private Button GravarBdButtom;
    private Button botaoCamada;
    private Button botaoLimparFila;
    private Button button_BD;

    // Componentes do Mapa
    private GoogleMap mMap;
    private Marker currentMarker;

    // Dados do Usuário e Localização
    private double latitudeAtual = 0.0;
    private double longitudeAtual = 0.0;
    private int userId;
    private String userName;

    // Gerenciamento de Regiões
    private final Queue<Region> filaDeRegions = new LinkedList<>();
    private int contRegion = 0;
    private int contSubRegion = 0;
    private int contRestRegion = 0;
    private boolean ultimaAddSub = false;
    private Region auxRegion;

    // Medição de Desempenho
    private double timeUltimaLerDados;
    private long tempo_inicio_atividade;
    public double Time_T1;
    private double Time_T2;
    public double Time_T3;
    public double Time_T4;
    public double Time_T5;

    // Concorrência e Armazenamento
    private final Semaphore semaphore = new Semaphore(1);
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // SharedPreferences para armazenamento de configurações ou dados do usuário
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userNameTextView = findViewById(R.id.userNameTextView);
        userIdTextView = findViewById(R.id.userIdTextView);
        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);
        labelsizeFila = findViewById(R.id.labelsizeFila);
        notification = findViewById(R.id.notification);
        addRegionButton = findViewById(R.id.addRegionButton);
        GravarBdButtom = findViewById(R.id.GravarBdButtom);
        botaoCamada = findViewById(R.id.botaoCamada);
        button_BD = findViewById(R.id.BD);
        botaoLimparFila = findViewById(R.id.botaoLimparFila);
        T1 = findViewById(R.id.T1);
        T2 = findViewById(R.id.T2);
        T3 = findViewById(R.id.T3);
        T4 = findViewById(R.id.T4);
        T5 = findViewById(R.id.T5);
        X = findViewById(R.id.X);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // Verifique se é a primeira execução
        boolean isFirstRun = sharedPreferences.getBoolean("isFirstRun", true);
        if (isFirstRun) {
            // Se for a primeira execução, atualize o valor de isFirstRun para false
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isFirstRun", false);
            editor.apply();

            // Inicie a RegisterActivity
            Intent iniciaRegister = new Intent(this, RegisterActivity.class);
            startActivity(iniciaRegister);
            finish();
            return;
        }

        // Tenta recuperar os dados do usuário da Intent ou das SharedPreferences
        Intent intent = getIntent();
        userName = intent.getStringExtra("NomeUsuario");
        userId = intent.getIntExtra("IdUsuario", -1); // -1 como valor padrão

        // Se os dados não forem encontrados na Intent, tenta recuperar das SharedPreferences
        if (userName == null || userName.isEmpty() || userId == -1) {
            userName = sharedPreferences.getString("NomeUsuario", "Nome de Usuário indisponível");
            userId = sharedPreferences.getInt("IdUsuario", -1); // -1 indica que o ID não está disponível
        }

        userNameTextView.setText(userName);
        if (userId != -1) {
            userIdTextView.setText(valueOf(userId));
        } else {
            userIdTextView.setText("ID não disponível");
        }

        addRegionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimarBotton.aplicarPulsacao(v);
                //System.out.println("\nComputação LerDados: "+ timeUltimaLerDados + " segundos.");
                Time_T1 = timeUltimaLerDados;

                tempo_inicio_atividade = System.nanoTime();
                AtomicInteger contadorDeRespostas = new AtomicInteger(0);
                AtomicBoolean RegProx = new AtomicBoolean(false);
                AtomicBoolean SubRegProx = new AtomicBoolean(false);
                AtomicBoolean RestRegProx = new AtomicBoolean(false);

                CallbackConsulta callback = (existeRegProx, existeSubRegProx, existeRestRegProx, regionMain, time_2, time_3) -> {
                    if(time_2 != 0.0) Time_T2 = time_2;
                    if(time_3 != 0.0) Time_T3 = time_3;
                    if (existeRegProx) {
                        RegProx.set(true);
                        auxRegion = regionMain;
                    }
                    if (existeSubRegProx) SubRegProx.set(true);
                    if (existeRestRegProx) RestRegProx.set(true);

                    if (contadorDeRespostas.incrementAndGet() == 2) {
                        partiuAddRegion(RegProx, SubRegProx, RestRegProx);
                    }
                };

                Thread consultaNaFila = new ConsultaFila(filaDeRegions, latitudeAtual, longitudeAtual, semaphore, callback, auxRegion, tempo_inicio_atividade);
                Thread consultarBd = new ConsultaBD(db, latitudeAtual, longitudeAtual, callback, auxRegion, tempo_inicio_atividade);

                consultarBd.start();
                consultaNaFila.start();
            }

            private void partiuAddRegion(AtomicBoolean RegProx, AtomicBoolean SubRegProx, AtomicBoolean RestRegProx) {


                String chave = (RegProx.get() ? "1" : "0") + (SubRegProx.get() ? "1" : "0") + (RestRegProx.get() ? "1" : "0");

                switch (chave) {
                    case "000": // Não há Região, SubRegião ou Região Restrita próxima
                        addNewRegion();
                        break;
                    case "101": // Há Região, não há SubRegião, há Região Restrita
                        runOnUiThread(() -> {
                            notification.setText("Região Restrita existente! \u26A0\uFE0F");
                            notification.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> notification.setVisibility(View.GONE), 1000);
                        });
                        break;
                    case "110": // Há Região, há SubRegião, não há Região Restrita
                        runOnUiThread(() -> {
                            notification.setText("Sub Região existente! \u26A0\uFE0F");
                            notification.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> notification.setVisibility(View.GONE), 1000);
                        });
                        break;
                    case "100": // Há Região, não há SubRegião, não há Região Restrita
                        addSUBorREST();
                        break;
                    default:
                        runOnUiThread(() -> {
                            notification.setText("Região existente! \u26A0\uFE0F");
                            notification.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> notification.setVisibility(View.GONE), 1000);
                        });
                        break;
                }
            }

            private void addNewRegion() {
                try {
                    contRegion++;
                    Region atualRegion = new Region(
                            "Região " + contRegion,
                            latitudeAtual,
                            longitudeAtual,
                            userId
                    );

                    CriptografarDados criptografarRegion = new CriptografarDados(atualRegion);
                    criptografarRegion.start();
                    criptografarRegion.join();  // Aguarda a conclusão da thread de criptografia

                    Time_T4 = ((System.nanoTime() - tempo_inicio_atividade)/1_000_000_000.0);

                    Region regionJsonCriptografada = criptografarRegion.getRegionEncryptedJson();  // Obtém o resultado após a conclusão
                    addRegionFila(regionJsonCriptografada);  // Adiciona a Region criptografada à fila
                } catch (InterruptedException e) {
                    System.err.println("Thread interrompida: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            private void addNewSubRegion() {
                try {
                    contSubRegion++;
                    SubRegion atualSubRegion = new SubRegion(
                            "Sub Região " + contSubRegion + " da " + auxRegion.getName(),
                            latitudeAtual,
                            longitudeAtual,
                            userId,
                            auxRegion
                    );

                    CriptografarDados criptografarSubRegion = new CriptografarDados(atualSubRegion);
                    criptografarSubRegion.start();
                    criptografarSubRegion.join();  // Aguarda a conclusão da thread de criptografia

                    Time_T4 = ((System.nanoTime() - tempo_inicio_atividade)/1_000_000_000.0);

                    SubRegion SubregionJsonCriptografada = criptografarSubRegion.getSRegionEncryptedJson();  // Obtém o resultado após a conclusão
                    addRegionFila(SubregionJsonCriptografada);  // Adiciona a string criptografada à fila

                } catch (Exception e) {
                    System.err.println("\n\nErro ao criptografar os dados: " + e.getMessage() + "\n\n");
                    e.printStackTrace();
                }
            }

            private void addNewRestrictedRegion() {
                try {
                    contRestRegion++;
                    RestrictedRegion atualRestRegion = new RestrictedRegion(
                            "Região restrita " + contRestRegion + " da " + auxRegion.getName(),
                            latitudeAtual,
                            longitudeAtual,
                            userId,
                            auxRegion,
                            true
                    );

                    CriptografarDados criptografarRestRegion = new CriptografarDados(atualRestRegion);
                    criptografarRestRegion.start();
                    criptografarRestRegion.join();  // Aguarda a conclusão da thread de criptografia

                    Time_T4 = ((System.nanoTime() - tempo_inicio_atividade)/1_000_000_000.0);

                    RestrictedRegion RestregionJsonCriptografada = criptografarRestRegion.getRRegionEncryptedJson();  // Obtém o resultado após a conclusão
                    addRegionFila(RestregionJsonCriptografada);  // Adiciona a Rest region criptografada à fila

                } catch (InterruptedException e) {
                    System.err.println("Thread interrompida: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            private void addSUBorREST() {
                if (ultimaAddSub) {
                    ultimaAddSub = false;
                    addNewRestrictedRegion();
                } else {
                    ultimaAddSub = true;
                    addNewSubRegion();
                }
            }

            private void addRegionFila(Region region) {
                try {
                    Thread adicionarNaFila = new AdicionarFila(filaDeRegions, region, semaphore);
                    adicionarNaFila.start();
                    adicionarNaFila.join();

                    X.setVisibility(View.VISIBLE);
                    if (Time_T1 > 0.45){
                        T1.setText("T1 NÃO É ESCALONÁVEL: " + String.format("%.3f", Time_T1) + " segundos...");
                    }else{
                        T1.setText("T1 É ESCALONÁVEL: " + String.format("%.3f", Time_T1) + " segundos...");
                    }

                    if (Time_T2 > 0.55){
                        T2.setText("T2 NÃO É ESCALONÁVEL: " + String.format("%.3f", Time_T2) + " segundos...");
                    }else {
                        T2.setText("T2 É ESCALONÁVEL: " + String.format("%.3f", Time_T2) + " segundos...");
                    }

                    if (Time_T3 > 0.85){
                        T3.setText("T3 NÃO É ESCALONÁVEL: " + String.format("%.3f", Time_T3) + " segundos...");
                    }else {
                        T3.setText("T3 É ESCALONÁVEL: " + String.format("%.3f", Time_T3) + " segundos...");
                    }

                    if (Time_T4 > 0.9){
                        T4.setText("T4 NÃO É ESCALONÁVEL: " + String.format("%.3f", Time_T4) + " segundos...");
                    }else{
                        T4.setText("T4 É ESCALONÁVEL: " + String.format("%.3f", Time_T4) + " segundos...");
                    }

                    Time_T5 = ((System.nanoTime() - tempo_inicio_atividade)/1_000_000_000.0);
                    if (Time_T5 > 1.0){
                        T5.setText("T5 NÃO É ESCALONÁVEL: " + String.format("%.3f", Time_T5) + " segundos...");
                    }else {
                        T5.setText("T5 É ESCALONÁVEL: " + String.format("%.3f", Time_T5) + " segundos...");
                    }
                    new Handler().postDelayed(() -> X.setVisibility(View.GONE), 2000);

                    String tipoRegion;
                    if (region instanceof SubRegion) {
                        tipoRegion = "SubRegião";
                    } else if (region instanceof RestrictedRegion) {
                        tipoRegion = "Região Restrita";
                    } else {
                        tipoRegion = "Região";
                    }

                    String finalTipoRegion = tipoRegion;
                    notification.setText(finalTipoRegion + " Adicionada! \u2705");
                    notification.setVisibility(View.VISIBLE);
                    new Handler().postDelayed(() -> notification.setVisibility(View.GONE), 1000);
                } catch (InterruptedException e) {
                    System.err.println("Thread interrompida: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        GravarBdButtom.setOnClickListener(v -> {
            AnimarBotton.aplicarPulsacao(v);
            RemFaddBD threadRemoverDaFila = new RemFaddBD(db, filaDeRegions, semaphore, uiHandler, AtividadePrincipal.this, notification);
            threadRemoverDaFila.start();
        });

        button_BD.setOnClickListener(v -> {
            AnimarBotton.aplicarPulsacao(v);
            Intent incia_BD = new Intent(this, Atividade_BD.class);
            startActivity(incia_BD);
        });

        botaoLimparFila.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimarBotton.aplicarPulsacao(v);
                try {
                    semaphore.acquire();
                    synchronized (filaDeRegions) {
                        if(!filaDeRegions.isEmpty()){
                            filaDeRegions.remove();
                        }else{
                            notification.setText("Fila vazia...");
                            notification.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> notification.setVisibility(View.GONE), 1000);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                }
                labelsizeFila.setText(format("%d Regiões na fila!", filaDeRegions.size()));
            }
        });

        botaoLimparFila.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AnimarBotton.aplicarPulsacaoLong(v);
                try {
                    semaphore.acquire();
                    synchronized (filaDeRegions) {
                        if (filaDeRegions.isEmpty()) {
                            notification.setText("Fila vazia...");
                            notification.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(() -> notification.setVisibility(View.GONE), 1000);
                        } else {
                            filaDeRegions.clear();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                }
                labelsizeFila.setText(format("%d Regiões na fila!", filaDeRegions.size()));
                return true;
            }
        });

        botaoCamada.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnimarBotton.aplicarPulsacao(v);
                if (mMap != null) {
                    // Alternar entre os tipos de mapa
                    if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
                        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                    } else {
                        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                    }
                }
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Solicitar permissões de localização em tempo de execução
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Começa a Thread quando a permissão já foi concedida
            new LerDadosGps(this).start();
        }
    }

    public void atualizarLocalization(double latitude, double longitude, long tempo_inicio) {
        latitudeAtual = latitude;
        longitudeAtual = longitude;
        timeUltimaLerDados = ((System.nanoTime() - tempo_inicio)/1_000_000_000.0);
        latitudeTextView.setText(format("Latitude: %.6f", latitudeAtual));
        longitudeTextView.setText(format("Longitude: %.6f", longitudeAtual));
        labelsizeFila.setText(format("%d Regiões na fila!",filaDeRegions.size()));

        // Remove o marcador anterior, se houver
        if (currentMarker != null) {
            currentMarker.remove();
        }

        // Adicionar um novo marcador na localização atual
        currentMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitudeAtual, longitudeAtual))
                .title("Localização Atual"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                inicializaRecursosLocalizacao(); // Inicializa recursos de GPS e mapa
            } else {
                // Exibe um diálogo de alerta se a permissão não for concedida
                new AlertDialog.Builder(this)
                        .setTitle("Permissão de Localização Necessária")
                        .setMessage("Esta aplicação precisa da permissão de localização para adicionar regiões. Por favor, conceda a permissão nas configurações do aplicativo.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .create().show();
            }
        }
    }

    private void inicializaRecursosLocalizacao() {
        new LerDadosGps(this).start(); // Inicia a thread de dados de GPS

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true); // Ativa a camada de localização no mapa
            FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
                }
            });
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
                }
            });
        }
    }

}