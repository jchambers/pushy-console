Pushy Console is a simple GUI application for sending APNs (iOS/macOS/Safari) push notifications. It's built on the [Pushy library](https://github.com/relayrides/pushy) and is maintained by the engineers at [Turo](https://turo.com/).

Pushy Console is intended as a tool for developers, and is primarily intended to provide an easy way to send single notifications to specific devices. It is _not_ intended as a tool for use in any production environment, for sending large numbers of notifications, or for automated use. Users who need to send lots of notifications quickly and efficiently should check out the [Pushy library](https://github.com/relayrides/pushy).

## Running Pushy Console

To run Pushy Console from the command line:

```sh
mvn exec:java -Dexec.mainClass="com.turo.pushy.console.PushyConsoleApplication"
```

You can also run Pushy Console from within the IDE of your choice.
