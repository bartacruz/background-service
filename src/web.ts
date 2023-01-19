import { PluginListenerHandle, WebPlugin } from '@capacitor/core';


import type { BackgroundServicePlugin, PushNotificationSchema} from './definitions';



export class BackgroundServiceWeb extends WebPlugin implements BackgroundServicePlugin {
  started = false;
  start(): void {
    console.log('start');
    this.started = true;
  }
  stop(): void {
    this.started = false;
    console.log('stop');
  }
  addListener(
    eventName: 'pushNotificationReceived',
    listenerFunc: (notification: PushNotificationSchema) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;

  
}
