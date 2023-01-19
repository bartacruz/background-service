# @lagruva/background-service

Lagruva Background Services

## Install

```bash
npm install @lagruva/background-service
npx cap sync
```

## API

<docgen-index>

* [`start()`](#start)
* [`stop()`](#stop)
* [`addListener('pushNotificationReceived', ...)`](#addlistenerpushnotificationreceived)
* [`addListener('positionReceived', ...)`](#addlistenerpositionreceived)
* [`sendMessage(...)`](#sendmessage)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### start()

```typescript
start() => void
```

Start geolocation

--------------------


### stop()

```typescript
stop() => void
```

Stop geolocation

--------------------


### addListener('pushNotificationReceived', ...)

```typescript
addListener(eventName: 'pushNotificationReceived', listenerFunc: (notification: PushNotificationSchema) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Called when the device receives a push notification.

| Param              | Type                                                                                                 |
| ------------------ | ---------------------------------------------------------------------------------------------------- |
| **`eventName`**    | <code>'pushNotificationReceived'</code>                                                              |
| **`listenerFunc`** | <code>(notification: <a href="#pushnotificationschema">PushNotificationSchema</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### addListener('positionReceived', ...)

```typescript
addListener(eventName: 'positionReceived', listenerFunc: (position: Position) => void) => Promise<PluginListenerHandle> & PluginListenerHandle
```

Called when the device receives a push notification.

| Param              | Type                                                                 |
| ------------------ | -------------------------------------------------------------------- |
| **`eventName`**    | <code>'positionReceived'</code>                                      |
| **`listenerFunc`** | <code>(position: <a href="#position">Position</a>) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt; & <a href="#pluginlistenerhandle">PluginListenerHandle</a></code>

--------------------


### sendMessage(...)

```typescript
sendMessage(options: { receiver: string; data: object; }) => Promise<{ message: string; }>
```

Sends a message to a FCM receiver

| Param         | Type                                             |
| ------------- | ------------------------------------------------ |
| **`options`** | <code>{ receiver: string; data: object; }</code> |

**Returns:** <code>Promise&lt;{ message: string; }&gt;</code>

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### PushNotificationSchema

Copied from push-notificatons

| Prop               | Type                 | Description                                                                                                          | Since |
| ------------------ | -------------------- | -------------------------------------------------------------------------------------------------------------------- | ----- |
| **`title`**        | <code>string</code>  | The notification title.                                                                                              | 1.0.0 |
| **`subtitle`**     | <code>string</code>  | The notification subtitle.                                                                                           | 1.0.0 |
| **`body`**         | <code>string</code>  | The main text payload for the notification.                                                                          | 1.0.0 |
| **`id`**           | <code>string</code>  | The notification identifier.                                                                                         | 1.0.0 |
| **`badge`**        | <code>number</code>  | The number to display for the app icon badge.                                                                        | 1.0.0 |
| **`notification`** | <code>any</code>     | It's not being returned.                                                                                             | 1.0.0 |
| **`data`**         | <code>any</code>     | Any additional data that was included in the push notification payload.                                              | 1.0.0 |
| **`click_action`** | <code>string</code>  | The action to be performed on the user opening the notification. Only available on Android.                          | 1.0.0 |
| **`link`**         | <code>string</code>  | Deep link from the notification. Only available on Android.                                                          | 1.0.0 |
| **`group`**        | <code>string</code>  | Set the group identifier for notification grouping. Only available on Android. Works like `threadIdentifier` on iOS. | 1.0.0 |
| **`groupSummary`** | <code>boolean</code> | Designate this notification as the summary for an associated `group`. Only available on Android.                     | 1.0.0 |


#### Position

| Prop            | Type                                                                                                                                                                                | Description                                             | Since |
| --------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------- | ----- |
| **`timestamp`** | <code>number</code>                                                                                                                                                                 | Creation timestamp for coords                           | 1.0.0 |
| **`coords`**    | <code>{ latitude: number; longitude: number; accuracy: number; altitudeAccuracy: number \| null; altitude: number \| null; speed: number \| null; heading: number \| null; }</code> | The GPS coordinates along with the accuracy of the data | 1.0.0 |

</docgen-api>
