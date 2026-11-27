const ESPN = "https://site.api.espn.com/apis/site/v2/sports/soccer";
const ESPN_STANDINGS = "https://site.api.espn.com/apis/v2/sports/soccer";
const FD = "https://api.football-data.org/v4";
const APISPORTS = "https://v3.football.api-sports.io";

async function getJson(url, headers = {}) {
  const response = await fetch(url, { headers, signal: AbortSignal.timeout(12000) });
  if (!response.ok) throw new Error(`HTTP ${response.status} from ${new URL(url).hostname}`);
  return response.json();
}

function espnMatch(event, league) {
  const competition = event.competitions?.[0];
  const competitors = competition?.competitors || [];
  const home = competitors.find((team) => team.homeAway === "home") || competitors[0];
  const away = competitors.find((team) => team.homeAway === "away") || competitors[1];
  const status = competition?.status?.type || event.status?.type || {};
  return {
    id: String(event.id),
    leagueId: league.id,
    league: league.name,
    date: event.date || null,
    status: status.state === "in" ? "LIVE" : status.completed ? "FINISHED" : "SCHEDULED",
    minute: competition?.status?.displayClock || null,
    home: { name: home?.team?.displayName || "Home", logo: home?.team?.logo || null },
    away: { name: away?.team?.displayName || "Away", logo: away?.team?.logo || null },
    score: { home: home?.score == null ? null : Number(home.score), away: away?.score == null ? null : Number(away.score) },
    source: "ESPN"
  };
}

export async function espnMatches(league) {
  const data = await getJson(`${ESPN}/${league.espn}/scoreboard`);
  return (data.events || []).map((event) => espnMatch(event, league));
}

export async function espnStandings(league) {
  const data = await getJson(`${ESPN_STANDINGS}/${league.espn}/standings`);
  const entries = data.children?.flatMap((group) => group.standings?.entries || [])
    || data.standings?.entries || [];
  return entries.map((entry) => {
    const stats = Object.fromEntries((entry.stats || []).map((item) => [item.name, Number(item.value)]));
    return {
      position: Number(stats.rank || 0),
      team: entry.team?.displayName || "Unknown",
      logo: entry.team?.logos?.[0]?.href || null,
      played: Number(stats.gamesPlayed || 0),
      won: Number(stats.wins || 0),
      drawn: Number(stats.ties || 0),
      lost: Number(stats.losses || 0),
      goalsFor: Number(stats.pointsFor || 0),
      goalsAgainst: Number(stats.pointsAgainst || 0),
      goalDifference: Number(stats.pointDifferential || 0),
      points: Number(stats.points || 0),
      source: "ESPN"
    };
  });
}

function fdMatch(match, league) {
  const status = match.status === "IN_PLAY" || match.status === "PAUSED" ? "LIVE"
    : match.status === "FINISHED" ? "FINISHED" : "SCHEDULED";
  return {
    id: String(match.id), leagueId: league.id, league: league.name,
    date: match.utcDate || null, status, minute: match.minute ? String(match.minute) : null,
    home: { name: match.homeTeam?.name || "Home", logo: match.homeTeam?.crest || null },
    away: { name: match.awayTeam?.name || "Away", logo: match.awayTeam?.crest || null },
    score: { home: match.score?.fullTime?.home ?? match.score?.regularTime?.home ?? null,
      away: match.score?.fullTime?.away ?? match.score?.regularTime?.away ?? null },
    source: "FOOTBALL_DATA"
  };
}

export async function footballDataMatches(league) {
  const token = process.env.FOOTBALL_DATA_TOKEN;
  if (!token || !league.footballData) throw new Error("football-data.org token/competition not configured");
  const data = await getJson(`${FD}/competitions/${league.footballData}/matches?dateFrom=${new Date().toISOString().slice(0,10)}&dateTo=${new Date(Date.now()+7*86400000).toISOString().slice(0,10)}`, { "X-Auth-Token": token });
  return (data.matches || []).map((match) => fdMatch(match, league));
}

export async function footballDataStandings(league) {
  const token = process.env.FOOTBALL_DATA_TOKEN;
  if (!token || !league.footballData) throw new Error("football-data.org token/competition not configured");
  const data = await getJson(`${FD}/competitions/${league.footballData}/standings`, { "X-Auth-Token": token });
  const table = data.standings?.find((item) => item.type === "TOTAL")?.table || [];
  return table.map((row) => ({
    position: row.position, team: row.team?.name || "Unknown", logo: row.team?.crest || null,
    played: row.playedGames || 0, won: row.won || 0, drawn: row.draw || 0, lost: row.lost || 0,
    goalsFor: row.goalsFor || 0, goalsAgainst: row.goalsAgainst || 0,
    goalDifference: row.goalDifference || 0, points: row.points || 0, source: "FOOTBALL_DATA"
  }));
}

export async function apiSportsMatches(league) {
  const key = process.env.APISPORTS_KEY;
  if (!key) throw new Error("APISPORTS_KEY not configured");
  const id = process.env.APISPORTS_INDONESIA_LEAGUE_ID || "274";
  const season = process.env.APISPORTS_SEASON || String(new Date().getUTCFullYear());
  const data = await getJson(`${APISPORTS}/fixtures?league=${id}&season=${season}&timezone=Asia%2FJakarta`, { "x-apisports-key": key });
  if (data.errors && Object.keys(data.errors).length) throw new Error(JSON.stringify(data.errors));
  return (data.response || []).map((item) => ({
    id: String(item.fixture?.id), leagueId: league.id, league: league.name,
    date: item.fixture?.date || null,
    status: ["1H","2H","HT","ET","P","LIVE"].includes(item.fixture?.status?.short) ? "LIVE"
      : ["FT","AET","PEN"].includes(item.fixture?.status?.short) ? "FINISHED" : "SCHEDULED",
    minute: item.fixture?.status?.elapsed == null ? null : String(item.fixture.status.elapsed),
    home: { name: item.teams?.home?.name || "Home", logo: item.teams?.home?.logo || null },
    away: { name: item.teams?.away?.name || "Away", logo: item.teams?.away?.logo || null },
    score: { home: item.goals?.home ?? null, away: item.goals?.away ?? null }, source: "API_SPORTS"
  }));
}

export async function apiSportsStandings(league) {
  const key = process.env.APISPORTS_KEY;
  if (!key) throw new Error("APISPORTS_KEY not configured");
  const id = process.env.APISPORTS_INDONESIA_LEAGUE_ID || "274";
  const season = process.env.APISPORTS_SEASON || String(new Date().getUTCFullYear());
  const data = await getJson(`${APISPORTS}/standings?league=${id}&season=${season}`, { "x-apisports-key": key });
  if (data.errors && Object.keys(data.errors).length) throw new Error(JSON.stringify(data.errors));
  const rows = data.response?.[0]?.league?.standings?.[0] || [];
  return rows.map((row) => ({
    position: row.rank, team: row.team?.name || "Unknown", logo: row.team?.logo || null,
    played: row.all?.played || 0, won: row.all?.win || 0, drawn: row.all?.draw || 0,
    lost: row.all?.lose || 0, goalsFor: row.all?.goals?.for || 0,
    goalsAgainst: row.all?.goals?.against || 0, goalDifference: row.goalsDiff || 0,
    points: row.points || 0, source: "API_SPORTS"
  }));
}
