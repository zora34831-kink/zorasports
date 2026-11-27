const LEAGUES = [
  { id: "premier-league", name: "Premier League", country: "England", espn: "eng.1", footballData: "PL" },
  { id: "la-liga", name: "La Liga", country: "Spain", espn: "esp.1", footballData: "PD" },
  { id: "serie-a", name: "Serie A", country: "Italy", espn: "ita.1", footballData: "SA" },
  { id: "bundesliga", name: "Bundesliga", country: "Germany", espn: "ger.1", footballData: "BL" },
  { id: "ligue-1", name: "Ligue 1", country: "France", espn: "fra.1", footballData: "FL" },
  { id: "liga-1-indonesia", name: "Liga 1 Indonesia", country: "Indonesia", apisports: true },
  { id: "champions-league", name: "UEFA Champions League", country: "Europe", espn: "uefa.champions", footballData: "CL" }
];

const ESPN = "https://site.api.espn.com/apis/site/v2/sports/soccer";
const ESPN_STANDINGS = "https://site.api.espn.com/apis/v2/sports/soccer";
const FD = "https://api.football-data.org/v4";
const APISPORTS = "https://v3.football.api-sports.io";

const json = (data, status = 200, extra = {}) => new Response(JSON.stringify(data), {
  status,
  headers: { "content-type": "application/json; charset=utf-8", "access-control-allow-origin": "*", "access-control-allow-methods": "GET,POST,OPTIONS", "access-control-allow-headers": "content-type,x-zora-admin-token", ...extra }
});
const getLeague = (id) => LEAGUES.find((league) => league.id === id);
const safeFetch = async (url, headers = {}) => {
  const response = await fetch(url, { headers, cf: { cacheTtl: 45, cacheEverything: true } });
  if (!response.ok) throw new Error(`HTTP ${response.status} from ${new URL(url).hostname}`);
  return response.json();
};

function normalizeEspnMatch(event, league) {
  const competition = event.competitions?.[0];
  const teams = competition?.competitors || [];
  const home = teams.find((team) => team.homeAway === "home") || teams[0];
  const away = teams.find((team) => team.homeAway === "away") || teams[1];
  const status = competition?.status?.type || event.status?.type || {};
  return {
    id: String(event.id), leagueId: league.id, league: league.name, date: event.date || null,
    status: status.state === "in" ? "LIVE" : status.completed ? "FINISHED" : "SCHEDULED",
    minute: competition?.status?.displayClock || null,
    home: { name: home?.team?.displayName || "Home", logo: home?.team?.logo || null },
    away: { name: away?.team?.displayName || "Away", logo: away?.team?.logo || null },
    score: { home: home?.score == null ? null : Number(home.score), away: away?.score == null ? null : Number(away.score) },
    source: "ESPN"
  };
}

async function espnMatches(league) {
  const data = await safeFetch(`${ESPN}/${league.espn}/scoreboard`);
  return (data.events || []).map((event) => normalizeEspnMatch(event, league));
}

async function espnStandings(league) {
  const data = await safeFetch(`${ESPN_STANDINGS}/${league.espn}/standings`);
  const entries = data.children?.flatMap((group) => group.standings?.entries || []) || data.standings?.entries || [];
  return entries.map((entry) => {
    const stats = Object.fromEntries((entry.stats || []).map((item) => [item.name, Number(item.value)]));
    return {
      position: Number(stats.rank || 0), team: entry.team?.displayName || "Unknown",
      logo: entry.team?.logos?.[0]?.href || null, played: Number(stats.gamesPlayed || 0),
      won: Number(stats.wins || 0), drawn: Number(stats.ties || 0), lost: Number(stats.losses || 0),
      goalsFor: Number(stats.pointsFor || 0), goalsAgainst: Number(stats.pointsAgainst || 0),
      goalDifference: Number(stats.pointDifferential || 0), points: Number(stats.points || 0), source: "ESPN"
    };
  });
}

function normalizeFdMatch(match, league) {
  const status = ["IN_PLAY", "PAUSED"].includes(match.status) ? "LIVE" : match.status === "FINISHED" ? "FINISHED" : "SCHEDULED";
  return {
    id: String(match.id), leagueId: league.id, league: league.name, date: match.utcDate || null, status,
    minute: match.minute ? String(match.minute) : null,
    home: { name: match.homeTeam?.name || "Home", logo: match.homeTeam?.crest || null },
    away: { name: match.awayTeam?.name || "Away", logo: match.awayTeam?.crest || null },
    score: { home: match.score?.fullTime?.home ?? match.score?.regularTime?.home ?? null, away: match.score?.fullTime?.away ?? match.score?.regularTime?.away ?? null },
    source: "FOOTBALL_DATA"
  };
}

async function footballDataMatches(league, env) {
  if (!env.FOOTBALL_DATA_TOKEN || !league.footballData) throw new Error("FOOTBALL_DATA_TOKEN atau kompetisi tidak tersedia");
  const today = new Date().toISOString().slice(0, 10);
  const next = new Date(Date.now() + 7 * 86400000).toISOString().slice(0, 10);
  const data = await safeFetch(`${FD}/competitions/${league.footballData}/matches?dateFrom=${today}&dateTo=${next}`, { "X-Auth-Token": env.FOOTBALL_DATA_TOKEN });
  return (data.matches || []).map((match) => normalizeFdMatch(match, league));
}

async function footballDataStandings(league, env) {
  if (!env.FOOTBALL_DATA_TOKEN || !league.footballData) throw new Error("FOOTBALL_DATA_TOKEN atau kompetisi tidak tersedia");
  const data = await safeFetch(`${FD}/competitions/${league.footballData}/standings`, { "X-Auth-Token": env.FOOTBALL_DATA_TOKEN });
  const table = data.standings?.find((item) => item.type === "TOTAL")?.table || [];
  return table.map((row) => ({
    position: row.position, team: row.team?.name || "Unknown", logo: row.team?.crest || null,
    played: row.playedGames || 0, won: row.won || 0, drawn: row.draw || 0, lost: row.lost || 0,
    goalsFor: row.goalsFor || 0, goalsAgainst: row.goalsAgainst || 0, goalDifference: row.goalDifference || 0,
    points: row.points || 0, source: "FOOTBALL_DATA"
  }));
}

async function apiSports(league, kind, env) {
  if (!env.APISPORTS_KEY) throw new Error("APISPORTS_KEY belum dikonfigurasi");
  const id = env.APISPORTS_INDONESIA_LEAGUE_ID || "274";
  const season = env.APISPORTS_SEASON || String(new Date().getUTCFullYear());
  const path = kind === "matches" ? `fixtures?league=${id}&season=${season}&timezone=Asia%2FJakarta` : `standings?league=${id}&season=${season}`;
  const data = await safeFetch(`${APISPORTS}/${path}`, { "x-apisports-key": env.APISPORTS_KEY });
  if (data.errors && Object.keys(data.errors).length) throw new Error(JSON.stringify(data.errors));
  if (kind === "standings") {
    const rows = data.response?.[0]?.league?.standings?.[0] || [];
    return rows.map((row) => ({
      position: row.rank, team: row.team?.name || "Unknown", logo: row.team?.logo || null,
      played: row.all?.played || 0, won: row.all?.win || 0, drawn: row.all?.draw || 0, lost: row.all?.lose || 0,
      goalsFor: row.all?.goals?.for || 0, goalsAgainst: row.all?.goals?.against || 0,
      goalDifference: row.goalsDiff || 0, points: row.points || 0, source: "API_SPORTS"
    }));
  }
  return (data.response || []).map((item) => ({
    id: String(item.fixture?.id), leagueId: league.id, league: league.name, date: item.fixture?.date || null,
    status: ["1H", "2H", "HT", "ET", "P", "LIVE"].includes(item.fixture?.status?.short) ? "LIVE" : ["FT", "AET", "PEN"].includes(item.fixture?.status?.short) ? "FINISHED" : "SCHEDULED",
    minute: item.fixture?.status?.elapsed == null ? null : String(item.fixture.status.elapsed),
    home: { name: item.teams?.home?.name || "Home", logo: item.teams?.home?.logo || null },
    away: { name: item.teams?.away?.name || "Away", logo: item.teams?.away?.logo || null },
    score: { home: item.goals?.home ?? null, away: item.goals?.away ?? null }, source: "API_SPORTS"
  }));
}

async function loadLeagueData(kind, league, env) {
  const providers = league.id === "liga-1-indonesia"
    ? [["API_SPORTS", () => apiSports(league, kind, env)]]
    : [
        ["ESPN", () => kind === "matches" ? espnMatches(league) : espnStandings(league)],
        ["FOOTBALL_DATA", () => kind === "matches" ? footballDataMatches(league, env) : footballDataStandings(league, env)]
      ];
  const errors = [];
  for (const [source, load] of providers) {
    try {
      const data = await load();
      if (Array.isArray(data) && data.length) return { data, source, stale: false, updatedAt: new Date().toISOString() };
      errors.push(`${source}: respons kosong`);
    } catch (error) {
      errors.push(`${source}: ${error.message}`);
    }
  }
  throw new Error(errors.join(" | ") || "Tidak ada sumber data tersedia");
}

async function handle(request, env) {
  const url = new URL(request.url);
  if (request.method === "OPTIONS") return new Response(null, { headers: { "access-control-allow-origin": "*", "access-control-allow-methods": "GET,POST,OPTIONS", "access-control-allow-headers": "content-type,x-zora-admin-token" } });
  if (url.pathname === "/api/v1/health") return json({ ok: true, service: "ZORA FOOTBALL DATA", runtime: "Cloudflare Workers", time: new Date().toISOString() });
  if (url.pathname === "/api/v1/leagues") return json({ data: LEAGUES.map(({ id, name, country }) => ({ id, name, country, enabled: true })) });

  if (url.pathname === "/api/v1/admin/sync" && request.method === "POST") {
    if (!env.ADMIN_SYNC_TOKEN || request.headers.get("x-zora-admin-token") !== env.ADMIN_SYNC_TOKEN) return json({ error: "Unauthorized" }, 401);
    return json({ ok: true, message: "Worker tidak menyimpan cache persistente; data akan diperbarui saat endpoint dipanggil.", time: new Date().toISOString() });
  }

  let kind = null;
  let league = null;
  if (url.pathname === "/api/v1/matches/live") kind = "live";
  else if (url.pathname === "/api/v1/matches") kind = "matches";
  else {
    const match = url.pathname.match(/^\/api\/v1\/standings\/([^/]+)$/);
    if (match) { kind = "standings"; league = getLeague(match[1]); }
  }
  if (!kind) return json({ error: "Endpoint tidak ditemukan" }, 404);
  if (kind === "standings" && !league) return json({ error: "Liga tidak ditemukan" }, 404);

  const requestedId = url.searchParams.get("league");
  if (kind === "matches" && requestedId && !getLeague(requestedId)) return json({ error: "Liga tidak ditemukan" }, 404);
  const selected = league ? [league] : kind === "matches" && requestedId ? [getLeague(requestedId)] : LEAGUES;

  const results = await Promise.allSettled(selected.map(async (item) => ({ league: item, ...(await loadLeagueData(kind === "live" ? "matches" : kind, item, env)) })));
  const good = results.filter((item) => item.status === "fulfilled").map((item) => item.value);
  const failed = results.filter((item) => item.status === "rejected");
  const data = good.flatMap((item) => kind === "live" ? item.data.filter((match) => match.status === "LIVE") : item.data);
  if (!good.length) return json({ error: "Semua sumber data gagal", details: failed.map((item) => item.reason?.message || "Unknown error") }, 503);
  if (kind === "matches") data.sort((a, b) => String(a.date).localeCompare(String(b.date)));
  const metadata = { data, count: data.length, updatedAt: new Date().toISOString(), partial: failed.length > 0 };
  if (kind === "standings") return json({ league: { id: league.id, name: league.name }, ...metadata, source: good[0].source, stale: false }, 200, { "cache-control": "public, max-age=0, s-maxage=60" });
  return json(metadata, 200, { "cache-control": "public, max-age=0, s-maxage=45" });
}

export default {
  async fetch(request, env) {
    try { return await handle(request, env); }
    catch (error) { return json({ error: error.message || "Internal server error" }, 500); }
  },
  async scheduled(_event, env, ctx) {
    ctx.waitUntil((async () => {
      const jobs = LEAGUES.flatMap((league) => ["matches", "standings"].map((kind) => loadLeagueData(kind, league, env)));
      await Promise.allSettled(jobs);
    })());
  }
};
