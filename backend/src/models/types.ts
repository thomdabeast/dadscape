/**
 * Type definitions matching the Java data models
 */

export enum TaskType {
  KILL = 'KILL',
  SKILL = 'SKILL',
  QUEST = 'QUEST',
  ITEM = 'ITEM',
  LOCATION = 'LOCATION',
  BOSS = 'BOSS',
  MINIGAME = 'MINIGAME',
  CUSTOM = 'CUSTOM'
}

export interface DiaryTask {
  id: string;
  description: string;
  type: TaskType;
  requirements: Record<string, string>;
  hint: string;
  order: number;
}

export interface DiaryTier {
  tierName: string;
  tierColor: string;
  tasks: DiaryTask[];
  rewardDescription: string;
  order: number;
}

export interface ClanDiary {
  id: string;
  name: string;
  description: string;
  category: string;
  version: string;
  createdDate: number;
  createdBy: string;
  lastModified: number;
  lastModifiedBy: string;
  tiers: DiaryTier[];
  active: boolean;
}

export interface DiaryCreateRequest {
  name: string;
  description?: string;
  category: string;
  createdBy: string;
}

export interface DiaryUpdateRequest {
  id: string;
  name?: string;
  description?: string;
  category?: string;
  version?: string;
  tiers?: DiaryTier[];
  active?: boolean;
  lastModifiedBy: string;
}

export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}
