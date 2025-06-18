package com.example.runtime;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class DecryptAssets {
    private static final String TAG = "DecryptAssets";
    private static final int KEY_SIZE = 32;
    private static final int NONCE_SIZE = 12;
    private static final int KEY_PARTS = 4;
    private static final int PART_SIZE = KEY_SIZE / KEY_PARTS;

    public static void run(Context context) {
        File assetsDir = new File(context.getFilesDir(), "assets"); // or adjust as needed
        decryptFilesRecursive(assetsDir);
        Log.i(TAG, "✅ Decryption complete");
    }

    private static void decryptFilesRecursive(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                decryptFilesRecursive(file);
            } else {
                try {
                    decryptFile(file);
                    Log.i(TAG, "🔓 Decrypted: " + file.getName());
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to decrypt " + file.getName(), e);
                }
            }
        }
    }

    private static void decryptFile(File file) throws Exception {
        byte[] fileBytes = readFile(file);

        if (fileBytes.length < KEY_SIZE + NONCE_SIZE) {
            throw new IllegalArgumentException("File too small");
        }

        byte[] keyPart1 = Arrays.copyOfRange(fileBytes, 0, PART_SIZE);
        byte[] keyPart2 = Arrays.copyOfRange(fileBytes, PART_SIZE, PART_SIZE * 2);
        byte[] keyPart3 = Arrays.copyOfRange(fileBytes, fileBytes.length - PART_SIZE * 2, fileBytes.length - PART_SIZE);
        byte[] keyPart4 = Arrays.copyOfRange(fileBytes, fileBytes.length - PART_SIZE, fileBytes.length);
        byte[] key = concatenate(keyPart1, keyPart2, keyPart3, keyPart4);

        byte[] nonce = Arrays.copyOfRange(fileBytes, PART_SIZE * 2, PART_SIZE * 2 + NONCE_SIZE);
        byte[] ciphertext = Arrays.copyOfRange(fileBytes, PART_SIZE * 2 + NONCE_SIZE, fileBytes.length - PART_SIZE * 2);

        byte[] plaintext = decryptAESGCM(key, nonce, ciphertext);
        writeFile(file, plaintext);
    }

    private static byte[] decryptAESGCM(byte[] key, byte[] nonce, byte[] ciphertext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(128, nonce);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        return cipher.doFinal(ciphertext);
    }

    private static byte[] readFile(File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        int read = fis.read(data);
        fis.close();
        return data;
    }

    private static void writeFile(File file, byte[] data) throws Exception {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
    }

    private static byte[] concatenate(byte[]... parts) {
        int totalLength = 0;
        for (byte[] part : parts) totalLength += part.length;

        byte[] result = new byte[totalLength];
        int pos = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, pos, part.length);
            pos += part.length;
        }
        return result;
    }
}
