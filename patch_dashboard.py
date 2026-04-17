with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardScreen.kt", "r") as f:
    content = f.read()

import_line = "import com.azuratech.azuratime.ui.core.designsystem.AzuraCard\n"
if "AzuraCard" not in content:
    content = content.replace("import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen\n", "import com.azuratech.azuratime.ui.core.designsystem.AzuraScreen\n" + import_line)

pending_card = """
            if (!data.isApproved) {
                item {
                    AzuraCard(
                        title = "Menunggu Persetujuan",
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = "Akun Anda menunggu verifikasi admin untuk mengakses fitur Absensi.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (data.isApproved) {"""

content = content.replace("            if (data.isApproved) {", pending_card)

with open("app/src/main/java/com/azuratech/azuratime/ui/dashboard/DashboardScreen.kt", "w") as f:
    f.write(content)
