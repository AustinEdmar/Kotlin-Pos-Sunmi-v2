package com.example.sunmiprinter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.RemoteException
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.InnerResultCallback
import com.sunmi.peripheral.printer.SunmiPrinterService
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private var printerService: SunmiPrinterService? = null
    private lateinit var statusText: TextView
    private lateinit var btnPrint: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar views
        statusText = findViewById(R.id.tvStatus)
        btnPrint = findViewById(R.id.btnTestPrint)

        // Conectar à impressora
        connectPrinter()
    }
    companion object {
        const val SMALL_MARGIN = 1
        const val MEDIUM_MARGIN = 2
        const val LARGE_MARGIN = 3
        const val SECTION_MARGIN = 4
        const val PAGE_END_MARGIN = 5
    }

    private fun connectPrinter() {
        try {
            val result = InnerPrinterManager.getInstance().bindService(this, printerCallback)

            if (result) {
                updateStatus("Conectando...")
            } else {
                updateStatus("Falha na conexão")
                Toast.makeText(this, "Não foi possível conectar à impressora", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            updateStatus("Erro: ${e.message}")
            Toast.makeText(this, "Erro ao conectar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private val printerCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService?) {
            printerService = service
            runOnUiThread {
                updateStatus("Conectado")
                Toast.makeText(this@MainActivity, "Impressora conectada!", Toast.LENGTH_SHORT).show()
                btnPrint.isEnabled = true
            }
        }

        override fun onDisconnected() {
            printerService = null
            runOnUiThread {
                updateStatus("Desconectado")
                Toast.makeText(this@MainActivity, "Impressora desconectada", Toast.LENGTH_SHORT).show()
                btnPrint.isEnabled = false
            }
        }
    }

    fun onTestPrintClick(view: View) {
        if (printerService == null) {
            Toast.makeText(this, "Impressora não conectada", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Configurar modo de impressão (remove background)
            printerService?.printerInit(printCallback)

            // Imprimir logo/imagem
            printLogo()

            // Imprimir texto formatado
            printerService?.printText("=== TESTE DE IMPRESSÃO ===\n", null)
            printerService?.printText("Data: ${Date()}\n", null)
            printerService?.printText("Status: OK\n", null)
            printerService?.printText("===========================\n", null)

            // Quebra de linha
            printerService?.lineWrap(2, null)

        } catch (e: RemoteException) {
            Toast.makeText(this, "Erro ao imprimir: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun onPrintReceiptClick(view: View) {
        if (printerService == null) {
            Toast.makeText(this, "Impressora não conectada", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Inicializar impressora (limpa formatação anterior)
            printerService?.printerInit(null)

            // Imprimir logo
            printerService?.printText("\n \n ", null)
            printLogo()

            // Cabeçalho centralizado
            printerService?.setAlignment(1, null) // 0=esquerda, 1=centro, 2=direita
            printerService?.printText("RECIBO DE VENDA\n", null)
            printerService?.printText("================================\n", null)

            // Voltar alinhamento à esquerda
            printerService?.setAlignment(0, null)
            printerService?.printText("Data: ${DateFormat.getDateTimeInstance().format(Date())}\n", null)
            printerService?.printText("--------------------------------\n", null)

            // Itens
            printerService?.printText("Item 1: Produto A     1000,00 kz\n", null)
            printerService?.printText("Item 2: Produto B    1500,50 kz\n", null)
            printerService?.printText("Item 3: Produto C      800,30 kz\n", null)
            printerService?.printText("--------------------------------\n", null)

            // Total centralizado e em negrito
            printerService?.setAlignment(1, null)
            printerService?.printText("TOTAL:  3000,80 kz\n", null)
            //printerService?.lineWrap(3, null)

            printerService?.printText("================================\n", null)
            printerService?.printText("Obrigado pela preferência! ", null)
            printerService?.printText("\n \n \n \n \n", null)
            printerService?.lineWrap(PAGE_END_MARGIN, null)

            // Resetar alinhamento
            printerService?.setAlignment(0, null)
           // printerService?.lineWrap(5, null)

            Toast.makeText(this, "Recibo enviado!", Toast.LENGTH_SHORT).show()

        } catch (e: RemoteException) {
            Toast.makeText(this, "Erro ao imprimir recibo: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun onCheckStatusClick(view: View) {
        if (printerService != null) {
            Toast.makeText(this, "Impressora está conectada e operacional", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Impressora não está conectada", Toast.LENGTH_SHORT).show()
            connectPrinter()
        }
    }

    fun onPrintImageClick(view: View) {
        if (printerService == null) {
            Toast.makeText(this, "Impressora não conectada", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            printerService?.printerInit(null)
            printLogo()
            printerService?.printText("Teste de impressão com imagem!\n", null)
            printerService?.lineWrap(2, null)
        } catch (e: RemoteException) {
            Toast.makeText(this, "Erro ao imprimir imagem", Toast.LENGTH_SHORT).show()
        }
    }

    fun onPrintQRClick(view: View) {
        if (printerService == null) {
            Toast.makeText(this, "Impressora não conectada", Toast.LENGTH_SHORT).show()
            return
        }

        printQRCode("https://www.example.com")
        Toast.makeText(this, "QR Code enviado para impressão", Toast.LENGTH_SHORT).show()
    }

    // Método para imprimir logo/imagem
    private fun printLogo() {
        try {
            // Opção 1: Imprimir imagem dos resources
            val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.logo)
            logoBitmap?.let {
                // Redimensionar imagem para impressão (máximo 384 pixels de largura para SUNMI)
                val resizedBitmap = resizeBitmap(it, 200, 100)
                printerService!!.setAlignment(1, null) // 0: esquerda, 1: centro, 2: direita
                printerService?.printBitmap(resizedBitmap, printCallback)
                printerService?.lineWrap(3, null)
            }

            // Opção 2: Criar uma imagem de texto personalizada
            val textBitmap = createTextBitmap("LOJA AUSTIN", 32, true)
            printerService?.printBitmap(textBitmap, null)
            printerService?.lineWrap(2, null)

        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    // Método para redimensionar bitmap
    private fun resizeBitmap(originalBitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, false)
    }

    // Método para criar bitmap de texto personalizado
    private fun createTextBitmap(text: String, textSize: Int, bold: Boolean): Bitmap {
        val paint = Paint().apply {
            color = Color.BLACK
            this.textSize = textSize.toFloat()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER

            if (bold) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }

        // Calcular dimensões
        val fontMetrics = paint.fontMetrics
        val textWidth = paint.measureText(text).toInt()
        val textHeight = (fontMetrics.bottom - fontMetrics.top).toInt()

        // Criar bitmap
        val bitmap = Bitmap.createBitmap(textWidth + 20, textHeight + 10, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE) // Fundo branco

        // Desenhar texto
        canvas.drawText(text, (textWidth + 20) / 2f, textHeight - fontMetrics.bottom + 5, paint)

        return bitmap
    }

    // Método para imprimir QR Code (se necessário)
    fun printQRCode(content: String) {
        try {
            printerService?.let {
                // Tamanho do QR Code: 0-10 (0=pequeno, 10=grande)
                it.printQRCode(content, 6, 3, printCallback)
                it.lineWrap(2, null)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    // Método para imprimir código de barras
    fun printBarcode(content: String) {
        try {
            printerService?.let {
                // Parâmetros: código, tipo (8=CODE128), altura, largura, posição do texto
                it.printBarCode(content, 8, 162, 2, 2, printCallback)
                it.lineWrap(2, null)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    private val printCallback = object : InnerResultCallback() {
        override fun onRunResult(isSuccess: Boolean) {
            runOnUiThread {
                if (isSuccess) {
                    Toast.makeText(this@MainActivity, "Impressão enviada com sucesso", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Falha ao enviar impressão", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onReturnString(result: String?) {
            // Dados retornados (se houver)
        }

        override fun onRaiseException(code: Int, msg: String?) {
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Erro na impressão: $msg", Toast.LENGTH_LONG).show()
            }
        }

        override fun onPrintResult(code: Int, msg: String?) {
            runOnUiThread {
                if (code == 0) {
                    Toast.makeText(this@MainActivity, "Impressão concluída!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Erro na impressão: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun updateStatus(status: String) {
        statusText.text = "Status: $status"
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            InnerPrinterManager.getInstance().unBindService(this, printerCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
