# Backend API Testing Guide

## Prerequisites

1. Run database migrations:
```bash
cd backend
npm run migrate
```

2. Create an API key and test users:
```bash
npm run seed-test-data
```

Or manually insert test data using the SQL commands below.

## Manual Test Data Setup

### 1. Create an API Key

```sql
INSERT INTO api_keys (key, description, created_by, active)
VALUES ('test-api-key-12345', 'Test API key for development', 'admin', 1);
```

### 2. Create Test Clan Members

```sql
-- Admin user (rank 100)
INSERT INTO clan_members (rsn, rank, joined_date, last_seen)
VALUES ('TestAdmin', 100, 1704067200000, 1704067200000);

-- Regular user (rank 10)
INSERT INTO clan_members (rsn, rank, joined_date, last_seen)
VALUES ('TestUser', 10, 1704067200000, 1704067200000);

-- Owner (rank 127)
INSERT INTO clan_members (rsn, rank, joined_date, last_seen)
VALUES ('ClanOwner', 127, 1704067200000, 1704067200000);
```

## API Testing with cURL

### Health Check (No Auth Required)

```bash
curl http://localhost:3000/health
```

### Get All Diaries (Auth Required)

```bash
curl -H "Authorization: Bearer test-api-key-12345" \
  http://localhost:3000/api/diaries
```

### Get Single Diary (Auth Required)

```bash
curl -H "Authorization: Bearer test-api-key-12345" \
  http://localhost:3000/api/diaries/{diary-id}
```

### Create Diary (Auth + Admin Required)

```bash
curl -X POST \
  -H "Authorization: Bearer test-api-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Diary",
    "category": "PvM",
    "createdBy": "TestAdmin",
    "rsn": "TestAdmin",
    "description": "A test diary for API validation"
  }' \
  http://localhost:3000/api/diaries
```

### Update Diary (Auth + Admin Required)

```bash
curl -X PUT \
  -H "Authorization: Bearer test-api-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Test Diary",
    "lastModifiedBy": "TestAdmin",
    "rsn": "TestAdmin"
  }' \
  http://localhost:3000/api/diaries/{diary-id}
```

### Delete Diary (Auth + Admin Required)

```bash
curl -X DELETE \
  -H "Authorization: Bearer test-api-key-12345" \
  "http://localhost:3000/api/diaries/{diary-id}?rsn=TestAdmin"
```

### Get MOTD (Auth Required)

```bash
curl -H "Authorization: Bearer test-api-key-12345" \
  http://localhost:3000/api/motd
```

### Set MOTD (Auth + Admin Required)

```bash
curl -X POST \
  -H "Authorization: Bearer test-api-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "motd": "Welcome to DadScape! Check out our new diary challenges!",
    "rsn": "TestAdmin"
  }' \
  http://localhost:3000/api/motd
```

## Testing Authorization

### Test Failed Authentication (Invalid API Key)

```bash
curl -H "Authorization: Bearer invalid-key" \
  http://localhost:3000/api/diaries
```

Expected response: `401 Unauthorized`

### Test Failed Authorization (Non-Admin User)

```bash
curl -X POST \
  -H "Authorization: Bearer test-api-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Unauthorized Diary",
    "category": "PvM",
    "createdBy": "TestUser",
    "rsn": "TestUser"
  }' \
  http://localhost:3000/api/diaries
```

Expected response: `403 Forbidden` (TestUser has rank 10, needs rank 100+)

### Test Missing RSN

```bash
curl -X POST \
  -H "Authorization: Bearer test-api-key-12345" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Diary",
    "category": "PvM",
    "createdBy": "TestAdmin"
  }' \
  http://localhost:3000/api/diaries
```

Expected response: `400 Bad Request` (missing rsn field)

## Environment Variables

Create a `.env` file in the backend directory:

```env
# Server
PORT=3000
NODE_ENV=development

# Database
DATABASE_PATH=./data/diaries.db

# CORS
CORS_ORIGIN=*

# Rate Limiting
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=100

# Authorization
MIN_ADMIN_RANK=0
```

## Rank Values Reference

- GUEST = -1
- FRIEND = 0
- RECRUIT = 10
- CORPORAL = 20
- SERGEANT = 30
- LIEUTENANT = 40
- CAPTAIN = 50
- GENERAL = 60
- ADMIN = 100
- DEPUTY_OWNER = 125
- OWNER = 127

Set `MIN_ADMIN_RANK` in `.env` to control who can create/edit/delete diaries.

## Expected Success Responses

All successful responses follow this format:

```json
{
  "success": true,
  "data": { ... },
  "message": "Optional success message"
}
```

## Expected Error Responses

All error responses follow this format:

```json
{
  "success": false,
  "error": "Error message describing what went wrong"
}
```

## Testing with Postman

1. Import the following as a collection
2. Set environment variable `API_KEY` = `test-api-key-12345`
3. Set environment variable `BASE_URL` = `http://localhost:3000`
4. Add header `Authorization: Bearer {{API_KEY}}` to all authenticated requests

## Phase 1 Completion Checklist

- [x] Authentication middleware validates API keys
- [x] Authorization middleware checks clan rank
- [x] GET endpoints require authentication only
- [x] POST/PUT/DELETE endpoints require authentication + admin rank
- [x] MOTD endpoints created with proper permissions
- [x] Error responses are clear and informative
- [ ] Manual testing completed successfully
- [ ] Ready for Phase 2 (Java API client implementation)
