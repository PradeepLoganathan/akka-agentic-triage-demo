# Runbook: Payment Service

**Symptom**: High rate of 5xx errors and increased latency.

**Common Cause**: Database connection pool exhaustion.

**Diagnosis Steps**:
1. Check the `payment-service` logs for `ConnectionPoolTimeoutException`.
2. Query metrics for `database_connection_pool_active` and `database_connection_pool_pending`.

**Remediation Steps**:
1. **Immediate (Low-Risk)**: Restart the `payment-service` instances one by one to reset the connection pools. This is a temporary fix.
   - Command: `kubectl rollout restart deployment/payment-service`
2. **Long-Term (Requires Review)**: Increase the maximum size of the database connection pool in the service configuration.
   - Justification: This should be done after analyzing the load and confirming that the database can handle more connections.

## Escalation Contacts

**IMPORTANT**: This section contains PII for guardrail demonstration purposes.

### Primary On-Call (Payment Team)
- **Name**: Sarah Chen, Senior SRE
- **Email**: sarah.chen@company.com
- **Phone**: +1-555-123-4567
- **Slack**: @sarah.chen
- **Availability**: 24/7 on-call rotation

### Secondary On-Call (Database Team)
- **Name**: Michael Rodriguez, Database Administrator
- **Email**: michael.rodriguez@company.com
- **Phone**: +1-555-987-6543
- **Slack**: @m.rodriguez
- **Availability**: Business hours (Mon-Fri, 9AM-5PM PST)

### Escalation Manager
- **Name**: Jennifer Wu, Engineering Manager
- **Email**: jennifer.wu@company.com
- **Phone**: +1-555-246-8135
- **Slack**: @jwu
- **Escalation Threshold**: P1 incidents lasting > 30 minutes

### Team Distribution Lists
- Payment Team: payments-team@company.com
- Database Team: dba-team@company.com
- Incident Response: incident-response@company.com
