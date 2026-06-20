# 🚀 AzuraTime Roadmap (v3.5.0+)
*The Path to Enterprise Gold Standard*

This document outlines the strategic future features for AzuraTime, focusing on data transparency, proactiveness, and high-security biometric validation.

---

## 📊 Phase 34: Visual Insights & Analytics
*Objective: Transform raw data into actionable decision-making tools for Admins.*

- **Dashboard Charts:** Implementation of native charts (Jetpack Compose) to show:
    - Today's Attendance Distribution (Hadir, Izin, Alpa).
    - Weekly Attendance Trends.
    - Class-specific performance metrics.
- **Top Delinquency Report:** Quick view of students with high absence rates.

## 📜 Phase 35: Enterprise Audit Logging
*Objective: Ensure 100% accountability and prevent data manipulation.*

- **Action Tracking:** Every manual change to an attendance record (status override, time change) must be logged.
- **Admin Audit UI:** A dedicated screen for Super Admins to review "Who did what and when."
- **Immutable Records:** Transitioning core attendance logs to a semi-immutable state where only authorized overrides are permitted.

## 🔔 Phase 36: Smart Proactive Notifications
*Objective: Create a living ecosystem that bridges Schools, Supervisors, and Parents.*

- **Parent Alerts:** Real-time push notifications to `azura-parent` when a student arrives or is flagged as "Late/Absent."
- **Supervisor Reminders:** "Class Starts in 5 Minutes" alerts based on the Matrix Schedule.
- **Weekly Summary:** Automated weekly PDF/Notification reports sent to School Admins.

## 🛡️ Phase 37: Biometric Hardening (Anti-Spoofing 2.0)
*Objective: Reach military-grade security for face recognition.*

- **Random Challenge Response:** Moving beyond simple "Blink Detection" to random prompts (e.g., "Look Left", "Smile", "Nod").
- **Depth Estimation:** (Experimental) Utilizing multiple frame analysis to detect 2D surfaces (photos/screens) vs 3D human faces.
- **Model Encryption:** Hardening the `.azr` model assets against extraction from the device filesystem.

---

## 🛠️ Infrastructure & Tech Debt
- **Modularization:** Splitting `app` module into dynamic features (`:feature:attendance`, `:feature:account`, `:feature:reporting`).
- **KMP Expansion:** Moving more business logic from Android-specific code to the `azura-engine-kmp` for future iOS compatibility.
- **Unit Test Coverage:** Aiming for >80% coverage on all UseCase and ViewModel classes.

---
*Roadmap owner: AzuraTech Core Team*
