package main

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"fmt"
	"io/fs"
	"io/ioutil"
	"os"
	"os/exec"
	"path/filepath"
)

const (
	AESKeySize = 32
	NonceSize  = 12
	NumParts   = 4
)

func generateRandomKey() ([]byte, error) {
	key := make([]byte, AESKeySize)
	_, err := rand.Read(key)
	return key, err
}

func splitKey(key []byte, numParts int) [][]byte {
	partLen := len(key) / numParts
	parts := make([][]byte, numParts)
	for i := 0; i < numParts; i++ {
		parts[i] = make([]byte, partLen)
		copy(parts[i], key[i*partLen:(i+1)*partLen])
	}
	return parts
}

func joinKey(parts [][]byte) []byte {
	var key []byte
	for _, part := range parts {
		key = append(key, part...)
	}
	return key
}

func encryptData(plaintext []byte, key []byte) ([]byte, error) {
	keyParts := splitKey(key, NumParts)

	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, err
	}

	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, err
	}

	nonce := make([]byte, NonceSize)
	if _, err := rand.Read(nonce); err != nil {
		return nil, err
	}

	ciphertext := gcm.Seal(nil, nonce, plaintext, nil)

	// Structure: [key1 + key2][nonce][ciphertext][key3 + key4]
	result := bytes.Buffer{}
	result.Write(keyParts[0])
	result.Write(keyParts[1])
	result.Write(nonce)
	result.Write(ciphertext)
	result.Write(keyParts[2])
	result.Write(keyParts[3])

	return result.Bytes(), nil
}

func runApktoolJar(apkPath, outputDir string) error {
	apktoolJar := "apktool_2.11.1.jar"
	cmd := exec.Command("java", "-jar", apktoolJar, "d", apkPath, "-o", outputDir, "-f")
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}

func encryptAssetsFolder(assetsPath string) error {
	return filepath.WalkDir(assetsPath, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if d.IsDir() {
			return nil
		}

		fmt.Println("Encrypting:", path)

		data, err := ioutil.ReadFile(path)
		if err != nil {
			return err
		}

		key, err := generateRandomKey()
		if err != nil {
			return err
		}

		encrypted, err := encryptData(data, key)
		if err != nil {
			return err
		}

		return ioutil.WriteFile(path, encrypted, 0644)
	})
}

func main() {
	apkPath := "app-debug.apk"     // 🔁 Replace with your actual APK file
	outputDir := "decompiled_apk" // Folder where APK will be decompiled

	fmt.Println("➡️  Decompiling APK using apktool.jar...")
	if err := runApktoolJar(apkPath, outputDir); err != nil {
		panic("Apktool decompilation failed: " + err.Error())
	}

	assetsPath := filepath.Join(outputDir, "assets")
	if _, err := os.Stat(assetsPath); os.IsNotExist(err) {
		panic("❌ assets folder not found in the APK.")
	}

	fmt.Println("🔐 Encrypting files inside assets/ ...")
	if err := encryptAssetsFolder(assetsPath); err != nil {
		panic("Encryption failed: " + err.Error())
	}

	fmt.Println("✅ Done. Encrypted all files inside:", assetsPath)
}
