import { run, close } from './database';
import { v4 as uuidv4 } from 'uuid';

/**
 * Seed test data for development and testing
 */
async function seed() {
  console.log('Starting database seed...\n');

  try {
    // Create test API key
    console.log('Creating test API key...');
    await run(
      `INSERT OR REPLACE INTO api_keys (key, description, created_by, active)
       VALUES (?, ?, ?, ?)`,
      ['test-api-key-12345', 'Test API key for development', 'system', 1]
    );
    console.log('✓ API Key: test-api-key-12345');

    // Create test clan members
    console.log('\nCreating test clan members...');

    // Admin user
    await run(
      `INSERT OR REPLACE INTO clan_members (rsn, rank, joined_date, last_seen)
       VALUES (?, ?, ?, ?)`,
      ['TestAdmin', 100, Date.now(), Date.now()]
    );
    console.log('✓ TestAdmin (rank 100 - Admin)');

    // Regular user
    await run(
      `INSERT OR REPLACE INTO clan_members (rsn, rank, joined_date, last_seen)
       VALUES (?, ?, ?, ?)`,
      ['TestUser', 10, Date.now(), Date.now()]
    );
    console.log('✓ TestUser (rank 10 - Recruit)');

    // Clan owner
    await run(
      `INSERT OR REPLACE INTO clan_members (rsn, rank, joined_date, last_seen)
       VALUES (?, ?, ?, ?)`,
      ['ClanOwner', 127, Date.now(), Date.now()]
    );
    console.log('✓ ClanOwner (rank 127 - Owner)');

    // Create a sample diary
    console.log('\nCreating sample diary...');
    const diaryId = uuidv4();
    const now = Date.now();

    const sampleDiary = {
      id: diaryId,
      name: 'Beginner Combat Diary',
      description: 'Complete basic combat challenges',
      category: 'PvM',
      version: '1.0',
      createdDate: now,
      createdBy: 'TestAdmin',
      lastModified: now,
      lastModifiedBy: 'TestAdmin',
      active: true,
      tiers: [
        {
          tierName: 'Easy',
          tierColor: '#00ff00',
          order: 1,
          rewardDescription: '10k GP',
          tasks: [
            {
              id: uuidv4(),
              description: 'Kill 10 Chickens',
              type: 'KILL',
              requirements: { npc: 'Chicken', count: '10' },
              hint: 'Chickens can be found near Lumbridge',
              order: 1
            },
            {
              id: uuidv4(),
              description: 'Reach 20 Attack',
              type: 'SKILL',
              requirements: { skill: 'Attack', level: '20' },
              hint: 'Train on low-level monsters',
              order: 2
            }
          ]
        }
      ]
    };

    await run(
      `INSERT OR REPLACE INTO diaries (
        id, name, description, category, version,
        created_date, created_by, last_modified, last_modified_by,
        active, tiers_json
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        sampleDiary.id,
        sampleDiary.name,
        sampleDiary.description,
        sampleDiary.category,
        sampleDiary.version,
        sampleDiary.createdDate,
        sampleDiary.createdBy,
        sampleDiary.lastModified,
        sampleDiary.lastModifiedBy,
        sampleDiary.active ? 1 : 0,
        JSON.stringify(sampleDiary.tiers)
      ]
    );
    console.log(`✓ Sample diary created: ${sampleDiary.name} (ID: ${diaryId})`);

    // Create sample MOTD
    console.log('\nCreating sample MOTD...');
    await run(
      `INSERT OR REPLACE INTO config (key, value, updated_by)
       VALUES (?, ?, ?)`,
      ['motd', 'Welcome to DadScape! Check out our new diary challenges!', 'TestAdmin']
    );
    console.log('✓ Sample MOTD created');

    console.log('\n✅ Seed completed successfully!');
    console.log('\n📝 Test Credentials:');
    console.log('   API Key: test-api-key-12345');
    console.log('   Admin User: TestAdmin (rank 100)');
    console.log('   Regular User: TestUser (rank 10)');
    console.log('   Owner: ClanOwner (rank 127)');
    console.log('\n🧪 Run tests:');
    console.log('   curl -H "Authorization: Bearer test-api-key-12345" http://localhost:3000/api/diaries\n');
  } catch (error) {
    console.error('❌ Seed failed:', error);
    throw error;
  }
}

// Run seed if this file is executed directly
if (require.main === module) {
  seed()
    .then(() => {
      close();
      process.exit(0);
    })
    .catch((error) => {
      console.error(error);
      close();
      process.exit(1);
    });
}

export { seed };
