import os

# Target directory and output file paths
REPO_DIR = "/home/max/Desktop/New folder/New folder2/azura-main/app/src/main/java/com/azuratech/azuratime/repository"
OUTPUT_FILE = os.path.join(REPO_DIR, "repository_summary.md")

def generate_summary():
    # Double-check if the directory exists
    if not os.path.exists(REPO_DIR):
        print(f"❌ Error: Could not find directory at:\n{REPO_DIR}")
        return

    print(f"⏳ Generating summary at:\n{OUTPUT_FILE}...\n")
    
    # Open the output file in write mode
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as outfile:
        # Write the main header
        outfile.write("# Repository Layer — Full Source Summary\n\n")
        outfile.write("Generated from all .kt files in the `repository/` package.\n\n")
        
        # Get all files and sort them alphabetically so the markdown is organized
        files = sorted(os.listdir(REPO_DIR))
        count = 0
        
        for filename in files:
            # Only process Kotlin files
            if filename.endswith(".kt"):
                filepath = os.path.join(REPO_DIR, filename)
                
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

    print(f"\n🎉 Done! Successfully bundled {count} files into repository_summary.md.")

if __name__ == "__main__":
    generate_summary()
