import os
import subprocess
import sys
import tarfile
import tempfile

def check_adb():
    try:
        result = subprocess.run(['adb', 'devices'], capture_output=True, text=True)
        if "device\n" not in result.stdout and "device\r\n" not in result.stdout:
            print("Error: No Android device or emulator found. Please start your emulator or connect a device via USB.")
            return False
        return True
    except FileNotFoundError:
        print("Error: 'adb' command not found. Please ensure Android SDK Platform-Tools are installed and in your PATH.")
        return False

def sideload():
    print("=== Recall AI Model Sideloading Tool ===")
    print("This script will help you push the Gemma LLM model to your device.")
    print("1. Please download the 'gemma-2b-it-cpu-int4' model from Kaggle:")
    print("   https://www.kaggle.com/models/google/gemma/tfLite")
    print("2. Enter the full path to the downloaded file (.tar.gz or .bin) below:")
    
    file_path = input("> ").strip().strip('"').strip("'")
    
    if not os.path.exists(file_path):
        print(f"Error: File not found at '{file_path}'")
        return
        
    if not check_adb():
        return
        
    actual_file_to_push = file_path
    temp_dir_obj = None
    
    if file_path.endswith('.tar.gz') or file_path.endswith('.tgz'):
        print("\nDetected an archive. Extracting...")
        temp_dir_obj = tempfile.TemporaryDirectory()
        extract_path = temp_dir_obj.name
        try:
            with tarfile.open(file_path, "r:gz") as tar:
                tar.extractall(path=extract_path)
                
            # Find the .bin or .task file
            found = False
            for root, dirs, files in os.walk(extract_path):
                for file in files:
                    if file.endswith('.bin') or file.endswith('.task'):
                        actual_file_to_push = os.path.join(root, file)
                        print(f"Found model file: {file}")
                        found = True
                        break
                if found:
                    break
                    
            if not found:
                print("Error: Could not find a .bin or .task model file inside the archive.")
                return
        except Exception as e:
            print(f"Error extracting archive: {e}")
            return
            
    dest_path = "/storage/emulated/0/Android/data/com.recall.app/files/Download/llm_model.task"
    print(f"\nPushing model to {dest_path}...")
    print("This may take a few minutes for a 1.5GB file. Please wait...")
    
    try:
        subprocess.run(['adb', 'push', actual_file_to_push, dest_path], check=True)
        print("\nSuccess! The model has been sideloaded.")
        print("You can now launch the Recall app and use the AI features.")
    except subprocess.CalledProcessError:
        print("\nError: Failed to push the file to the device. Please check your connection.")
    finally:
        if temp_dir_obj:
            temp_dir_obj.cleanup()

if __name__ == '__main__':
    sideload()
