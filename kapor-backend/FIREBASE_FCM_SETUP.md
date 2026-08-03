# Firebase Cloud Messaging setup

Kapor's reminder flow is safe to deploy before Firebase is configured: set
`FIREBASE_ENABLED=false` and device registration/reminders remain disabled.

## 1. Configure the Flutter app

Create a Firebase project, add the Android and iOS applications, then run
FlutterFire configuration from `kapor_flutter`:

```bash
dart pub global activate flutterfire_cli
flutterfire configure
```

This generates the Firebase platform configuration. Keep iOS APNs credentials
configured in the Firebase console before testing on physical iPhones.

## 2. Configure the API server

In Firebase Console, create an Admin SDK service-account JSON key and copy it
to a file readable by Docker, for example:

```bash
sudo install -d -m 700 /opt/kapor/secrets
sudo install -m 600 ~/Downloads/kapor-firebase-admin.json /opt/kapor/secrets/kapor-firebase-admin.json
```

Add these variables to `kapor-backend/.env.local` on the production host:

```dotenv
FIREBASE_ENABLED=true
FIREBASE_SERVICE_ACCOUNT_HOST_PATH=/opt/kapor/secrets/kapor-firebase-admin.json
FIREBASE_SERVICE_ACCOUNT_FILE=/run/secrets/kapor-firebase-admin.json
# Optional: scheduler checks every five minutes by default.
FIREBASE_REMINDER_CRON=0 */5 * * * *
```

Deploy the API with the FCM override:

```bash
docker compose --env-file .env.local -f docker-compose.yml -f docker-compose.fcm.yml up -d --build kapor-api
```

For an existing MongoDB volume, create the token indexes once:

```bash
docker compose exec mongo mongosh kapor --eval 'db.fcm_device_tokens.createIndex({token:1},{unique:true}); db.fcm_device_tokens.createIndex({userId:1,enabled:1})'
```

The API records device tokens only for authenticated users. The scheduled job
sends at most one reminder per device/day after the learner's selected reminder
time, only while the daily minute goal has not been reached.
