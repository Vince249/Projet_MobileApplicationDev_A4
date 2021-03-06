package com.example.td3_source_code_with_c

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import java.security.cert.Certificate
import java.sql.DriverManager.println
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLPeerUnverifiedException

class MainActivity : AppCompatActivity() {

    companion object {
        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }

    // Référence aux fonctions C++
    private external fun getAPIconfig(): String

    private external fun getKeyEncryption(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private var inputUserMasterKey: String = ""

    override fun onStart(){
        super.onStart()

        Confirm_Button.setOnClickListener{
            connectFunction()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun connectFunction(){
        // On hash la masterkey suivant un "Secure Hash Algorithm". Ici on va utiliser un SHA-1.
        // J'ai executé ce code une seule fois pour avoir la hashed masterkey mais je le mets maintenant en commentaire.
        // Bien sûr, dans un réel projet, ces lignes seraient supprimées pour sécuriser le code source
        // mais dans le cadre scolaire, je les laisse pour montrer comment j'ai fait
        /*
        // La poly encryption ne fonctionne pas mis ici car la poly encryption utilise l'input du user
        // mais je le mets pour me souvenir que je l'ai fais
        // Si on veut que ça fonctionne il faut le mettre après que le user ait rentré quelque chose pour la masterkey
        val EncryptedMasterKey = polyEncryption(hashedMasterKey)

        val EncryptedMasterKey = "MSA#G"
        val bytesToHash2 = MessageDigest.getInstance("SHA-1").digest(EncryptedMasterKey.toByteArray())
        val hashedMasterKey = bytesToHash2.joinToString("") {
            "%02x".format(it)
        }
        */

        var connect = true // Variable pour déterminer si user peut passer à la page suivante ou pas
        var dataJson = JSONObject()

        // Vérifier que la masterkey entrée est ok
        val hashedAndEncryptedMasterKey = "52c37332dd58c200520a21b91834a853b32e9844" // Je l'ai eu avec code au-dessus

        //// On va encrypter puis hash l'input pour voir si elle correspond au encryption + hash de la masterkey
        inputUserMasterKey = MasterKey_EditBox.text.toString()
        var hashedAndEncryptedInputMasterKey = ""

        if (inputUserMasterKey != ""){
            val encryptedInputMasterKey = polyEncryption(inputUserMasterKey)
            val bytesToHash = MessageDigest.getInstance("SHA-1").digest(encryptedInputMasterKey.toByteArray())
            hashedAndEncryptedInputMasterKey = bytesToHash.joinToString("") {
                "%02x".format(it)
            }

        }

        // Si l'input masterkey fausse
        if (hashedAndEncryptedMasterKey != hashedAndEncryptedInputMasterKey){
            connect = false
            Error_TextBox.text = "Error in the masterkey"
        }

        // Vérifier si un id a été rentré ou pas
        // S'il n'est pas renseigné on arrive sur une page avec tous les id qui existe donc on n'aura pas d'erreur
        // mais nous ne voulons pas ces valeurs. Nous ne voulons qu'un id
        if (id_EditBox.text.toString() == ""){
            connect = false
        }

        // Aller chercher les data sur la l'API
        // Si id donné en paramètre redirige vers une page qui n'existe pas, dataJson sera uniquement composé de ["error":true]
        if (id_EditBox.text.toString() != ""){
            dataJson = getJsonFromURLorFile(id_EditBox.text.toString())

            // Si page n'existe pas ou que l'on est hors ligne
            if (dataJson["error"] == true){
                connect = false
            }
        }

        // Si id pas rentré ou id ne mène vers aucune page ou il n'y a pas de données sauvegardées localement
        // Mais que le user a rentré la bonne masterkey
        if (!connect && hashedAndEncryptedMasterKey == hashedAndEncryptedInputMasterKey){
            Error_TextBox.text = "Error in the id or you are offline and have no data saved locally for this id"
        }

        // Si masterkey ok et id redirige vers une page qui existe (API)
        // --> Passer à la page Accounts en donnant l'id, le nom et prenom en paramètre
        // On passe également l'input du user pour la masterkey car on utilise
        // les fonctions de poly encryption et decryption dans la page suivante
        if (connect){
            val goToAccountsPage = Intent(this, Accounts::class.java)
            goToAccountsPage.putExtra("id", dataJson["id"].toString())
            goToAccountsPage.putExtra("firstName", dataJson["name"].toString())
            goToAccountsPage.putExtra("lastName", dataJson["lastname"].toString())
            goToAccountsPage.putExtra("inputUserMasterKey", inputUserMasterKey)
            startActivity(goToAccountsPage)
        }

    }

    // Return un json avec les infos du user par rapport à l'id lorsqu'il est en ligne
    private fun getJsonFromURLorFile(idTextBox: String): JSONObject {
        var dataJson = JSONObject()
        var url: URL
        runBlocking {
            val job = GlobalScope.async {
                try {
                    val concatenatedURL = "${polyDecryption(getAPIconfig())}$idTextBox"
                    url = URL(concatenatedURL)
                    val con = url.openConnection() as HttpsURLConnection

                    // Si je veux voir tous les logs
                    //printHttpsCert(con)

                    val data = con.inputStream.bufferedReader().readText()
                    dataJson = JSONObject(data)
                    dataJson.put("error", false)

                    // Update les data dans le fichier associé à l'url
                    // Si le fichier n'existe pas, il est créé et s'il existe, il est écrasé
                    writeFile(dataJson.toString(), idTextBox)

                    // Disconnect
                    con.disconnect()
                }
                // Erreur lorsque le lien en prenant en compte idTextBox n'amène vers aucune page.
                // S'il n'est pas renseigné (idTextBox) on arrive sur une page avec tous les id qui existe
                // Donc cela est géré dans la connect function
                catch (e: FileNotFoundException) {
                    dataJson.put("error", true)
                }
                // Erreur lorsque le user est hors ligne
                catch (e: UnknownHostException){
                    val data = readFile(idTextBox)
                    if (data == "error"){
                        // le fichier n'existe pas donc il n'y a pas de data
                        dataJson.put("error", true)
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

    // Lis le contenu d'un fichier dont le nom est donné en paramètre et le return en string
    private fun readFile(fileName: String): String {
        var dataDecrypted = "error"

        // Ici on se place dans /storage/emulated/0/Android/data/com.example.app_td3/files
        val path = this.getExternalFilesDir(null)

        // Puis on créé un dossier "config"
        val folder = File(path, "config")

        // On vérifie si ce dossier existe ou non
        if (folder.exists()){
            val file = File(folder, fileName)
            // On vérifie si ce fichier existe ou non
            if (file.exists()){
                val data = file.bufferedReader().readText()
                dataDecrypted = polyDecryption(data)
            }
        }
        // Si tout existe, data sera égale à ce que contient le fichier
        // Si non, data sera égale à sa valeur d'initialisation
        return dataDecrypted
    }

    // Ecris dans un fichier de la data (string) dont le nom est donné en paramètre
    private fun writeFile(data: String, fileName: String) {
        try {
            // Pour voir les dossiers du telephone, il faut aller dans le Device File Explorer

            // Ici on se place dans /storage/emulated/0/Android/data/com.example.app_td3/files
            val path = this.getExternalFilesDir(null)

            // Puis on créé un dossier "config"
            val folder = File(path, "config")

            // Créer le dossier en question s'il ne l'est pas déjà
            folder.mkdirs()

            // Et enfin on met le fichier dans le dossier "config"
            // Son nom est l'id rentré par le user
            val file = File(folder, fileName)

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
    // Utilisée uniquement au début pour voir si la connexion https fonctionnait
    private fun printHttpsCert(con: HttpsURLConnection?) {
        if (con != null) {
            try {
                Log.d("Response Code", con.responseCode.toString())
                Log.d("Cipher Suite", con.cipherSuite)
                println("\n")
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