import express from "express";
import cors from "cors";
import { LEAGUES, leagueById, CACHE_TTL } from "./config.js";
import {
  espnMatches, espnStandings, footballDataMatches, footballDataStandings,
  apiSportsMatches, apiSportsStandings
} from "./providers.js";

const app = express();
app.use(cors());
app.use(express.json({ limit: "32kb" }));

const cache = new Map();
const providerStatus = new Map();
const now = () => new Date().toISOString();
const cacheKey = (kind, leagueId) => `${kind}:${leagueId}`;

async function firstSuccessful(providers, key) {
  const errors = [];
  for (const [name, load] of providers) {
    try {
      const data = await load();
      if (!Array.isArray(data)) throw new Error("Provider returned invalid data");
      providerStatus.set(name, { ok: true, checkedAt: now(), error: null });
      if (data.length) {
        cache.set(key, { data, updatedAt: now(), source: name });
        return { data, source: name, stale: false, updatedAt: now() };
      }
      errors.push(`${name}: empty response`);
    } catch (error) {
      const message = error.message || "unknown error";
      providerStatus.set(name, { ok: false, checkedAt: now(), error: message });
      errors.push(`${name}: ${message}`);
    }
  }
  const old = cache.get(key);
  if (old) return { ...old, stale: true, warning: errors.join(" | ") };
  throw Object.assign(new Error(errors.join(" | ") || "No provider configured"), { status: 503 });
}

function providersFor(league, kind) {
  if (league.id === "liga-1-indonesia") {
    return [[ "API_SPORTS", () => kind === "matches" ? apiSportsMatches(league) : apiSportsStandings(league) ]];
  }
  const providers = [
    ["ESPN", () => kind === "matches" ? espnMatches(league) : espnStandings(league)],
    ["FOOTBALL_DATA", () => kind === "matches" ? footballDataMatches(league) : footballDataStandings(league)]
  ];
  return providers;
}

async function getData(kind, league) {
  const key = cacheKey(kind, league.id);
  const existing = cache.get(key);
  if (existing && Date.now() - Date.parse(existing.updatedAt) < CACHE_TTL) {
    return { ...existing, stale: false, cached: true };
  }
  return firstSuccessful(providersFor(league, kind), key);
}

app.get("/api/v1/health", (_req, res) => {
  res.json({ ok: true, service: "ZORA FOOTBALL DATA", time: now(), providers: Object.fromEntries(providerStatus) });
});

app.get("/api/v1/leagues", (_req, res) => {
  res.json({ data: LEAGUES.map(({ id, name, country }) => ({ id, name, country, enabled: true })) });
});

app.get("/api/v1/matches/live", async (_req, res) => {
  try {
    const results = await Promise.allSettled(LEAGUES.map(async (league) => {
      const result = await getData("matches", league);
      return { league, result };
    }));
    const data = results.flatMap((item) => item.status === "fulfilled"
      ? item.value.result.data.filter((match) => match.status === "LIVE") : []);
    res.json({ data, count: data.length, updatedAt: now() });
  } catch (error) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

app.get("/api/v1/matches", async (req, res) => {
  const league = req.query.league ? leagueById(String(req.query.league)) : null;
  if (req.query.league && !league) return res.status(404).json({ error: "Liga tidak ditemukan" });
  const selected = league ? [league] : LEAGUES;
  const results = await Promise.allSettled(selected.map(async (item) => ({ item, result: await getData("matches", item) })));
  const data = results.flatMap((item) => item.status === "fulfilled" ? item.value.result.data : []);
  data.sort((a, b) => String(a.date).localeCompare(String(b.date)));
  res.json({ data, count: data.length, updatedAt: now(), partial: results.some((item) => item.status === "rejected") });
});

app.get("/api/v1/standings/:league", async (req, res) => {
  const league = leagueById(req.params.league);
  if (!league) return res.status(404).json({ error: "Liga tidak ditemukan" });
  try {
    const result = await getData("standings", league);
    res.json({ league: { id: league.id, name: league.name }, data: result.data, source: result.source, stale: !!result.stale, updatedAt: result.updatedAt });
  } catch (error) {
    res.status(error.status || 500).json({ error: error.message });
  }
});

app.post("/api/v1/admin/sync", async (req, res) => {
  const expected = process.env.ADMIN_SYNC_TOKEN;
  if (!expected || req.get("x-zora-admin-token") !== expected) return res.status(401).json({ error: "Unauthorized" });
  const selected = req.body?.league ? [leagueById(String(req.body.league))].filter(Boolean) : LEAGUES;
  const result = await Promise.allSettled(selected.flatMap((league) => ["matches", "standings"].map((kind) => getData(kind, league))));
  res.json({ ok: true, completed: result.filter((item) => item.status === "fulfilled").length, failed: result.filter((item) => item.status === "rejected").length, time: now() });
});

const port = Number(process.env.PORT || 3000);
app.listen(port, () => console.log(`ZORA FOOTBALL DATA listening on ${port}`));
