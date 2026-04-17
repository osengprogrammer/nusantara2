import os

# Target directory and output file paths
DB_DIR = "/home/max/Desktop/New folder/New folder2/azura-main/app/src/main/java/com/azuratech/azuratime/db"
OUTPUT_FILE = os.path.join(DB_DIR, "db_summary.md")

def generate_summary():
    # Double-check if the directory exists
    if not os.path.exists(DB_DIR):
        print(f"❌ Error: Could not find directory at:\n{DB_DIR}")
        return

    print(f"⏳ Generating summary at:\n{OUTPUT_FILE}...\n")
    
    # Open the output file in write mode
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as outfile:
        # Write the main header
        outfile.write("# DB Layer — Full Source Summary\n\n")
        outfile.write("Generated from all .kt files in the `db/` package.\n\n")
        
        # Get all files and sort them alphabetically so the markdown is organized
        files = sorted(os.listdir(DB_DIR))
        count = 0
        
        for filename in files:
            # Only process Kotlin files
            if filename.endswith(".kt"):
                filepath = os.path.join(DB_DIR, filename)
                
                # Write the markdown header for the file and open the code block
                outfile.write(f"## {filename}\n\n")
                outfile.write("```kotlin\n")
                
                # Read the Kotlin file and write its contents
                with open(filepath, 'r', encoding='utf-8') as infile:
                    outfile.write(infile.read())
                    
                # Close the code block
                outfile.write("\n```\n\n")
                print(f"✅ Added {filename}")
                count += 1

    print(f"\n🎉 Done! Successfully bundled {count} files into db_summary.md.")

if __name__ == "__main__":
    generate_summary()
