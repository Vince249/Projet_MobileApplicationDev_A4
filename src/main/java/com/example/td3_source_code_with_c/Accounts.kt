package com.example.td3_source_code_with_c


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_accounts.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import java.net.UnknownHostException
import java.security.cert.Certificate
import java.sql.DriverManager
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException


class Accounts : AppCompatActivity() {

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }


    // Référence aux fonctions C++
    private external fun getAPIaccounts(): String

    private external fun getKeyEncryption(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)
    }

    private var inputUserMasterKey: String = ""

    @SuppressLint("SetTextI18n")
    override fun onStart(){
        super.onStart()

        // Remplir la Hello_TextBox avec le nom et prénom de l'id passé en paramètre depuis la page précédente
        val id = intent.extras?.get("id")
        val firstName = intent.extras?.get("firstName")
        val lastName = intent.extras?.get("lastName")
        inputUserMasterKey = intent.extras?.get("inputUserMasterKey") as String

        Hello_TextBox.text = "Hello $firstName $lastName"
        id_TextBox.text = "Your id is : $id"

        actualizeButtonFunction()

        Actualize_Button.setOnClickListener{
            actualizeButtonFunction()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun actualizeButtonFunction(){
        // Récupérer les data depuis https://60102f166c21e10017050128.mockapi.io/labbbank/accounts
        // Etablir une connexion sécurisée pour se connecter à cet url
        val dataJson = getJsonFromURLorFile()

        val dataJsonArray: JSONArray
        // Remplir la listView des accounts si c'est possible, sinon afficher message erreur
        // Au cas où la connexion saute entre le moment où le user se connecte et le moment où il arrive sur la page
        // S'il arrive à rétablir la connexion, il pourra d'ailleurs cliquer sur le bouton "Actualize"
        if (dataJson["error"] as Boolean){
            Error_TextBox.text = "You are offline and have no data saved locally"
        } else{
            // Conversion en json array
            dataJsonArray = dataJson.getJSONArray("accounts")
            fillListViewAccounts(dataJsonArray)
            Error_TextBox.text = ""
        }
    }

    // Return un json avec les accounts et une possible erreur ou non
    private fun getJsonFromURLorFile(): JSONObject{
        var url: URL
        var dataJson = JSONObject()
        runBlocking {
            val job = GlobalScope.async {
                try {
                    val concatenatedURL = polyDecryption(getAPIaccounts())
                    url = URL(concatenatedURL)
                    val con = url.openConnection() as HttpsURLConnection

                    // Si je veux voir tous les logs
                    //printHttpsCert(con)

                    var data = con.inputStream.bufferedReader().readText()
                    // Ajout de caractères pour qu'il ressemble à un json object (c'est un tableau json et non un json simple)
                    data = "{'accounts':$data}"

                    // Conversion en json object
                    dataJson = JSONObject(data)

                    // Ajout error = false
                    dataJson.put("error",false)

                    // Update les data dans le fichier associé à l'url
                    // Si le fichier n'existe pas, il est créé et s'il existe, il est écrasé
                    writeFile(dataJson.toString())

                    // Disconnect
                    con.disconnect()
                }
                // Erreur lorsque le user est hors ligne
                catch (e: UnknownHostException){
                    val data = readFile()
                    if (data == "error"){
                        // le fichier n'existe pas donc il n'y a pas de data
                        dataJson.put("error",true)
                    } else{
                        dataJson = JSONObject(data)
                    }
                }
                dataJson
            }
            dataJson = job.await()
        }
        return dataJson
    }

    // Permet de remplir la listView à partir d'un JSONArray
    private fun fillListViewAccounts(dataJsonArray: JSONArray){
        // Remplissage de la listeView
        val accountList = ArrayList<String>()

        for (i in 0 until dataJsonArray.length()){
            val currentID = dataJsonArray.getJSONObject(i)["id"].toString()
            val currentAccountName = dataJsonArray.getJSONObject(i)["accountName"].toString()
            val currentAmount = dataJsonArray.getJSONObject(i)["amount"].toString()
            val currentIban = dataJsonArray.getJSONObject(i)["iban"].toString()
            val currentCurrency = dataJsonArray.getJSONObject(i)["currency"].toString()

            accountList.add("ID : $currentID \n" +
                    "Account name : $currentAccountName \n" +
                    "Amount : $currentAmount \n" +
                    "Iban : $currentIban \n" +
                    "Currency : $currentCurrency \n")
        }

        Accounts_ListView.adapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, accountList)
    }

    // Lis le contenu d'un fichier dont le nom est donné en paramètre et le return en string
    private fun readFile(): String {
        var dataDecrypted = "error"

        // Ici on se place dans /storage/emulated/0/Android/data/com.example.app_td3/files
        val path = this.getExternalFilesDir(null)

        val file = File(path, "accounts")
        // On vérifie si ce fichier existe ou non
        if (file.exists()){
            val data = file.bufferedReader().readText()
            dataDecrypted = polyDecryption(data)
        }

        // Si tout existe, data sera égale à ce que contient le fichier
        // Si non, data sera égalé à sa valeur d'initialisation ligne 136
        return dataDecrypted
    }

    // Ecris dans un fichier de la data (string) dont le nom est donné en paramètre
    private fun writeFile(data: String) {
        try {
            // Pour voir les dossiers du telephone, il faut aller dans le Device File Explorer

            // Ici on se place dans /storage/emulated/0/Android/data/com.example.app_td3/files
            val path = this.getExternalFilesDir(null)

            // Et on met le fichier dans le dossier files (celui par défaut)
            // On ne créé pas de dossier comme pour config car ici il n'y aura qu'un seul fichier
            val file = File(path, "accounts")

            // On écrit dans le fichier --> on écrase ce qui a été écrit volontairement
            // On ne veut qu'une seule ligne dans le fichier (tout comme on a en accédant à l'API)
            // Je n'ai pas trouvé comment cacher l'endroit où est sauvegardé le fichier mais
            // on peut au moins crypter son contenu
            val dataEncrypted = polyEncryption(data)
            file.writeText(dataEncrypted)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun polyEncryption(to_encrypt: String): String {
        val encrypted = StringBuilder()
        val key = inputUserMasterKey
        val size= key.length
        for(i in 0..to_encrypt.length - 1){
            var newAscii= to_encrypt[i].toInt() + key[i%size].toInt()
            if(newAscii>127) newAscii= 32 + newAscii%127
            encrypted.append(newAscii.toChar())
        }
        return encrypted.toString()
    }

    private fun polyDecryption(to_decrypt: String): String {
        val decrypted = StringBuilder()
        val key = inputUserMasterKey
        val size= key.length
        for(i in 0..to_decrypt.length - 1){
            var oldAscii= to_decrypt[i].toInt() - key[i%size].toInt()
            if(oldAscii<32){
                val diff=32-oldAscii
                oldAscii=127-diff
            }
            decrypted.append(oldAscii.toChar())
        }
        return decrypted.toString()
    }

    // Permet de voir tous les logs dû à la connexion https
    private fun printHttpsCert(con: HttpsURLConnection?) {
        if (con != null) {
            try {
                Log.d("Response Code", con.responseCode.toString())
                Log.d("Cipher Suite", con.cipherSuite)
                DriverManager.println("\n")
                val certs: Array<Certificate> = con.serverCertificates
                for (cert in certs) {
                    Log.d("Cert Type", cert.type)
                    Log.d("Cert Hash Code : ", cert.hashCode().toString())
                    Log.d("Cert Public Key Algo", cert.publicKey.algorithm)
                    Log.d("Cert Public Key Format", cert.publicKey.format)
                }
            } catch (e: SSLPeerUnverifiedException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}