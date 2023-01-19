import { registerPlugin } from '@capacitor/core';

import type { BackgroundServicePlugin } from './definitions';

const BackgroundService = registerPlugin<BackgroundServicePlugin>(
  'BackgroundService', 
  {}
);

export * from './definitions';
export { BackgroundService };
