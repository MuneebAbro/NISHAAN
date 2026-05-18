# NISHAAN Autonomous Agent

Autonomous agent backend for the NISHAAN crisis intelligence system.

## Setup
1. Create a `.env` file in this directory with the following variables:
   - `OPENWEATHERMAP_API_KEY`
   - `GROQ_API_KEY`
   - `GOOGLE_APPLICATION_CREDENTIALS` (absolute path to `serviceAccountKey.json`, e.g. `../serviceAccountKey.json`)
2. Install requirements:
   `pip install -r requirements.txt`

## Running
Run the agent loop:
`python main.py`

## Architecture
- `main.py`: Entry point, runs the agent loop every 60s.
- `signals/`: Fetchers (Weather, Social Media, Traffic).
- `agent/`: Core logic (Fusing, Classification, Allocation, Simulation, Notification).
- `firestore/writer.py`: Writes all decisions to Firestore.
