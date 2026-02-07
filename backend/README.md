# DadScape Diary API

Backend API for the DadScape clan achievement diary system.

## Features

- RESTful API for clan diary management
- SQLite database for persistent storage
- TypeScript for type safety
- Rate limiting and security headers
- CORS support for RuneLite plugin integration

## Installation

```bash
cd backend
npm install
```

## Configuration

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
```

Edit `.env` to set your configuration:

```env
PORT=3000
DATABASE_PATH=./data/diaries.db
NODE_ENV=development
```

## Database Setup

Run migrations to create the database schema:

```bash
npm run migrate
```

This creates:
- `diaries` table - stores clan achievement diaries
- `clan_members` table - tracks clan member info
- `user_progress` table - tracks task completion per user
- `api_keys` table - for API authentication (future use)

## Development

Start the development server with auto-reload:

```bash
npm run dev
```

## Production

Build and run in production:

```bash
npm run build
npm start
```

## API Endpoints

### Health Check
```
GET /health
```

### Diaries

**Get all diaries**
```
GET /api/diaries
Query params: ?category=PvM&active=true
```

**Get diary by ID**
```
GET /api/diaries/:id
```

**Create diary**
```
POST /api/diaries
Body: {
  "name": "DadScape Achievement Diary",
  "category": "PvM",
  "createdBy": "PlayerName",
  "description": "Optional description"
}
```

**Update diary**
```
PUT /api/diaries/:id
Body: {
  "name": "Updated Name",
  "tiers": [...],
  "lastModifiedBy": "PlayerName"
}
```

**Delete diary**
```
DELETE /api/diaries/:id
```

**Get categories**
```
GET /api/diaries/categories
```

## Data Models

### ClanDiary
```typescript
{
  id: string;              // UUID
  name: string;
  description: string;
  category: string;
  version: string;
  createdDate: number;     // Unix timestamp
  createdBy: string;       // RSN
  lastModified: number;
  lastModifiedBy: string;
  tiers: DiaryTier[];
  active: boolean;
}
```

### DiaryTier
```typescript
{
  tierName: string;        // "Easy", "Medium", "Hard", "Elite"
  tierColor: string;       // Hex color
  tasks: DiaryTask[];
  rewardDescription: string;
  order: number;
}
```

### DiaryTask
```typescript
{
  id: string;              // UUID
  description: string;
  type: TaskType;          // KILL, SKILL, QUEST, etc.
  requirements: Record<string, string>;
  hint: string;
  order: number;
}
```

## Testing

Test the API with curl:

```bash
# Health check
curl http://localhost:3000/health

# Get all diaries
curl http://localhost:3000/api/diaries

# Create a diary
curl -X POST http://localhost:3000/api/diaries \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Diary",
    "category": "PvM",
    "createdBy": "TestPlayer"
  }'
```

## Project Structure

```
backend/
├── src/
│   ├── controllers/     # Request handlers
│   ├── db/             # Database connection & migrations
│   ├── middleware/     # Express middleware
│   ├── models/         # TypeScript type definitions
│   ├── routes/         # API route definitions
│   └── server.ts       # Main application entry point
├── data/               # SQLite database files (created on first run)
├── dist/               # Compiled JavaScript (after build)
├── .env                # Environment variables (create from .env.example)
├── package.json
└── tsconfig.json
```

## Integration with RuneLite Plugin

The RuneLite plugin should:

1. Set the API endpoint in the config (`http://localhost:3000/api/diaries`)
2. Poll for diary updates periodically
3. POST new diaries created by officers
4. PUT updates when diaries are modified

Example Java code:
```java
// In ApiService.java
String apiUrl = config.apiEndpoint() + "/diaries";
// Use OkHttpClient to make requests
```

## License

MIT
