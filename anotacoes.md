// 1-MainActivy.java

package com.example.sunmiprinter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerResultCallback;
import com.sunmi.peripheral.printer.SunmiPrinterService;

public class MainActivity extends AppCompatActivity {

    private SunmiPrinterService printerService;
    private TextView statusText;
    private Button btnPrint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar views
        statusText = findViewById(R.id.tvStatus);
        btnPrint = findViewById(R.id.btnTestPrint);

        // Conectar à impressora
        connectPrinter();
    }

    private void connectPrinter() {
        try {
            boolean result = InnerPrinterManager.getInstance().bindService(this, printerCallback);

            if (result) {
                updateStatus("Conectando...");
            } else {
                updateStatus("Falha na conexão");
                Toast.makeText(this, "Não foi possível conectar à impressora", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            updateStatus("Erro: " + e.getMessage());
            Toast.makeText(this, "Erro ao conectar: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private InnerPrinterCallback printerCallback = new InnerPrinterCallback() {
        @Override
        protected void onConnected(SunmiPrinterService service) {
            printerService = service;
            runOnUiThread(() -> {
                updateStatus("Conectado");
                Toast.makeText(MainActivity.this, "Impressora conectada!", Toast.LENGTH_SHORT).show();
                btnPrint.setEnabled(true);
            });
        }

        @Override
        protected void onDisconnected() {
            printerService = null;
            runOnUiThread(() -> {
                updateStatus("Desconectado");
                Toast.makeText(MainActivity.this, "Impressora desconectada", Toast.LENGTH_SHORT).show();
                btnPrint.setEnabled(false);
            });
        }
    };

    public void onTestPrintClick(View view) {
        if (printerService == null) {
            Toast.makeText(this, "Impressora não conectada", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Configurar modo de impressão (remove background)
            printerService.printerInit(printCallback);

            // Imprimir logo/imagem
            printLogo();

            // Imprimir texto formatado
            printerService.printText("=== TESTE DE IMPRESSÃO ===\n", null);
            printerService.printText("Data: " + new java.util.Date().toString() + "\n", null);
            printerService.printText("Status: OK\n", null);
            printerService.printText("===========================\n", null);

            // Quebra de linha
            printerService.lineWrap(2, null);

        } catch (RemoteException e) {
            Toast.makeText(this, "Erro ao imprimir: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void onPrintReceiptClick(View view) {
        if (printerService == null) {
            Toast.makeText(this, "Impressora não conectada", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Inicializar impressora (limpa formatação anterior)
            printerService.printerInit(null);
            

            // Imprimir logo
            printLogo();

            // Cabeçalho centralizado
            printerService.setAlignment(1, null); // 0=esquerda, 1=centro, 2=direita
            printerService.printText("RECIBO DE VENDA\n", null);
            printerService.printText("================================\n", null);

            // Voltar alinhamento à esquerda
            printerService.setAlignment(0, null);
            printerService.printText("Data: " + java.text.DateFormat.getDateTimeInstance().format(new java.util.Date()) + "\n", null);
            printerService.printText("--------------------------------\n", null);

            // Itens
            printerService.printText("Item 1: Produto A     1000,00 kz\n", null);
            printerService.printText("Item 2: Produto B    1500,50 kz\n", null);
            printerService.printText("Item 3: Produto C      800,30 kz\n", null);
            printerService.printText("--------------------------------\n", null);

            // Total centralizado e em negrito
            printerService.setAlignment(1, null);
           // printerService.sendRAWData(new byte[]{0x1B, 0x45, 0x00}, null); // Negrito ON
            printerService.printText("TOTAL:  3000,80 kz\n", null);
           // printerService.sendRAWData(new byte[]{0x1B, 0x45, 0x00}, null); // Negrito OFF

            printerService.printText("================================\n", null);
            printerService.printText("Obrigado pela preferência!\n ", null);

            // Resetar alinhamento
            printerService.setAlignment(0, null);
            printerService.lineWrap(3, null);

            Toast.makeText(this, "Recibo enviado!", Toast.LENGTH_SHORT).show();

        } catch (RemoteException e) {
            Toast.makeText(this, "Erro ao imprimir recibo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void onCheckStatusClick(View view) {
        if (printerService != null) {
            Toast.makeText(this, "Impressora está conectada e operacional", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Impressora não está conectada", Toast.LENGTH_SHORT).show();
            connectPrinter();
        }
    }

    public void onPrintImageClick(View view) {
        if (printerService == null) {
            Toast.makeText(this, "Impressora não conectada", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            printerService.printerInit(null);
            printLogo();
            printerService.printText("Teste de impressão com imagem!\n", null);
            printerService.lineWrap(2, null);
        } catch (RemoteException e) {
            Toast.makeText(this, "Erro ao imprimir imagem", Toast.LENGTH_SHORT).show();
        }
    }

    public void onPrintQRClick(View view) {
        if (printerService == null) {
            Toast.makeText(this, "Impressora não conectada", Toast.LENGTH_SHORT).show();
            return;
        }

        printQRCode("https://www.example.com");
        Toast.makeText(this, "QR Code enviado para impressão", Toast.LENGTH_SHORT).show();
    }

    // Método para imprimir logo/imagem
    private void printLogo() {
        try {
            // Opção 1: Imprimir imagem dos resources
            Bitmap logoBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground);
            if (logoBitmap != null) {
                // Redimensionar imagem para impressão (máximo 384 pixels de largura para SUNMI)
                Bitmap resizedBitmap = resizeBitmap(logoBitmap, 200, 100);
                printerService.printBitmap(resizedBitmap, printCallback);
                printerService.lineWrap(1, null);
            }

            // Opção 2: Criar uma imagem de texto personalizada
            Bitmap textBitmap = createTextBitmap("MINHA LOJA", 32, true);
            printerService.printBitmap(textBitmap, null);
            printerService.lineWrap(1, null);

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Método para redimensionar bitmap
    private Bitmap resizeBitmap(Bitmap originalBitmap, int newWidth, int newHeight) {
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false);
    }

    // Método para criar bitmap de texto personalizado
    private Bitmap createTextBitmap(String text, int textSize, boolean bold) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);

        if (bold) {
            paint.setTypeface(Typeface.DEFAULT_BOLD);
        }

        // Calcular dimensões
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        int textWidth = (int) paint.measureText(text);
        int textHeight = (int) (fontMetrics.bottom - fontMetrics.top);

        // Criar bitmap
        Bitmap bitmap = Bitmap.createBitmap(textWidth + 20, textHeight + 10, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE); // Fundo branco

        // Desenhar texto
        canvas.drawText(text, (textWidth + 20) / 2, textHeight - fontMetrics.bottom + 5, paint);

        return bitmap;
    }

    // Método para imprimir QR Code (se necessário)
    public void printQRCode(String content) {
        try {
            if (printerService != null) {
                // Tamanho do QR Code: 0-10 (0=pequeno, 10=grande)
                printerService.printQRCode(content, 6, 3, printCallback);
                printerService.lineWrap(2, null);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Método para imprimir código de barras
    public void printBarcode(String content) {
        try {
            if (printerService != null) {
                // Parâmetros: código, tipo (8=CODE128), altura, largura, posição do texto
                printerService.printBarCode(content, 8, 162, 2, 2, printCallback);
                printerService.lineWrap(2, null);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private InnerResultCallback printCallback = new InnerResultCallback() {
        @Override
        public void onRunResult(boolean isSuccess) throws RemoteException {
            runOnUiThread(() -> {
                if (isSuccess) {
                    Toast.makeText(MainActivity.this, "Impressão enviada com sucesso", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Falha ao enviar impressão", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onReturnString(String result) throws RemoteException {
            // Dados retornados (se houver)
        }

        @Override
        public void onRaiseException(int code, String msg) throws RemoteException {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Erro na impressão: " + msg, Toast.LENGTH_LONG).show();
            });
        }

        @Override
        public void onPrintResult(int code, String msg) throws RemoteException {
            runOnUiThread(() -> {
                if (code == 0) {
                    Toast.makeText(MainActivity.this, "Impressão concluída!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Erro na impressão: " + msg, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private void updateStatus(String status) {
        if (statusText != null) {
            statusText.setText("Status: " + status);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (printerCallback != null) {
                InnerPrinterManager.getInstance().unBindService(this, printerCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

// 2-activity_main.xml

<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:gravity="center"
    tools:context=".MainActivity">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="SUNMI Printer Test"
        android:textSize="24sp"
        android:textStyle="bold"
        android:layout_marginBottom="32dp" />

    <Button
        android:id="@+id/btnTestPrint"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Imprimir Teste"
        android:textSize="16sp"
        android:layout_marginBottom="16dp"
        android:onClick="onTestPrintClick" />

    <Button
        android:id="@+id/btnPrintReceipt"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Imprimir Recibo"
        android:textSize="16sp"
        android:layout_marginBottom="16dp"
        android:onClick="onPrintReceiptClick" />

    <Button
        android:id="@+id/btnCheckStatus"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Verificar Status"
        android:textSize="16sp"
        android:layout_marginBottom="16dp"
        android:onClick="onCheckStatusClick" />

    <Button
        android:id="@+id/btnPrintImage"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Imprimir com Imagem"
        android:textSize="16sp"
        android:layout_marginBottom="16dp"
        android:onClick="onPrintImageClick" />

    <Button
        android:id="@+id/btnPrintQR"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Imprimir QR Code"
        android:textSize="16sp"
        android:onClick="onPrintQRClick" />

    <TextView
        android:id="@+id/tvStatus"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Status: Desconectado"
        android:textSize="14sp"
        android:layout_marginTop="32dp"
        android:textColor="#666666" />

</LinearLayout>

// 3- AndroidManifest.xml
 <!-- Permissões para impressora SUNMI -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <!-- Permissão para dispositivos SUNMI -->
    <uses-permission android:name="com.sunmi.permission.PRINTER" />

// 4- build.gradle (Module: app)

dependencies {

    implementation libs.appcompat
    implementation libs.material
    implementation libs.activity
    implementation libs.constraintlayout
    testImplementation libs.junit
    androidTestImplementation libs.ext.junit
    androidTestImplementation libs.espresso.core

    implementation 'com.sunmi:printerlibrary:1.0.13'


}

// 5- settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}



