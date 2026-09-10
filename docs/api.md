# REST API Specifications

All endpoints prefix: `/api/v1`

## Standard Response Format
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... },
  "timestamp": "2026-09-10T14:30:00Z"
}
```

## Standard Error Format
```json
{
  "timestamp": "2026-09-10T14:30:00Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Invalid input provided",
  "path": "/api/v1/settings/profile",
  "fieldErrors": {
    "gstin": "Invalid GSTIN format. Must match 15-character alphanumeric pattern."
  }
}
```
