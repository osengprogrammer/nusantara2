import os
import glob

old_pkg = "com.azuratech.azuratime.data.repository"
new_pkg = "com.azuratech.azuratime.data.repo"

# Rename directory
os.makedirs("app/src/main/java/com/azuratech/azuratime/data/repo", exist_ok=True)
files = glob.glob("app/src/main/java/com/azuratech/azuratime/data/repository/*.kt")
for file in files:
    new_file = file.replace("/repository/", "/repo/")
    os.rename(file, new_file)

os.rmdir("app/src/main/java/com/azuratech/azuratime/data/repository")

# Update all Kotlin files
all_kt = glob.glob("app/src/**/*.kt", recursive=True)
for file in all_kt:
    with open(file, "r") as f: content = f.read()
    
    if old_pkg in content:
        content = content.replace(old_pkg, new_pkg)
        with open(file, "w") as f: f.write(content)

