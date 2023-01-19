import type { PluginListenerHandle } from '@capacitor/core';

export interface BackgroundServicePlugin {
  /**
   * Start geolocation 
   */
  start():void;
  
  /**
   * Stop geolocation
   */
  stop():void;
  
  /**
   * Called when the device receives a push notification.
   */
   addListener(
    eventName: 'pushNotificationReceived',
    listenerFunc: (notification: PushNotificationSchema) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
  /**
   * Called when the device receives a push notification.
   */
   addListener(
    eventName: 'positionReceived',
    listenerFunc: (position: Position) => void,
  ): Promise<PluginListenerHandle> & PluginListenerHandle;
  
  /**
     * Sends a message to a FCM receiver
     * @param receiver a fcm receiver (eg: '12345679012@fcm.googleapis.com')
     * @para data JSON object with data to send
     */
   sendMessage(options: {
    receiver: string,
    data: object,
    }): Promise<{
      message: string;
    }>;
  
}

export interface Position {
  /**
   * Creation timestamp for coords
   *
   * @since 1.0.0
   */
  timestamp: number;
  /**
   * The GPS coordinates along with the accuracy of the data
   *
   * @since 1.0.0
   */
  coords: {
      /**
       * Latitude in decimal degrees
       *
       * @since 1.0.0
       */
      latitude: number;
      /**
       * longitude in decimal degrees
       *
       * @since 1.0.0
       */
      longitude: number;
      /**
       * Accuracy level of the latitude and longitude coordinates in meters
       *
       * @since 1.0.0
       */
      accuracy: number;
      /**
       * Accuracy level of the altitude coordinate in meters, if available.
       *
       * Available on all iOS versions and on Android 8.0+.
       *
       * @since 1.0.0
       */
      altitudeAccuracy: number | null | undefined;
      /**
       * The altitude the user is at (if available)
       *
       * @since 1.0.0
       */
      altitude: number | null;
      /**
       * The speed the user is traveling (if available)
       *
       * @since 1.0.0
       */
      speed: number | null;
      /**
       * The heading the user is facing (if available)
       *
       * @since 1.0.0
       */
      heading: number | null;
  };
}
/**
 * Copied from push-notificatons
 */
export interface PushNotificationSchema {
  /**
   * The notification title.
   *
   * @since 1.0.0
   */
  title?: string;
  /**
   * The notification subtitle.
   *
   * @since 1.0.0
   */
  subtitle?: string;
  /**
   * The main text payload for the notification.
   *
   * @since 1.0.0
   */
  body?: string;
  /**
   * The notification identifier.
   *
   * @since 1.0.0
   */
  id: string;
  /**
   * The number to display for the app icon badge.
   *
   * @since 1.0.0
   */
  badge?: number;
  /**
   * It's not being returned.
   *
   * @deprecated will be removed in next major version.
   * @since 1.0.0
   */
  notification?: any;
  /**
   * Any additional data that was included in the
   * push notification payload.
   *
   * @since 1.0.0
   */
  data: any;
  /**
   * The action to be performed on the user opening the notification.
   *
   * Only available on Android.
   *
   * @since 1.0.0
   */
  click_action?: string;
  /**
   * Deep link from the notification.
   *
   * Only available on Android.
   *
   * @since 1.0.0
   */
  link?: string;
  /**
   * Set the group identifier for notification grouping.
   *
   * Only available on Android. Works like `threadIdentifier` on iOS.
   *
   * @since 1.0.0
   */
  group?: string;
  /**
   * Designate this notification as the summary for an associated `group`.
   *
   * Only available on Android.
   *
   * @since 1.0.0
   */
  groupSummary?: boolean;
}