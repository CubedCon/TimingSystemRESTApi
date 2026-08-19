## TimingSystemRESTApi
This plugin provides a basic JSON REST api for [TimingSystem](https://github.com/Makkuusen/TimingSystem).

This plugin requires [TimingSystem](https://github.com/Makkuusen/TimingSystem) to be installed on the server, otherwise it doesn't do anything.

Files placed in the ./plugins/TimingSystemRESTApi/public_html/ folder will be served from the internal web server.       
The internal web server port can be configured by changing the `port` in `config.yml`.

### Optional plugin integrations

Some routes come from other plugins. None of them have to be installed: their routes are only
registered when the plugin is found on the server, and a plugin that is missing, disabled or too old
is logged and skipped without affecting the rest of the API. Every route below is read only and takes
the usual `api_key` query parameter.

#### DailyGP

Registered when [DailyGP](https://github.com/M2B5/DailyGP) is installed.

| Route | Returns |
|-------|---------|
| `/api/v4/readonly/dailygp` | The Grand Prix as it stands: state, track, lap count, pit stops, driver cap, qualifying time left and the drivers taking part |
| `/api/v4/readonly/dailygp/results/latest` | The classification of the most recent Grand Prix, with race times and DNFs (404 before the first race) |
| `/api/v4/readonly/dailygp/stats` | Every driver's Grand Prix wins and streaks, most wins first |
| `/api/v4/readonly/dailygp/stats/:uuidorusername` | One driver's wins, current streak and highest streak |
| `/api/v4/readonly/dailygp/excluded-tracks` | The track ids currently held back from the draw, and the queue length |

#### PartyTS duels

Registered when [PartyTS](https://github.com/2818/PartyTS) is installed.

| Route | Returns |
|-------|---------|
| `/api/v4/readonly/duels/leaderboard` | The ELO leaderboard. Paged with `page` and `page_size` (default 10, max 100) |
| `/api/v4/readonly/duels/players/:uuidorusername` | One player's ELO, wins, losses and total duels |
| `/api/v4/readonly/duels/players/:uuidorusername/matches` | That player's recent duels with ELO changes. `limit` defaults to 10, max 100 |

Player routes accept either a UUID or a username the server has seen before.     

