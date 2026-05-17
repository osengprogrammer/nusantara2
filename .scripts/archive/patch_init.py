with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardViewModel.kt", "r") as f:
    content = f.read()

# remove init block
import re
init_block_regex = re.compile(r"    init \{[\s\S]*?    \}\n", re.MULTILINE)
init_block = init_block_regex.search(content).group(0)
content = content.replace(init_block, "")

# insert it after _sessionStudentsFlow
target = r"            } else {\n                flowOf(emptyList())\n            }\n        }"
content = content.replace(target, target + "\n\n" + init_block)

with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardViewModel.kt", "w") as f:
    f.write(content)
