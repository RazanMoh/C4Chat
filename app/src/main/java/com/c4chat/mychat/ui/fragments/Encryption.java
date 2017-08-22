package com.c4chat.mychat.ui.fragments;

/**
 * Created by hendalzahrani on 8/9/17.
 */

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.c4chat.mychat.utils.SharedPrefUtil;

import org.json.JSONObject;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class Encryption{
    SecretKey sharedKey=null;
    BigInteger p;
    BigInteger g;
    JSONObject MyJson, sentJoson;
    SharedPrefUtil pre;
    Context context;

    public Encryption(Context c){
         context= c;
    }


    public String getAction(String room, String message) {
        String msg = "";

         pre = new SharedPrefUtil(context);
        String jsonString = pre.getString(room);

        if(jsonString != null) {
            Log.d("json string in action: ", jsonString);

            try {
                JSONObject json = new JSONObject(jsonString);

                 if (json.get("SharedKey") == null) {
                     Log.d("SharedKey null: ", message);


                    json.put("messages", json.get("messages") + "\n" + message);

                    jsonString = json.toString();
                    pre.saveString(room, jsonString);
                     return null;

                } else{

                     Log.d("SharedKey NOT null: ", message);
                     String en= encrypt(room, message);
                     Log.d("Encrypted message: ", en);

                    return en;
                }


            } catch (Exception e) {

            }

        }
        else {
            return initiatePG(room, message);

        }

        Log.d("getAction msg: ", "82");
        return msg;
    }


    public String recieveKey(String room, String message){

        Log.d("message in rKey 96", message);

        try{
            JSONObject Recievedjson= new JSONObject(message);
            Log.d("json created", "done");
            String pr= Recievedjson.getString("p");
            String gr= Recievedjson.getString("g");
            String pu= Recievedjson.getString("PublicKey");
            String init= Recievedjson.getString("init");
            Log.d("json values", "gotten");

            BigInteger pr2= new BigInteger(pr);
            BigInteger gr2= new BigInteger(gr);

            SharedPrefUtil pre = new SharedPrefUtil(context);

            String jsonString= pre.getString(room);

            if(jsonString != null) {
                Log.d("json in receivekey 106", jsonString);
                JSONObject j= new JSONObject(jsonString);
                if(j.has("SharedKey"))
                    return null;
            }

            sentJoson= new JSONObject();
            //
            if(jsonString ==null){
                Log.d("init sentJson: ", "enc: 102");

                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
                DHParameterSpec dhSpec = new DHParameterSpec(pr2, gr2);
                keyGen.initialize(dhSpec);
                KeyPair keypair = keyGen.generateKeyPair();

                // Get the generated public and private keys
                PrivateKey privateKey = keypair.getPrivate();
                PublicKey publicKey = keypair.getPublic();
                byte[] publicKeyBytes = publicKey.getEncoded();
                String sentPublicKey = Arrays.toString(publicKeyBytes);
                String myPrivateString = Arrays.toString(privateKey.getEncoded());

                MyJson = new JSONObject();
                MyJson.put("p", pr);
                MyJson.put("g", gr);
                MyJson.put("PrivateKey", myPrivateString);


                sentJoson.put("p", pr);
                sentJoson.put("g", gr);
                sentJoson.put("PublicKey", sentPublicKey);
                sentJoson.put("init", init);
                jsonString = MyJson.toString();
                pre.saveString(room, jsonString);


            }

                 MyJson = new JSONObject(jsonString);
                String myKey = MyJson.getString("PrivateKey");

                if (myKey != null) {
                    Log.d("myKey", "136");

                    String[] byteValues = myKey.substring(1, myKey.length() - 1).split(",");
                    byte[] bytes = new byte[byteValues.length];

                    for (int i = 0, len = bytes.length; i < len; i++) {
                        bytes[i] = Byte.parseByte(byteValues[i].trim());
                    }

                    // Convert the private key bytes into a PrivateKey object
                    PKCS8EncodedKeySpec x509KeySpec = new PKCS8EncodedKeySpec(bytes);
                    KeyFactory keyFact = KeyFactory.getInstance("DH");
                    PrivateKey privateKey = keyFact.generatePrivate(x509KeySpec);

                    //////////////////////////////////////////////////////////////////////

                    byteValues = pu.substring(1, pu.length() - 1).split(",");
                    bytes = new byte[byteValues.length];

                    for (int i = 0, len = bytes.length; i < len; i++) {
                        bytes[i] = Byte.parseByte(byteValues[i].trim());
                    }

                    // Convert the public key bytes into a PublicKey object
                    X509EncodedKeySpec x509KeySpec2 = new X509EncodedKeySpec(bytes);
                    keyFact = KeyFactory.getInstance("DH");
                    PublicKey publicKey = keyFact.generatePublic(x509KeySpec2);

                    // Prepare to generate the secret key with the private key and public key of the other party
                    KeyAgreement ka = KeyAgreement.getInstance("DH");
                    ka.init(privateKey);
                    ka.doPhase(publicKey, true);

            //convert obj to byte
            byte[] secret = ka.generateSecret();
            // === Generates an AES key ===

            // you should really use a Key Derivation Function instead, but this is
            // rather safe
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] bkey = Arrays.copyOf(
                    sha256.digest(secret), 128 / Byte.SIZE);

             sharedKey = new SecretKeySpec(bkey, "AES");

            String encodedKey = Base64.encodeToString(sharedKey.getEncoded(), Base64.DEFAULT);
                    MyJson.put("SharedKey", encodedKey);
                    Log.d("SharedKey in r: ", encodedKey);

                    pre.saveString(room, MyJson.toString());


               }



        }
        catch (org.json.JSONException e){
            Log.d("catch in recivek: ", e.getMessage());
            String m= decrypt(room ,message);
            //Log.d("m in decrypt: ", m);
            return m;

        }
        catch (Exception e){


        }
        String js=sentJoson.toString();
        if(js != null)
        Log.d("sentJson 217: ", js );
        else
            Log.d("sentJson 217:  ", "is null" );
        return sentJoson.toString();

    }

    private String initiatePG(String room, String msg){
        try {
            String intid= room.substring(0,room.indexOf('_'));
            Log.d("InitId: ",intid);

            int bitLength = 1024;
            SecureRandom rnd = new SecureRandom();
            p = BigInteger.probablePrime(bitLength, rnd);
            g = BigInteger.probablePrime(bitLength, rnd);

            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
            DHParameterSpec dhSpec = new DHParameterSpec(p, g);
            keyGen.initialize(dhSpec);
            KeyPair keypair = keyGen.generateKeyPair();

            // Get the generated public and private keys
            PrivateKey privateKey = keypair.getPrivate();
            PublicKey publicKey = keypair.getPublic();
            byte[] publicKeyBytes = publicKey.getEncoded();
            String sentPublicKey = Arrays.toString(publicKeyBytes);
            String myPrivateString = Arrays.toString(privateKey.getEncoded());
            String sharedKeyString= null;


            // JSON object to be sent to the other party
            sentJoson = new JSONObject();
            sentJoson.put("p", p.toString());
            sentJoson.put("g", g.toString());
            sentJoson.put("PublicKey", sentPublicKey);
            sentJoson.put("init", intid);

            // JSON Object to be stored in my device
            MyJson = new JSONObject();
            MyJson.put("p", p.toString());
            MyJson.put("g", g.toString());
            //MyJson.put("SharedKey", sharedKeyString);
            MyJson.put("PrivateKey", myPrivateString);
            MyJson.put("messages", msg);
            MyJson.put("init", intid);

            SharedPrefUtil pre = new SharedPrefUtil(context);

            String jsonString = MyJson.toString();
            pre.saveString(room, jsonString);
        }
        catch (Exception e){

        }

        return sentJoson.toString();

    }


    public String encrypt(String room, String msg){

        pre = new SharedPrefUtil(context);
        //boolean roomDefined = pre.contains("room");
        //if(roomDefined) {

            String jsonString = pre.getString(room);
            try {
                JSONObject json = new JSONObject(jsonString);
                String sharedKeyString = json.getString("SharedKey");

                if(json.has("messages")) {
                    msg = json.getString("messages") + "\n" + msg;
                    json.remove("messages");
                    pre.saveString(room, json.toString());

                }

                byte[] encodedKey = Base64.decode(sharedKeyString, Base64.DEFAULT);
                SecretKey secretKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");

                Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
                c.init(Cipher.ENCRYPT_MODE, secretKey);

                byte[] ciphertext = c.doFinal(msg.getBytes());


                return Arrays.toString(ciphertext);
            } catch (Exception e) {
                Log.d("catch in encrypt(): ", e.getMessage());

            }
        //}
        return null;
    }

    public String decrypt(String room,String msg) {

        pre = new SharedPrefUtil(context);
        //boolean roomDefined = pre.contains("room");
        //if(roomDefined) {


            String jsonString = pre.getString(room);
            try {
                JSONObject json = new JSONObject(jsonString);
                String sharedKeyString = json.getString("SharedKey");

                byte[] encodedKey = Base64.decode(sharedKeyString, Base64.DEFAULT);
                SecretKey secretKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");

                Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");

                String[] byteValues = msg.substring(1, msg.length() - 1).split(",");
                byte[] bytes = new byte[byteValues.length];

                for (int i = 0, len = bytes.length; i < len; i++) {
                    bytes[i] = Byte.parseByte(byteValues[i].trim());
                }


                // inits the encryptionMode
                c.init(Cipher.DECRYPT_MODE, secretKey);

                return new String(c.doFinal(bytes), "utf-8");

            } catch (Exception e) {
                Log.d("catch in decrypt(): ", e.getMessage());
            }
        //}
        return null;
    }
}