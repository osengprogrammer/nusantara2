import os

# Target directory and output file paths
VM_DIR = "/home/max/Desktop/New folder/New folder2/azura-main/app/src/main/java/com/azuratech/azuratime/viewmodel"
OUTPUT_FILE = os.path.join(VM_DIR, "viewmodel_summary.md")

def generate_summary():
    if not os.path.exists(VM_DIR):
        print(f"❌ Error: Could not find directory at:\n{VM_DIR}")
        return

    print(f"⏳ Generating summary at:\n{OUTPUT_FILE}...\n")
    
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as outfile:
        outfile.write("# ViewModel Layer — Full Source Summary\n\n")
        outfile.write("Generated from all .kt files in the `viewmodel/` package.\n\n")
        
        files = sorted(os.listdir(VM_DIR))
        count = 0
        
        for filename in files:
            if filename.endswith(".kt"):
                filepath = os.path.join(VM_DIR, filename)
                
                outfile.write(f"## {filename}\n\n")
                outfile.write("```kotlin\n")
                
                with open(filepath, 'r', encoding='utf-8') as infile:
                    outfile.write(infile.read())
                    
                outfile.write("\n```\n\n")
                print(f"✅ Added {filename}")
                count += 1

    print(f"\n🎉 Done! Successfully bundled {count} files into viewmodel_summary.md.")

if __name__ == "__main__":
    generate_summary()
