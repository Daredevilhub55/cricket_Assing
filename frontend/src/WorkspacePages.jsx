import { useState } from 'react';
import { ArrowUpRight, Edit3, Plus, RefreshCw, Search, Shield, Trash2, Users, Wifi } from 'lucide-react';
import { request } from './api.js';

const initials = (value = '') => value.split(' ').map((word) => word[0]).join('').slice(0, 3).toUpperCase();
const json = (value) => JSON.stringify(value);

export function WorkspacePage({ page, matches, teams, players, selected, details, busy, onNotify, onRefresh, onSelectMatch, onEditMatch, onNewMatch, onScore, onStartMatch }) {
  if (page === 'Matches') return <MatchesPage matches={matches} onSelect={onSelectMatch} onEdit={onEditMatch} onNewMatch={onNewMatch} onRefresh={onRefresh} onNotify={onNotify} />;
  if (page === 'Teams & players') return <TeamsPage teams={teams} players={players} onRefresh={onRefresh} onNotify={onNotify} />;
  if (page === 'Scorer desk') return <ScorerPage matches={matches} selected={selected} details={details} busy={busy} onSelect={onSelectMatch} onScore={onScore} onStart={onStartMatch} onNotify={onNotify} />;
  if (page === 'Scorecard') return <ScorecardPage match={selected} details={details} />;
  return <LiveCentrePage matches={matches} selected={selected} details={details} onSelect={onSelectMatch} onScorecard={() => onSelectMatch(selected, 'Scorecard')} />;
}

function MatchesPage({ matches, onSelect, onEdit, onNewMatch, onRefresh, onNotify }) {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('ALL');
  const visible = matches.filter((match) => `${match.name} ${match.venue}`.toLowerCase().includes(search.toLowerCase()) && (status === 'ALL' || match.status === status));
  const remove = async (event, match) => {
    event.stopPropagation();
    if (!window.confirm(`Delete ${match.name}?`)) return;
    try { await request(`/matches/${match.id}`, { method: 'DELETE' }); await onRefresh(); onNotify('Match removed'); }
    catch (error) { onNotify(error.message); }
  };

  return <section className="workspace-page">
    <div className="workspace-toolbar"><div><span className="eyebrow">FIXTURE MANAGEMENT</span><h2>All matches</h2><p>Live, scheduled and completed fixtures from your score database.</p></div><button className="primary-button" onClick={onNewMatch}><Plus size={16}/> New match</button></div>
    <div className="page-filters"><div className="search-box"><Search size={15}/><input aria-label="Search matches" placeholder="Search teams or venues" value={search} onChange={(event) => setSearch(event.target.value)} /></div><select aria-label="Filter by status" value={status} onChange={(event) => setStatus(event.target.value)}><option value="ALL">All statuses</option><option value="LIVE">Live</option><option value="UPCOMING">Upcoming</option><option value="COMPLETED">Completed</option></select><button className="secondary-button" onClick={onRefresh}><RefreshCw size={14}/> Refresh</button></div>
    <div className="match-list">{visible.map((match) => <article key={match.id} className="match-row-card" onClick={() => onSelect(match, 'Live centre')}>
      <div className="match-row-main"><span className={`status-pill ${match.status.toLowerCase()}`}>{match.status}</span><b>{match.name}</b><small>{match.venue} · {match.matchType} · {match.totalOvers} overs · {match.createdAt ? new Date(match.createdAt).toLocaleString() : 'Date not set'}</small></div>
      <div className="match-row-teams"><span>{match.teamA.shortName}</span><i>vs</i><span>{match.teamB.shortName}</span></div>
      <div className="match-row-score"><b>{match.currentInnings ? `${match.currentInnings.runs}/${match.currentInnings.wickets}` : match.status === 'COMPLETED' ? 'Result available' : 'Not started'}</b><small>{match.currentInnings?.overs ? `${match.currentInnings.overs} ov` : '—'}</small></div>
      <div className="match-row-actions"><button className="secondary-button" onClick={(event) => { event.stopPropagation(); onSelect(match, match.status === 'UPCOMING' ? 'Scorer desk' : 'Live centre'); }}>Open <ArrowUpRight size={13}/></button>{match.status === 'UPCOMING' && <><button className="small-action" onClick={(event) => {event.stopPropagation();onEdit(match)}} aria-label={`Edit ${match.name}`}><Edit3 size={13}/></button><button className="text-danger" onClick={(event) => remove(event, match)} aria-label={`Delete ${match.name}`}><Trash2 size={14}/></button></>}</div>
    </article>)}{visible.length === 0 && <div className="page-empty">No matches match these filters.</div>}</div>
  </section>;
}

function TeamsPage({ teams, players, onRefresh, onNotify }) {
  const [teamForm, setTeamForm] = useState(null);
  const [playerForm, setPlayerForm] = useState(null);
  const [expanded, setExpanded] = useState(teams[0]?.id ?? null);
  const [saving, setSaving] = useState(false);
  const saveTeam = async (event) => {
    event.preventDefault(); setSaving(true);
    try { const body = { name: teamForm.name, shortName: teamForm.shortName }; await request(teamForm.id ? `/teams/${teamForm.id}` : '/teams', { method: teamForm.id ? 'PUT' : 'POST', body: json(body) }); setTeamForm(null); await onRefresh(); onNotify('Team saved'); }
    catch (error) { onNotify(error.message); } finally { setSaving(false); }
  };
  const savePlayer = async (event) => {
    event.preventDefault(); setSaving(true);
    try { const body = { name: playerForm.name, role: playerForm.role, teamId: Number(playerForm.teamId), battingStyle: playerForm.battingStyle, bowlingStyle: playerForm.bowlingStyle }; await request(playerForm.id ? `/players/${playerForm.id}` : '/players', { method: playerForm.id ? 'PUT' : 'POST', body: json(body) }); setPlayerForm(null); await onRefresh(); onNotify('Player saved'); }
    catch (error) { onNotify(error.message); } finally { setSaving(false); }
  };
  const deletePlayer = async (player) => { if (!window.confirm(`Remove ${player.name}?`)) return; try { await request(`/players/${player.id}`, { method: 'DELETE' }); await onRefresh(); onNotify('Player removed'); } catch (error) { onNotify(error.message); } };
  const deleteTeam = async (team) => { if (!window.confirm(`Remove ${team.name}? Teams with players or fixtures cannot be deleted.`)) return; try { await request(`/teams/${team.id}`, { method: 'DELETE' }); await onRefresh(); onNotify('Team removed'); } catch (error) { onNotify(error.message); } };

  return <section className="workspace-page">
    <div className="workspace-toolbar"><div><span className="eyebrow">SQUAD MANAGEMENT</span><h2>Teams & players</h2><p>Maintain team records, player roles and playing styles.</p></div><button className="primary-button" onClick={() => setTeamForm({ name: '', shortName: '' })}><Plus size={16}/> Add team</button></div>
    <div className="team-list">{teams.map((team) => { const roster = players.filter((player) => player.team?.id === team.id); const open = expanded === team.id; return <article className="team-management-card" key={team.id}>
      <button className="team-management-head" onClick={() => setExpanded(open ? null : team.id)}><span className="team-initials">{initials(team.shortName)}</span><span className="team-heading"><b>{team.name}</b><small>{team.shortName} · {roster.length} players</small></span><span className="team-head-actions"><span role="button" tabIndex="0" className="small-action" onClick={(event) => { event.stopPropagation(); setTeamForm({ id: team.id, name: team.name, shortName: team.shortName }); }}>Edit team</span><span className="chevron-text">{open ? '−' : '+'}</span></span></button>
      {open && <div className="team-roster"><div className="roster-toolbar"><span>PLAYER ROSTER</span><button className="secondary-button" onClick={() => setPlayerForm({ name: '', role: 'BATSMAN', teamId: team.id, battingStyle: '', bowlingStyle: '' })}><Plus size={13}/> Add player</button></div>{roster.map((player) => <div className="player-management-row" key={player.id}><span className="player-avatar">{initials(player.name)}</span><span className="player-management-name"><b>{player.name}</b><small>{player.role || 'PLAYER'} · {player.battingStyle || 'Batting style not set'} · {player.bowlingStyle || 'Bowling style not set'}</small></span><button className="small-action" onClick={() => setPlayerForm({ ...player, teamId: player.team.id })} aria-label={`Edit ${player.name}`}><Edit3 size={13}/></button><button className="small-action" onClick={() => deletePlayer(player)} aria-label={`Delete ${player.name}`}><Trash2 size={13}/></button></div>)}{roster.length === 0 && <p className="empty-roster">No players registered for this team yet.</p>}<button className="text-danger team-delete" onClick={() => deleteTeam(team)}>Delete team</button></div>}
    </article>; })}{teams.length === 0 && <div className="page-empty">Create a team to begin building the roster.</div>}</div>
    {teamForm && <div className="modal-backdrop" onClick={() => setTeamForm(null)}><form className="create-modal" onClick={(event) => event.stopPropagation()} onSubmit={saveTeam}><button className="modal-close" type="button" onClick={() => setTeamForm(null)}>×</button><span className="eyebrow">TEAM DETAILS</span><h2>{teamForm.id ? 'Edit team' : 'Add a team'}</h2><label>Team name<input required value={teamForm.name} onChange={(event) => setTeamForm({ ...teamForm, name: event.target.value })}/></label><label>Short name<input required maxLength="5" value={teamForm.shortName} onChange={(event) => setTeamForm({ ...teamForm, shortName: event.target.value.toUpperCase() })}/></label><button className="primary-button modal-submit" disabled={saving}>{saving ? 'Saving…' : 'Save team'}</button></form></div>}
    {playerForm && <div className="modal-backdrop" onClick={() => setPlayerForm(null)}><form className="create-modal" onClick={(event) => event.stopPropagation()} onSubmit={savePlayer}><button className="modal-close" type="button" onClick={() => setPlayerForm(null)}>×</button><span className="eyebrow">PLAYER DETAILS</span><h2>{playerForm.id ? 'Edit player' : 'Add a player'}</h2><label>Player name<input required value={playerForm.name} onChange={(event) => setPlayerForm({ ...playerForm, name: event.target.value })}/></label><label>Team<select value={playerForm.teamId} onChange={(event) => setPlayerForm({ ...playerForm, teamId: event.target.value })}>{teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}</select></label><label>Role<select value={playerForm.role || 'BATSMAN'} onChange={(event) => setPlayerForm({ ...playerForm, role: event.target.value })}>{['BATSMAN','BOWLER','ALL_ROUNDER','WICKET_KEEPER'].map((role) => <option key={role}>{role}</option>)}</select></label><label>Batting style<input placeholder="Right-hand bat" value={playerForm.battingStyle || ''} onChange={(event) => setPlayerForm({ ...playerForm, battingStyle: event.target.value })}/></label><label>Bowling style<input placeholder="Right-arm fast" value={playerForm.bowlingStyle || ''} onChange={(event) => setPlayerForm({ ...playerForm, bowlingStyle: event.target.value })}/></label><button className="primary-button modal-submit" disabled={saving}>{saving ? 'Saving…' : 'Save player'}</button></form></div>}
  </section>;
}

function LiveCentrePage({ matches, selected, details, onSelect, onScorecard }) {
  const live = matches.filter((match) => match.status === 'LIVE');
  const match = selected?.status === 'LIVE' ? selected : live[0];
  const innings = match?.currentInnings;
  return <section className="workspace-page">
    <div className="workspace-toolbar"><div><span className="eyebrow"><i className="pulse-dot"/> LIVE COVERAGE</span><h2>Live centre</h2><p>Scores and player figures refresh from the scoring database every few seconds.</p></div><span className="live-count"><Wifi size={14}/> {live.length} LIVE</span></div>
    {live.length > 1 && <div className="live-match-switcher">{live.map((item) => <button key={item.id} className={item.id === match?.id ? 'selected' : ''} onClick={() => onSelect(item, 'Live centre')}>{item.teamA.shortName} vs {item.teamB.shortName}<b>{item.currentInnings?.runs}/{item.currentInnings?.wickets}</b></button>)}</div>}
    {match ? <><div className="live-detail-card"><div className="live-detail-top"><span className="status-pill live">LIVE</span><span>{match.venue} · {match.matchType} · {match.totalOvers} overs</span></div><div className="live-detail-score"><div><span>{innings?.battingTeam?.name || match.teamA.name}</span><b>{innings ? `${innings.runs}/${innings.wickets}` : 'Yet to bat'}</b><small>{innings?.overs || '0.0'} overs · RR {innings?.runRate ?? '0.00'}</small></div><i>VS</i><div><span>{innings?.bowlingTeam?.name || match.teamB.name}</span><b>{innings ? `Target ${innings.target || '—'}` : match.status}</b><small>{match.venue}</small></div></div><div className="live-detail-players"><div><span>AT THE CREASE</span><b>✦ {innings?.striker || 'Awaiting lineup'}</b><b>{innings?.nonStriker || '—'}</b></div><div><span>CURRENT BOWLER</span><b>{innings?.bowler || 'Not selected'}</b></div><button className="secondary-button" onClick={onScorecard}>Full scorecard <ArrowUpRight size={13}/></button></div></div>
      <div className="panel live-recent-panel"><div className="panel-head"><div><span className="eyebrow">BALL BY BALL</span><h3>Recent deliveries</h3></div></div><div className="live-event-list">{(details?.events || []).map((event, index) => <div key={`${event.over}-${index}`}><span>{event.over}</span><b className={event.type === 'WICKET' ? 'event-wicket' : event.type === 'RUN' && event.runs >= 4 ? 'event-boundary' : ''}>{event.type === 'WICKET' ? 'WICKET' : event.type === 'WIDE' ? 'WIDE' : event.type === 'NO_BALL' ? 'NO BALL' : `${event.runs} run${event.runs === 1 ? '' : 's'}`}</b><small>{event.striker}</small></div>)}</div></div>
    </> : <div className="page-empty">No live matches right now. Upcoming fixtures are listed on the Matches page.</div>}
    <div className="live-match-switcher upcoming-switcher">{matches.filter((item) => item.status === 'UPCOMING').map((item) => <button key={item.id} onClick={() => onSelect(item, 'Scorer desk')}>{item.teamA.shortName} vs {item.teamB.shortName}<span>Upcoming</span></button>)}</div>
  </section>;
}

function ScorerPage({ matches, selected, details, busy, onSelect, onScore, onStart, onNotify }) {
  const innings = selected?.currentInnings;
  const live = matches.filter((match) => match.status === 'LIVE' || match.status === 'UPCOMING');
  return <section className="workspace-page scorer-workspace">
    <div className="workspace-toolbar"><div><span className="eyebrow">LIVE SCORING</span><h2>Scorer desk</h2><p>Every delivery is stored immediately and reflected on the live centre.</p></div><div className="scorer-live-indicator"><i/> DATABASE SYNC</div></div>
    <div className="page-filters"><label className="match-picker-label">SELECT FIXTURE<select value={selected?.id || ''} onChange={(event) => onSelect(matches.find((match) => String(match.id) === event.target.value), 'Scorer desk')}><option value="">Choose a match</option>{live.map((match) => <option key={match.id} value={match.id}>{match.name} · {match.status}</option>)}</select></label>{selected?.status === 'UPCOMING' && <button className="primary-button" onClick={onStart}><Wifi size={14}/> Start match</button>}</div>
    {selected && <><div className="scorer-score-card"><div><span>{innings?.battingTeam?.name || selected.teamA.name}</span><b>{innings ? `${innings.runs}/${innings.wickets}` : '—'}</b></div><div><span>OVERS</span><b>{innings?.overs || '0.0'}<small> / {selected.totalOvers}</small></b></div><div><span>RUN RATE</span><b>{innings?.runRate ?? '0.00'}</b></div><div><span>STRIKER</span><b>{innings?.striker || 'Set by opening lineup'}</b></div><div><span>BOWLER</span><b>{innings?.bowler || 'Set by opening lineup'}</b></div></div>
      <div className="scorer-controls-card"><div className="scoring-top"><span>RECORD DELIVERY</span><span>{innings?.nonStriker || '—'} <i>at non-striker’s end</i></span></div><div className="run-buttons">{[0,1,2,3,4,6].map((runs) => <button key={runs} disabled={busy || !innings || innings.status !== 'LIVE'} className={runs >= 4 ? 'boundary' : ''} onClick={() => onScore('RUN', runs)}>{runs}{runs >= 4 && <small>{runs === 4 ? 'FOUR' : 'SIX'}</small>}</button>)}</div><div className="event-buttons"><button disabled={busy || !innings || innings.status !== 'LIVE'} onClick={() => onScore('WIDE')}>WIDE</button><button disabled={busy || !innings || innings.status !== 'LIVE'} onClick={() => onScore('NO_BALL')}>NO BALL</button><button disabled={busy || !innings || innings.status !== 'LIVE'} onClick={() => onScore('BYE', 1)}>BYE</button><button disabled={busy || !innings || innings.status !== 'LIVE'} onClick={() => onScore('LEG_BYE', 1)}>LEG BYE</button><button className="wicket-button" disabled={busy || !innings || innings.status !== 'LIVE'} onClick={() => onScore('WICKET')}>WICKET</button></div></div>
      <div className="scorer-stats-grid"><StatTable title="CURRENT BATTERS" columns={['BATTER','R','B','4s','6s','SR']} rows={(details?.batting || []).filter((batter) => [innings?.striker, innings?.nonStriker].includes(batter.player)).map((batter) => [batter.player, batter.runs, batter.balls, batter.fours, batter.sixes, batter.strikeRate])}/><StatTable title="BOWLING FIGURES" columns={['BOWLER','O','R','W','ECO']} rows={(details?.bowling || []).map((bowler) => [bowler.player,bowler.overs,bowler.runs,bowler.wickets,bowler.economy])}/></div>
      <div className="panel live-recent-panel"><div className="panel-head"><div><span className="eyebrow">COMMENTARY</span><h3>Recent deliveries</h3></div></div><div className="live-event-list">{(details?.events || []).map((event,index)=><div key={`${event.over}-${index}`}><span>{event.over}</span><b>{event.type === 'WICKET' ? 'WICKET' : event.type === 'WIDE' ? 'WIDE' : event.type === 'NO_BALL' ? 'NO BALL' : `${event.runs} run${event.runs===1?'':'s'}`}</b><small>{event.striker}</small></div>)}</div></div>
    </>}{!selected && <div className="page-empty">Choose or create a match to start scoring.</div>}
    {live.length === 0 && matches.length > 0 && <p className="page-hint">There are no fixtures available to score. Create an upcoming match first.</p>}
    <button className="text-link" onClick={() => onNotify('Match, event, innings and player records are saved by the backend database.')}>How scoring updates are stored <ArrowUpRight size={13}/></button>
  </section>;
}

function ScorecardPage({ match, details }) {
  const innings = match?.currentInnings;
  return <section className="workspace-page"><div className="workspace-toolbar"><div><span className="eyebrow">MATCH SCORECARD</span><h2>{match?.name || 'Select a match'}</h2><p>{match?.venue || 'Choose a fixture from Matches or Live centre.'}</p></div></div>{innings && <><div className="scorer-score-card"><div><span>{innings.battingTeam.name}</span><b>{innings.runs}/{innings.wickets}</b></div><div><span>OVERS</span><b>{innings.overs} <small>/ {match.totalOvers}</small></b></div><div><span>RUN RATE</span><b>{innings.runRate}</b></div><div><span>STATUS</span><b>{match.status}</b></div></div><div className="scorer-stats-grid"><StatTable title="BATTING" columns={['BATTER','R','B','4s','6s','SR']} rows={(details?.batting || []).map((batter) => [batter.player,batter.runs,batter.balls,batter.fours,batter.sixes,batter.strikeRate])}/><StatTable title="BOWLING" columns={['BOWLER','O','R','W','ECO']} rows={(details?.bowling || []).map((bowler) => [bowler.player,bowler.overs,bowler.runs,bowler.wickets,bowler.economy])}/></div></>}</section>;
}

function StatTable({ title, columns, rows }) { return <div className="panel compact-stat-table"><div className="panel-head"><div><span className="eyebrow">{title}</span></div></div><div className="table-wrap"><table><thead><tr>{columns.map((column)=><th key={column}>{column}</th>)}</tr></thead><tbody>{rows.map((row,index)=><tr key={`${row[0]}-${index}`}>{row.map((cell,column)=><td key={`${index}-${column}`}>{cell}</td>)}</tr>)}{rows.length===0&&<tr><td className="empty-row" colSpan={columns.length}>Stats appear after deliveries are recorded.</td></tr>}</tbody></table></div></div>; }
