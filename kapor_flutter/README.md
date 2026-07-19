# kapor_flutter

## Backend API configuration

The API base URL is read from `API_BASE_URL` in the root `.env` file. To point
the app at a different backend, change that one value and restart the app:

```env
API_BASE_URL=http://192.168.0.85:8080/api
```

Use `.env.example` as a reference for emulator values. The `.env` file is
bundled with the app, so it must not contain secrets such as API keys or
passwords.
