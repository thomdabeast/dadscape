import { db, run } from './database';

/**
 * Database migration script
 * Creates all necessary tables for the clan diary system
 */

async function migrate() {
  console.log('Starting database migration...');

  try {
    // Create diaries table
    await run(`
      CREATE TABLE IF NOT EXISTS diaries (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        description TEXT,
        category TEXT NOT NULL,
        version TEXT NOT NULL,
        created_date INTEGER NOT NULL,
        created_by TEXT NOT NULL,
        last_modified INTEGER NOT NULL,
        last_modified_by TEXT NOT NULL,
        active INTEGER NOT NULL DEFAULT 1,
        tiers_json TEXT NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('✓ Created diaries table');

    // Create indices for better query performance
    await run(`
      CREATE INDEX IF NOT EXISTS idx_diaries_category
      ON diaries(category)
    `);
    console.log('✓ Created category index');

    await run(`
      CREATE INDEX IF NOT EXISTS idx_diaries_active
      ON diaries(active)
    `);
    console.log('✓ Created active index');

    await run(`
      CREATE INDEX IF NOT EXISTS idx_diaries_created_by
      ON diaries(created_by)
    `);
    console.log('✓ Created created_by index');

    // Create clan_members table for tracking who has access
    await run(`
      CREATE TABLE IF NOT EXISTS clan_members (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        rsn TEXT NOT NULL UNIQUE,
        rank INTEGER NOT NULL,
        joined_date INTEGER NOT NULL,
        last_seen INTEGER NOT NULL,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('✓ Created clan_members table');

    // Create user_progress table for tracking task completion
    await run(`
      CREATE TABLE IF NOT EXISTS user_progress (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        diary_id TEXT NOT NULL,
        rsn TEXT NOT NULL,
        task_id TEXT NOT NULL,
        completed INTEGER NOT NULL DEFAULT 0,
        completed_date INTEGER,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        FOREIGN KEY (diary_id) REFERENCES diaries(id) ON DELETE CASCADE,
        UNIQUE(diary_id, rsn, task_id)
      )
    `);
    console.log('✓ Created user_progress table');

    await run(`
      CREATE INDEX IF NOT EXISTS idx_user_progress_diary_rsn
      ON user_progress(diary_id, rsn)
    `);
    console.log('✓ Created user_progress index');

    // Create api_keys table for authentication
    await run(`
      CREATE TABLE IF NOT EXISTS api_keys (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        key TEXT NOT NULL UNIQUE,
        description TEXT,
        created_by TEXT NOT NULL,
        active INTEGER NOT NULL DEFAULT 1,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        last_used DATETIME
      )
    `);
    console.log('✓ Created api_keys table');

    // Create config table for storing global settings like MOTD
    await run(`
      CREATE TABLE IF NOT EXISTS config (
        key TEXT PRIMARY KEY,
        value TEXT,
        updated_by TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
      )
    `);
    console.log('✓ Created config table');

    console.log('\n✅ Migration completed successfully!');
  } catch (error) {
    console.error('❌ Migration failed:', error);
    throw error;
  }
}

// Run migration if this file is executed directly
if (require.main === module) {
  migrate()
    .then(() => {
      db.close();
      process.exit(0);
    })
    .catch((error) => {
      console.error(error);
      db.close();
      process.exit(1);
    });
}

export { migrate };
