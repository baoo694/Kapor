# kapor_flutter

## Backend API configuration

The API base URL is read from `API_BASE_URL` in the root `.env` file. To point
the app at a different backend, change that one value and restart the app:

```env
API_BASE_URL=https://api.domday.food/api
```

Use `.env.example` as a reference for local emulator values. The `.env` file
is bundled with the app, so it must not contain secrets such as API keys or
passwords.

## Google Sign-In

The app sends a Google ID token to `POST /auth/google`; the backend verifies
that token and issues Kapor's access and refresh JWTs. Add the OAuth 2.0 **Web
client ID** (not a client secret) to the app `.env` and set the identical value
as `GOOGLE_CLIENT_ID` for `kapor-backend`:

```env
GOOGLE_SERVER_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

The Google Cloud project also needs an Android OAuth client registered for the
application ID and signing-certificate SHA-1 used to build the app. The current
application ID is `com.example.kapor_flutter`; change it before a production
release and register the final ID and release SHA-1. For iOS, create an iOS
OAuth client for the final bundle ID and add its client ID plus reversed URL
scheme to `ios/Runner/Info.plist` as required by Google Sign-In.
