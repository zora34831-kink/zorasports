export const LEAGUES = [
  { id: "premier-league", name: "Premier League", country: "England", espn: "eng.1", footballData: "PL" },
  { id: "la-liga", name: "La Liga", country: "Spain", espn: "esp.1", footballData: "PD" },
  { id: "serie-a", name: "Serie A", country: "Italy", espn: "ita.1", footballData: "SA" },
  { id: "bundesliga", name: "Bundesliga", country: "Germany", espn: "ger.1", footballData: "BL" },
  { id: "ligue-1", name: "Ligue 1", country: "France", espn: "fra.1", footballData: "FL" },
  { id: "liga-1-indonesia", name: "Liga 1 Indonesia", country: "Indonesia", apisports: true },
  { id: "champions-league", name: "UEFA Champions League", country: "Europe", espn: "uefa.champions", footballData: "CL" }
];

export const leagueById = (id) => LEAGUES.find((league) => league.id === id);
export const CACHE_TTL = Math.max(15, Number(process.env.CACHE_TTL_SECONDS || 60)) * 1000;
