# Pushy Console

Pushy Console is a simple GUI application for sending APNs (iOS/macOS/Safari) push notifications. It's built on the [Pushy library](https://github.com/relayrides/pushy) and is maintained by the engineers at [Turo](https://turo.com/).

<div align="center"><img src="https://user-images.githubusercontent.com/31352/39678967-b03ce3a8-5164-11e8-89d4-460121ba8e04.png" width="712" alt="Pushy Console"/></div>

Pushy Console is intended as a tool for developers, and is primarily intended to provide an easy way to send single notifications to specific devices. It is _not_ intended as a tool for use in any production environment, for sending large numbers of notifications, or for automated use. Users who need to send lots of notifications quickly and efficiently should check out the [Pushy library](https://github.com/relayrides/pushy).

## Getting and running Pushy Console

Pushy Console is currently distributed as a source-only project. To get Pushy Console, you'll need to clone the Pushy Console repository. Once you've done that, you can use [Maven](https://maven.apache.org/) to run Pushy Console from the command line. Assuming you're already in the Pushy Console directory:

```sh
# Compilation only needs to happen once (or when the source code changes)
mvn clean package

# Run Pushy Console from an executable .jar
java -jar target/pushy-console-${version}.jar

# …or launch Pushy Console through Maven
mvn exec:java -Dexec.mainClass="com.turo.pushy.console.PushyConsoleApplication"
```

You can also run Pushy Console from within the IDE of your choice.

## Sending push notifications with Pushy Console

Before you can start sending push notifications, you'll need to follow the instructions in Apple's ["Registering Your App with APNs"](https://developer.apple.com/documentation/usernotifications/registering_your_app_with_apns) documentation. When you've done that, you'll have three things you need to send a notification to a device running your app:

1. APNs client credentials (either a certificate or a signing key).
2. An APNs "topic" to which you'd like to send a notification. Topics are generally the bundle ID for one of your apps.
3. A "device token," which specifies the device to which to send a notification.

Pushy Console supports both certificate-based and token-based authentication with the APNs server. Either is just fine from Pushy Console's perspective, and you can have multiple certificates and signing keys in play elsewhere. From a practical perspective:

- Certificates expire after a year, and can only be used for a specific set of topics associated with a single app. You'll usually need a password to "unlock" a certificate.
- Signing keys never expire, and can be used for all topics associated with your developer account. You don't need a pasword to use signing keys, but will need to know they key ID and your Apple Developer Team ID.

You can choose a credentials file by clicking the "browse" button next to the "credentials" field in Pushy Console. If you choose a certificate, you'll be prompted for a password. If you choose a signing key, you'll need to provide a key ID and team ID.

Next, you'll need to choose a topic to which to send a notification. If you're using certificate-based authentication, Pushy Console's topic list will be populated automatically with the topics allowed by the chosen certificate. If you've chosen a signing key instead, you'll need to enter the topic (usually your app's bundie ID) manually.

Regardless of the type of credentials you choose, you'll also need to specify a device token and a notification payload. Please see Apple's ["Generating a Remote Notification"](https://developer.apple.com/documentation/usernotifications/setting_up_a_remote_notification_server/generating_a_remote_notification) documentation for details about constructing a payload. As an easy example, though, you might try a payload that includes a simple alert:

```json
{
    "aps": {
        "alert": "Hello from Pushy Console!"
    }
}
```

With all of the pieces in place, you can click the "send notification" button to send the notification to Apple's servers. The outcome of the delivery attempt will appear in the table at the bottom of the Pushy Console window.
