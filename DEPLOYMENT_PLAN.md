# Roamly deployment plan

## 1. Prepare secrets

Move these values out of `src/main/resources/application.properties` before pushing publicly:

- database URL, username, and password
- JWT secret
- Stripe secret key and webhook secret
- Google OAuth client ID and client secret

Use environment variables on your backend host.

## 2. Push to GitHub

```bash
git init
git add .
git commit -m "Build Roamly frontend and booking flow"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

Before pushing, rotate any secrets that were ever committed or shared.

## 3. Backend deployment

Recommended beginner-friendly hosts:

- Render
- Railway
- Fly.io
- AWS Elastic Beanstalk / ECS when you want more control

Backend build command:

```bash
./mvnw -DskipTests package
```

Backend start command:

```bash
java -jar target/AirbnbApp-0.0.1-SNAPSHOT.jar
```

Set these environment variables on the backend host:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://HOST:5432/DB_NAME
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
JWT_SECRETKEY=...
STRIPE_SECRET_KEY=...
STRIPE_WEBHOOK_SECRET=...
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
FRONTEND_BASE_URL=https://YOUR_NETLIFY_SITE.netlify.app
FRONTEND_OAUTH_SUCCESS_URL=https://YOUR_NETLIFY_SITE.netlify.app/#/auth/callback?accessToken=
```

## 4. Netlify frontend deployment

This project currently serves the frontend from:

```text
src/main/resources/static
```

In Netlify:

- Base directory: leave empty or set project root
- Publish directory: `src/main/resources/static`
- Build command: leave empty

After backend deployment, edit:

```text
src/main/resources/static/config.js
```

Set:

```js
window.ROAMLY_API_BASE = "https://YOUR_BACKEND_DOMAIN/api/v1";
```

Then commit and push again. Netlify will redeploy the frontend.

## 5. Docker plan

Create an image for the Spring Boot app, then push it to Docker Hub or GitHub Container Registry.

Build:

```bash
docker build -t YOUR_USERNAME/roamly:latest .
```

Run locally:

```bash
docker run --env-file .env -p 8091:8090 YOUR_USERNAME/roamly:latest
```

Push to Docker Hub:

```bash
docker login
docker push YOUR_USERNAME/roamly:latest
```

## 6. Production checklist

- Use a managed PostgreSQL database.
- Add frontend origin to CORS.
- Configure Google OAuth redirect URI for the production backend.
- Configure Stripe webhook URL for the production backend.
- Rotate current secrets before public GitHub push.
- Disable verbose SQL logging in production.
