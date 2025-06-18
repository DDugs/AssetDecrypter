# AssetDecrypter
## 🔐 APK Asset Folder Encryption & Decryption Framework in Go and Java
Developed a custom security mechanism that ensures the sensitive contents of an Android APK's assets/ folder are encrypted at rest and only decrypted at runtime, using a combination of Go for encryption tooling and Java for runtime decryption. This hybrid approach tightly integrates encryption into the Android app lifecycle and introduces runtime protection through smart injection.

# 📦 Workflow Overview
1) Decompile the APK
- ✅ Using apktool, the target APK is decompiled to expose its resources and Smali code structure. This allows modifications to internal files like assets/ and integration of custom Smali routines.

2) Encrypt Assets via Go
- ✅ A custom-built Go tool performs AES-GCM encryption on all files in the assets/ directory. This encryption mechanism:

- ✅ Splits a 256-bit AES key into four equal parts.

- ✅ Inserts key parts around the payload: [key1][key2][nonce][ciphertext][key3][key4].

- ✅ Recursively encrypts all files within assets/, ensuring the contents are unintelligible without the decryption routine.

3) Inject Java Runtime Decryption
- ✅ The decryption logic is written in Java and:

- ✅ Extracts the key parts and nonce from the encrypted file.

- ✅ Reconstructs the AES key.

- ✅ Decrypts content using AES-GCM.

This Java class is compiled into .class, converted to .dex, and transformed into .smali, which is then injected into the APK’s Smali code. The decryption function is triggered at runtime each time the app is launched.

4) Re-encrypt on App Exit
- ✅ A clever lifecycle hook ensures that when the app is cleared from memory (e.g., by the system or user), the decrypted assets are encrypted again, maintaining secure state at all times.

## 🔄 Lifecycle Flow
1) App Launch:

-- Runtime decryption code activates.

-- Encrypted assets are decrypted and made usable by the app.

2) App Exit/Clear:

The assets folder is re-encrypted using the original logic to restore protection.

## ✅ Benefits of This Approach
1) 🔒 Strong Data Protection at Rest

-- Sensitive resources (config files, media, proprietary assets, etc.) remain encrypted within the APK, even after extraction or reverse engineering attempts.

2) 🛡️ Obfuscation Through Fragmented Keys

-- Splitting the AES key into four parts and embedding them in a non-obvious structure makes static key extraction very difficult.

3) 💣 Resistance to Static Analysis

-- Encrypted assets ensure that reverse engineers or attackers inspecting the APK cannot access raw asset data without executing the app in real runtime.

4) 📱 Runtime Decryption with Controlled Execution

-- The runtime decryptor ensures assets are only decrypted temporarily and in memory, reducing the attack surface.

5) ♻️ Automatic Re-encryption

-- Ensures sensitive data isn’t left exposed on the device filesystem if the app is force-stopped, minimizing forensics risk.

6) 🧠 Smali Injection for Stealth

-- Injecting decryption logic into Smali gives seamless integration without needing to modify original Java source code, which is useful for post-build security patching or asset protection in third-party APKs.
