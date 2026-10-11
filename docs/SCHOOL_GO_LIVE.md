# School Go-Live Control Record

This document is the release authority record for real ATU operations. It is
completed for the pilot and every production launch. Technical readiness alone
does not authorise student data or payment processing.

## Named Owners

| Responsibility | Named person / office | Contact | Approved date |
|---|---|---|---|
| Service owner (school) |  |  |  |
| Technical owner |  |  |  |
| Finance and refunds owner |  |  |  |
| Data-protection owner |  |  |  |
| Vendor operations owner |  |  |  |
| First-line support owner |  |  |  |
| Incident commander |  |  |  |

## Launch Approval

- [ ] Production hostname, TLS certificate, and uptime monitor are active.
- [ ] Staging uses separate data and has passed `docs/UAT_CHECKLIST.md`.
- [ ] `make check` passes in CI for the release commit.
- [ ] The signed Android release has been tested on representative real devices.
- [ ] Paystack test-mode checkout, duplicate webhook handling, refund, and
      reconciliation have passed; live credentials are held only in secrets.
- [ ] Database backup is encrypted/off-host; `scripts/verify_backup.sh` passes.
- [ ] A restore drill into a dedicated drill database has succeeded this month.
- [ ] Privacy Policy, Terms, retention schedule, and student support wording
      have been approved by the school authority.
- [ ] Vendor staff, finance staff, and support staff have completed training.
- [ ] Rollback owner and communication channel are named below.

## Operating Targets

Record the agreed baseline before launch. Review each month.

| Signal | Target | Alert when | Owner |
|---|---:|---:|---|
| API health availability | 99.5% monthly | health check fails for 2 minutes | Technical owner |
| API server errors | < 1% of requests | 5xx rate >= 2% for 5 minutes | Technical owner |
| Checkout completion | >= 98% | payment failures exceed normal baseline | Finance owner |
| Crash-free sessions | >= 99% | Crashlytics regression after release | Technical owner |
| Backup restore drill | monthly | missed or failed | Technical owner |
| Support first response | school-defined | target missed | Support owner |

## Incident Response

| Severity | Examples | First response | Escalate / communicate |
|---|---|---|---|
| S1 | suspected data exposure, duplicate charges, total service outage | Stop affected operation, preserve logs, notify incident commander immediately | School management, technical, finance, data protection |
| S2 | payment failures, vendor-wide outage, serious incorrect order state | Acknowledge within 15 minutes; use rollback or disable affected workflow | Technical and operations owners |
| S3 | individual user or vendor issue | Create support ticket and resolve in normal support window | Support owner |

For every S1/S2 event, record timeline, affected users, financial correction,
root cause, and preventive action. Do not delete audit logs or overwrite
financial records to hide an incident.

## Rollback Record

- Previous release/version: ____________________
- Rollback operator: ____________________
- Customer/vendor communication channel: ____________________
- Database migration rollback assessed: yes / no / not applicable
- Post-rollback health and payment reconciliation completed: yes / no
