import os

# Paths
UI_DIR = "/home/max/Desktop/New folder/New folder2/azura-main/app/src/main/java/com/azuratech/azuratime/ui"
OUTPUT_BASE_DIR = "/home/max/Desktop/New folder/New folder2/azura-main"

def generate_modular_summaries():
    if not os.path.exists(UI_DIR):
        print(f"❌ Error: Could not find directory at:\n{UI_DIR}")
        return

    print("⏳ Generating modular summaries...\n")
    
    # 1. Handle files sitting directly in the 'ui' root folder (like MainScreen.kt)
    root_files_content = "# UI Layer — Root Folder Summary\n\n"
    has_root_files = False
    
    for item in os.listdir(UI_DIR):
        item_path = os.path.join(UI_DIR, item)
        
        # If it's a file in the root
        if os.path.isfile(item_path) and item.endswith(".kt"):
            has_root_files = True
            root_files_content += f"## {item}\n\n```kotlin\n"
            with open(item_path, 'r', encoding='utf-8') as f:
                root_files_content += f.read()
            root_files_content += "\n```\n\n"
            
        # 2. Handle subfolders (admin, dashboard, workspace, etc.)
        elif os.path.isdir(item_path):
            folder_name = item
            output_filename = os.path.join(OUTPUT_BASE_DIR, f"ui_{folder_name}_summary.md")
            
            has_files = False
            content = f"# UI Layer — `{folder_name}` Folder Summary\n\n"
            count = 0
            
            for root, dirs, files in os.walk(item_path):
                for filename in files:
                    if filename.endswith(".kt"):
                        has_files = True
                        count += 1
                        filepath = os.path.join(root, filename)
                        rel_path = os.path.relpath(filepath, item_path)
                        
                        content += f"## {rel_path}\n\n```kotlin\n"
                        with open(filepath, 'r', encoding='utf-8') as f:
                            content += f.read()
                        content += "\n```\n\n"
            
            if has_files:
                with open(output_filename, 'w', encoding='utf-8') as out:
                    out.write(content)
                print(f"✅ Created: ui_{folder_name}_summary.md ({count} files)")

    # Save root files if any exist
    if has_root_files:
        root_output = os.path.join(OUTPUT_BASE_DIR, "ui_root_summary.md")
        with open(root_output, 'w', encoding='utf-8') as out:
            out.write(root_files_content)
        print(f"✅ Created: ui_root_summary.md")

    print("\n🎉 Done! Your UI layer is now cleanly separated by folder.")

if __name__ == "__main__":
    generate_modular_summaries()
