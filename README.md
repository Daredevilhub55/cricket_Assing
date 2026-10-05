# WicketLab · Cricket Score Management System

A full stack live cricket scoring demo with a Spring Boot REST API, JPA persistence, a React matchday dashboard, ball by ball scoring, live fixture polling, scorecards, and starter data for three fixtures.

## What’s here

- Live and upcoming fixture dashboard with searchable schedule and a responsive scorer desk.
- Spring Boot APIs for matches, teams, players, innings, deliveries, scorecards, and commentary events.
- JPA persistence with an H2 file database by default; MySQL can be selected with Spring’s `mysql` profile.
- Automatic starter squads, two live match cards, and an upcoming fixture.
- Ball events update the score, wickets, legal deliveries, run rate, strike rotation, batting and bowling figures.
- Dashboard refreshes from the API every four seconds so a scorer’s update appears on viewer screens.
- Importable Postman collection in `postman/wicketlab.postman_collection.json`.

## Architecture

```text
React + Vite  →  REST /api  →  Spring Boot controller + scoring service  →  Spring Data JPA  →  H2 or MySQL
```

The schema has `teams`, `players`, `matches`, `innings`, and `ball_events`. Players belong to teams; a match references two teams; innings belong to a match and reference batting and bowling teams; ball events belong to an innings.

## Run locally

Requirements: Java 17+, Maven 3.8+, Node.js 20+, and npm. H2 is bundled, so the first demo does not require installing a database server.

1. Start the API:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

   API: `http://localhost:8080/api/matches`. H2 console: `http://localhost:8080/h2-console` with JDBC URL `jdbc:h2:file:./data/cricket`, user `sa`, and an empty password.

2. Start the React dashboard in another terminal:

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

   Open the local Vite address printed by npm (normally `http://localhost:5173`). To use a remote API, set `VITE_API_URL` to its `/api` URL before starting Vite.

## MySQL option

Create the database if it does not exist:

```sql
CREATE DATABASE cricket_score_db;
```

Set `DB_URL`, `DB_USER`, and `DB_PASSWORD`, then start Spring Boot with the MySQL profile:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/cricket_score_db'
$env:DB_USER='root'
$env:DB_PASSWORD='your-local-password'
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Hibernate creates or updates tables with `ddl-auto=update`. Credentials are read from the environment. The default H2 profile stores its database under `backend/data/`.

## REST API

| Method | Path | Purpose |
|---|---|---|
| GET / POST | `/api/matches` | List or create fixtures |
| GET / DELETE | `/api/matches/{id}` | Read or remove a fixture |
| POST | `/api/matches/{id}/start` | Start a match and first innings |
| POST | `/api/matches/{id}/innings` | Start an innings with selected batting team and players |
| POST | `/api/matches/{id}/finish` | Finish a match |
| GET / POST | `/api/teams` | List or create teams |
| GET / POST | `/api/players` | List or create players |
| POST | `/api/innings/{id}/ball` | Record RUN, WIDE, NO_BALL, BYE, LEG_BYE, or WICKET |
| POST | `/api/innings/{id}/complete` | Complete an innings |
| GET | `/api/innings/{id}/score` | Current innings score |
| GET | `/api/innings/{id}/scorecard` | Batting, bowling, and recent delivery figures |
| GET | `/api/innings/{id}/events` | Delivery history / commentary data |

Example delivery body:

```json
{ "type": "RUN", "runs": 4, "striker": "Virat Kohli", "bowler": "Pat Cummins" }
```

`WIDE` and `NO_BALL` add one team run and do not consume a legal ball. `BYE` and `LEG_BYE` add the supplied runs without charging the bowler. A completed over rotates strike; odd running totals rotate it at the end of the delivery.

## Notes

The bundled demo intentionally does not require an external cricket data feed or user accounts. The match list and live score viewer use short interval polling, which keeps setup simple while preserving persistent, API-driven scoring. Player dismissal attribution, maiden-over adjudication, toss workflows, authentication, and tournament administration are natural next steps for a production scorer.
