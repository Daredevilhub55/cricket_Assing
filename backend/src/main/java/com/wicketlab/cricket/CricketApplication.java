package com.wicketlab.cricket;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.stream.*;

@SpringBootApplication
public class CricketApplication {
    public static void main(String[] args) { SpringApplication.run(CricketApplication.class, args); }

    @Bean CommandLineRunner seed(TeamRepo teams, PlayerRepo players, MatchRepo matches, InningsRepo innings, EventRepo events) {
        return args -> {
            if (matches.count() > 0) return;
            String[][] squads = {
                {"India","IND","Rohit Sharma","Virat Kohli","Suryakumar Yadav","Hardik Pandya","Ravindra Jadeja","Jasprit Bumrah","Arshdeep Singh"},
                {"Australia","AUS","David Warner","Travis Head","Steve Smith","Glenn Maxwell","Mitchell Marsh","Pat Cummins","Mitchell Starc"},
                {"England","ENG","Jos Buttler","Phil Salt","Harry Brook","Ben Stokes","Liam Livingstone","Jofra Archer","Adil Rashid"},
                {"Pakistan","PAK","Babar Azam","Mohammad Rizwan","Fakhar Zaman","Shadab Khan","Shaheen Afridi","Naseem Shah","Haris Rauf"},
                {"South Africa","SA","Quinton de Kock","Aiden Markram","Heinrich Klaasen","David Miller","Marco Jansen","Kagiso Rabada","Anrich Nortje"},
                {"New Zealand","NZ","Kane Williamson","Devon Conway","Glenn Phillips","Daryl Mitchell","Rachin Ravindra","Trent Boult","Lockie Ferguson"}
            };
            Map<String,Team> ts = new HashMap<>(); Map<String,List<Player>> ps = new HashMap<>();
            for (String[] s : squads) {
                Team t = teams.save(new Team(s[0],s[1])); ts.put(s[1],t); List<Player> roster = new ArrayList<>();
                for(int i=2;i<s.length;i++) roster.add(players.save(new Player(s[i], t, i==7?"BOWLER":"ALL_ROUNDER")));
                ps.put(s[1],roster);
            }
            Match india = createSeed(matches, innings, "India vs Australia", "Wankhede Stadium, Mumbai", "IND", "AUS", ts, ps, "LIVE", 20, 1);
            Innings first=innings.findByMatchIdAndNumber(india.id,1).orElseThrow();
            int[] sampleRuns={1,0,4,1,2,0, 6,1,1,0,4,1, 0,2,1,4,0,1, 1,1,6,0,2,1, 4,0,1,2,1,1, 0,4,1,0,2,1, 1,0,0,6,1,1, 2,1,0,4,1,0, 0,1,4,1,0,2, 1,6,0,1,2,1, 0,1,1,4,0,2, 1,0,6,1,0,1, 4,1,0,2,1,0, 1,4,0,1,2,1, 0,1,1,0,4,1, 1,0,2,1,0,4, 1,1,0,6,0,1, 2,0,1,4,0,1};
            for(int i=0;i<sampleRuns.length;i++) {
                int run=sampleRuns[i]; String type="RUN"; int batter=run;
                if(run==0 && i==5){type="WICKET"; batter=0;} else if(run==0 && i==42){type="WIDE";batter=0;}
                events.save(new BallEvent(first, i/6+1, i%6+1, ps.get("IND").get(Math.min(i/12,4)).name, ps.get("AUS").get(6).name,batter,type.equals("WIDE")?1:run,type));
                first.runs+= type.equals("WIDE")?1:run; if(type.equals("WICKET")) first.wickets++;
                if(!type.equals("WIDE")) first.legalBalls++;
            }
            first.striker=ps.get("IND").get(4).name; first.nonStriker=ps.get("IND").get(3).name; first.bowler=ps.get("AUS").get(4).name; innings.save(first);
            createSeed(matches,innings,"England vs Pakistan","Lord’s, London","ENG","PAK",ts,ps,"LIVE",20,2);
            createSeed(matches,innings,"South Africa vs New Zealand","Newlands, Cape Town","SA","NZ",ts,ps,"UPCOMING",20,0);
        };
    }
    private Match createSeed(MatchRepo repo, InningsRepo ir, String title, String venue, String a,String b,Map<String,Team> ts,Map<String,List<Player>> ps,String status,int overs,int live) {
        Match m=repo.save(new Match(title,venue,ts.get(a),ts.get(b),status,overs));
        if(live==0)return m;
        Innings in=ir.save(new Innings(m,1,ts.get(a),ts.get(b),overs)); in.status="LIVE";
        in.striker=ps.get(a).get(0).name;in.nonStriker=ps.get(a).get(1).name;in.bowler=ps.get(b).get(5).name;
        if(live==2){in.runs=142;in.wickets=3;in.legalBalls=94;in.striker=ps.get(b).get(1).name;in.nonStriker=ps.get(b).get(2).name;}
        ir.save(in); return m;
    }
}

@Entity @Table(name="teams") class Team {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false) String name; @Column(nullable=false,unique=true) String shortName;
    protected Team(){} Team(String name,String shortName){this.name=name;this.shortName=shortName;}
}
@Entity @Table(name="players") class Player {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(nullable=false) String name;
    String role,battingStyle,bowlingStyle,nationality; Integer jerseyNumber; @ManyToOne(fetch=FetchType.EAGER) Team team;
    protected Player(){} Player(String name,Team team,String role){this(name,team,role,null,null);}
    Player(String name,Team team,String role,String battingStyle,String bowlingStyle){this.name=name;this.team=team;this.role=role;this.battingStyle=battingStyle;this.bowlingStyle=bowlingStyle;}
}
@Entity @Table(name="matches") class Match {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(nullable=false) String name,venue,status;
    int totalOvers; String matchType="T20"; LocalDateTime createdAt=LocalDateTime.now();
    @ManyToOne(fetch=FetchType.EAGER) Team teamA; @ManyToOne(fetch=FetchType.EAGER) Team teamB;
    @OneToMany(mappedBy="match",cascade=CascadeType.ALL,orphanRemoval=true) List<Innings> innings=new ArrayList<>();
    protected Match(){} Match(String name,String venue,Team a,Team b,String status,int overs){this.name=name;this.venue=venue;teamA=a;teamB=b;this.status=status;totalOvers=overs;}
}
@Entity @Table(name="innings") class Innings {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @ManyToOne Match match;
    int number,runs=0,wickets=0,legalBalls=0,totalOvers; String status="UPCOMING",striker,nonStriker,bowler;
    @ManyToOne Team battingTeam; @ManyToOne Team bowlingTeam;
    @OneToMany(mappedBy="innings",cascade=CascadeType.ALL,orphanRemoval=true) List<BallEvent> events=new ArrayList<>();
    protected Innings(){} Innings(Match m,int n,Team bat,Team bowl,int overs){match=m;number=n;battingTeam=bat;bowlingTeam=bowl;totalOvers=overs;}
}
@Entity @Table(name="ball_events") class BallEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @ManyToOne Innings innings;
    int overNumber,ballNumber,runs; Integer teamRuns; String striker,bowler,type,wicketType,dismissedPlayer; LocalDateTime createdAt=LocalDateTime.now();
    protected BallEvent(){} BallEvent(Innings in,int over,int ball,String striker,String bowler,int runs,String type){this(in,over,ball,striker,bowler,runs,runs,type);}
    BallEvent(Innings in,int over,int ball,String striker,String bowler,int runs,int teamRuns,String type){innings=in;overNumber=over;ballNumber=ball;this.striker=striker;this.bowler=bowler;this.runs=runs;this.teamRuns=teamRuns;this.type=type;}
}
interface TeamRepo extends JpaRepository<Team,Long>{}
interface PlayerRepo extends JpaRepository<Player,Long>{List<Player> findByTeamId(Long id);}
interface MatchRepo extends JpaRepository<Match,Long>{boolean existsByTeamAIdOrTeamBId(Long teamAId,Long teamBId);}
interface InningsRepo extends JpaRepository<Innings,Long>{Optional<Innings> findByMatchIdAndNumber(Long id,int n);List<Innings> findByMatchIdOrderByNumber(Long id);}
interface EventRepo extends JpaRepository<BallEvent,Long>{List<BallEvent> findTop18ByInningsIdOrderByIdDesc(Long id);List<BallEvent> findByInningsIdOrderByIdAsc(Long id);}

record BallRequest(@Min(0) @Max(6) int runs, @Pattern(regexp="RUN|WIDE|NO_BALL|BYE|LEG_BYE|WICKET") String type, String striker, String bowler, String wicketType, String dismissedPlayer){}
@Service class CricketService {
    private final MatchRepo matches;private final TeamRepo teams;private final PlayerRepo players;private final InningsRepo innings;private final EventRepo events;
    CricketService(MatchRepo m,TeamRepo t,PlayerRepo p,InningsRepo i,EventRepo e){matches=m;teams=t;players=p;innings=i;events=e;}
    Map<String,Object> team(Team t){return Map.of("id",t.id,"name",t.name,"shortName",t.shortName);}
    Map<String,Object> match(Match m){List<Innings> ins=innings.findByMatchIdOrderByNumber(m.id);Innings current=ins.isEmpty()?null:ins.get(ins.size()-1);Map<String,Object> out=new LinkedHashMap<>();out.put("id",m.id);out.put("name",m.name);out.put("venue",m.venue);out.put("status",m.status);out.put("matchType",m.matchType);out.put("totalOvers",m.totalOvers);out.put("createdAt",m.createdAt);out.put("teamA",team(m.teamA));out.put("teamB",team(m.teamB));out.put("innings",ins.stream().map(this::score).toList());out.put("currentInnings",current==null?null:score(current));return out;}
    Map<String,Object> score(Innings i){Map<String,Object> x=new LinkedHashMap<>();x.put("id",i.id);x.put("number",i.number);x.put("battingTeam",team(i.battingTeam));x.put("bowlingTeam",team(i.bowlingTeam));x.put("runs",i.runs);x.put("wickets",i.wickets);x.put("legalBalls",i.legalBalls);x.put("overs",i.legalBalls/6+"."+i.legalBalls%6);x.put("runRate",i.legalBalls==0?0:Math.round(i.runs*600.0/i.legalBalls)/100.0);List<BallEvent> deliveries=events.findByInningsIdOrderByIdAsc(i.id);int stand=0;for(int at=deliveries.size()-1;at>=0;at--){BallEvent event=deliveries.get(at);if("WICKET".equals(event.type))break;stand+=event.teamRuns==null?event.runs:event.teamRuns;}x.put("partnershipRuns",stand);int target=i.number==2?innings.findByMatchIdAndNumber(i.match.id,1).map(a->a.runs+1).orElse(0):0;int required=Math.max(0,target-i.runs), ballsRemaining=Math.max(0,i.totalOvers*6-i.legalBalls);x.put("target",target==0?null:target);x.put("requiredRuns",target==0?null:required);x.put("requiredRunRate",target==0||ballsRemaining==0?null:Math.round(required*600.0/ballsRemaining)/100.0);x.put("status",i.status);x.put("striker",i.striker);x.put("nonStriker",i.nonStriker);x.put("bowler",i.bowler);return x;}
    Innings findInnings(Long id){return innings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Innings not found"));}
    @Transactional Map<String,Object> ball(Long id,BallRequest req){Innings i=findInnings(id);if(!"LIVE".equals(i.status))throw new ResponseStatusException(HttpStatus.CONFLICT,"Innings is not live");String striker=req.striker()==null?i.striker:req.striker();String bowler=req.bowler()==null?i.bowler:req.bowler();if(striker==null||bowler==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Select a striker and bowler before scoring");String type=req.type();String dismissed=type.equals("WICKET")?Objects.toString(req.dismissedPlayer(),striker):null;if(type.equals("WICKET")&&events.findByInningsIdOrderByIdAsc(id).stream().anyMatch(e->dismissed.equals(e.dismissedPlayer)))throw new ResponseStatusException(HttpStatus.CONFLICT,"That player has already been dismissed");int bat=(type.equals("RUN")||type.equals("NO_BALL")?req.runs():0);int total=bat+(type.equals("WIDE")||type.equals("NO_BALL")?1:0)+(type.equals("BYE")||type.equals("LEG_BYE")?req.runs():0);int legal=!(type.equals("WIDE")||type.equals("NO_BALL"))?1:0;int over=i.legalBalls/6+1, ball=i.legalBalls%6+1;
        BallEvent event=new BallEvent(i,over,ball,striker,bowler,bat,total,type);event.dismissedPlayer=dismissed;event.wicketType=type.equals("WICKET")?Objects.toString(req.wicketType(),"BOWLED"):null;events.save(event);i.runs+=total;i.legalBalls+=legal;int rotation=type.equals("WIDE")?total:bat+(type.equals("BYE")||type.equals("LEG_BYE")?req.runs():0);if(rotation%2==1){String hold=i.striker;i.striker=i.nonStriker;i.nonStriker=hold;}if(type.equals("WICKET")){i.wickets++;List<String> out=events.findByInningsIdOrderByIdAsc(id).stream().map(e->e.dismissedPlayer).filter(Objects::nonNull).toList();String next=players.findByTeamId(i.battingTeam.id).stream().map(p->p.name).filter(n->!n.equals(i.striker)&&!n.equals(i.nonStriker)&&!out.contains(n)).findFirst().orElse(null);if(dismissed.equals(i.nonStriker))i.nonStriker=next;else i.striker=next;}if(i.legalBalls%6==0){String hold=i.striker;i.striker=i.nonStriker;i.nonStriker=hold;}if(i.legalBalls>=i.totalOvers*6||i.wickets>=10)i.status="COMPLETED";innings.save(i);return score(i);}
    Map<String,Object> scorecard(Long id){Innings i=findInnings(id);List<BallEvent> all=events.findByInningsIdOrderByIdAsc(id);Map<String,Map<String,Object>> batting=new LinkedHashMap<>(),bowling=new LinkedHashMap<>();Set<String> bowlerWickets=Set.of("BOWLED","CAUGHT","LBW","STUMPED","HIT_WICKET");for(BallEvent e:all){int teamRuns=e.teamRuns==null?e.runs:e.teamRuns;Map<String,Object>b=batting.computeIfAbsent(e.striker,k->{Map<String,Object> z=new LinkedHashMap<>();z.put("player",k);z.put("runs",0);z.put("balls",0);z.put("fours",0);z.put("sixes",0);return z;});if(e.type.equals("RUN")||e.type.equals("NO_BALL")){b.put("runs",(int)b.get("runs")+e.runs);b.put("balls",(int)b.get("balls")+1);if(e.runs==4)b.put("fours",(int)b.get("fours")+1);if(e.runs==6)b.put("sixes",(int)b.get("sixes")+1);}Map<String,Object> q=bowling.computeIfAbsent(e.bowler,k->{Map<String,Object> z=new LinkedHashMap<>();z.put("player",k);z.put("runs",0);z.put("wickets",0);z.put("legalBalls",0);return z;});if(!e.type.equals("BYE")&&!e.type.equals("LEG_BYE")){q.put("runs",(int)q.get("runs")+teamRuns);if(e.type.equals("WICKET")&&bowlerWickets.contains(e.wicketType))q.put("wickets",(int)q.get("wickets")+1);}if(!e.type.equals("WIDE")&&!e.type.equals("NO_BALL"))q.put("legalBalls",(int)q.get("legalBalls")+1);}
        batting.values().forEach(b->{int balls=(int)b.get("balls"),runs=(int)b.get("runs");b.put("strikeRate",balls==0?0:Math.round(runs*10000.0/balls)/100.0);});bowling.values().forEach(b->{int balls=(int)b.get("legalBalls"),runs=(int)b.get("runs");b.put("overs",balls/6+"."+balls%6);b.put("economy",balls==0?0:Math.round(runs*600.0/balls)/100.0);});return Map.of("score",score(i),"batting",batting.values(),"bowling",bowling.values(),"events",events.findTop18ByInningsIdOrderByIdDesc(id).stream().map(e->Map.of("over",e.overNumber+"."+e.ballNumber,"runs",e.teamRuns==null?e.runs:e.teamRuns,"type",e.type,"striker",e.striker)).toList());}
}

@RestController @RequestMapping("/api") @CrossOrigin(origins="*") class CricketController {
    private final CricketService svc;private final MatchRepo matches;private final TeamRepo teams;private final PlayerRepo players;private final InningsRepo innings;private final EventRepo events;
    CricketController(CricketService s,MatchRepo m,TeamRepo t,PlayerRepo p,InningsRepo i,EventRepo e){svc=s;matches=m;teams=t;players=p;innings=i;events=e;}
    @GetMapping("/matches") List<Map<String,Object>> matches(){return matches.findAll().stream().map(svc::match).toList();}
    @GetMapping("/matches/{id}") Map<String,Object> match(@PathVariable Long id){return svc.match(matches.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Match not found")));}
    @PostMapping("/matches") ResponseEntity<Map<String,Object>> create(@RequestBody Map<String,Object> b){Team a=teams.findById(Long.valueOf(b.get("teamA").toString())).orElseThrow(),c=teams.findById(Long.valueOf(b.get("teamB").toString())).orElseThrow();if(a.id.equals(c.id))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose two different teams");Match m=matches.save(new Match(b.get("name").toString(),Objects.toString(b.get("venue"),"TBC"),a,c,"UPCOMING",Integer.parseInt(Objects.toString(b.get("totalOvers"),"20"))));return ResponseEntity.status(201).body(svc.match(m));}
    @PutMapping("/matches/{id}") Map<String,Object> updateMatch(@PathVariable Long id,@RequestBody Map<String,Object>b){Match m=matches.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Match not found"));if(b.containsKey("name"))m.name=b.get("name").toString();if(b.containsKey("venue"))m.venue=b.get("venue").toString();if(b.containsKey("totalOvers"))m.totalOvers=Integer.parseInt(b.get("totalOvers").toString());if(b.containsKey("teamA"))m.teamA=teams.findById(Long.valueOf(b.get("teamA").toString())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Team not found"));if(b.containsKey("teamB"))m.teamB=teams.findById(Long.valueOf(b.get("teamB").toString())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Team not found"));if(m.teamA.id.equals(m.teamB.id))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose two different teams");return svc.match(matches.save(m));}
    @DeleteMapping("/matches/{id}") ResponseEntity<Void> delete(@PathVariable Long id){if(!matches.existsById(id))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Match not found");matches.deleteById(id);return ResponseEntity.noContent().build();}
    @PostMapping("/matches/{id}/start") Map<String,Object> start(@PathVariable Long id){Match m=matches.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Match not found"));m.status="LIVE";matches.save(m);if(innings.findByMatchIdOrderByNumber(id).isEmpty()){Innings in=new Innings(m,1,m.teamA,m.teamB,m.totalOvers);List<Player> batters=players.findByTeamId(m.teamA.id),bowlers=players.findByTeamId(m.teamB.id);if(!batters.isEmpty())in.striker=batters.get(0).name;if(batters.size()>1)in.nonStriker=batters.get(1).name;if(!bowlers.isEmpty())in.bowler=bowlers.get(0).name;in.status="LIVE";innings.save(in);}return svc.match(m);}
    @GetMapping("/teams") List<Map<String,Object>> teams(){return teams.findAll().stream().map(t->Map.of("id",t.id,"name",t.name,"shortName",t.shortName,"players",players.findByTeamId(t.id).stream().map(this::playerDto).toList())).toList();}
    @GetMapping("/teams/{id}") Map<String,Object> getTeam(@PathVariable Long id){Team t=teams.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Team not found"));return Map.of("id",t.id,"name",t.name,"shortName",t.shortName,"players",players.findByTeamId(id).stream().map(this::playerDto).toList());}
    @PostMapping("/teams") ResponseEntity<?> team(@RequestBody Map<String,String>b){Team saved=teams.save(new Team(b.get("name"),b.get("shortName")));return ResponseEntity.status(201).body(Map.of("id",saved.id,"name",saved.name,"shortName",saved.shortName));}
    @PutMapping("/teams/{id}") Map<String,Object> updateTeam(@PathVariable Long id,@RequestBody Map<String,String>b){Team t=teams.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Team not found"));if(b.containsKey("name"))t.name=b.get("name");if(b.containsKey("shortName"))t.shortName=b.get("shortName");teams.save(t);return Map.of("id",t.id,"name",t.name,"shortName",t.shortName);}
    @DeleteMapping("/teams/{id}") ResponseEntity<Void> deleteTeam(@PathVariable Long id){Team t=teams.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Team not found"));if(!players.findByTeamId(id).isEmpty()||matches.existsByTeamAIdOrTeamBId(id,id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Remove the team’s players and fixtures before deleting it");teams.delete(t);return ResponseEntity.noContent().build();}
    Map<String,Object> playerDto(Player p){Map<String,Object> dto=new LinkedHashMap<>();dto.put("id",p.id);dto.put("name",p.name);dto.put("role",p.role);dto.put("battingStyle",p.battingStyle);dto.put("bowlingStyle",p.bowlingStyle);dto.put("nationality",p.nationality);dto.put("jerseyNumber",p.jerseyNumber);return dto;}
    Map<String,Object> playerWithTeam(Player p){Map<String,Object> dto=playerDto(p);dto.put("team",Map.of("id",p.team.id,"name",p.team.name,"shortName",p.team.shortName));return dto;}
    @GetMapping("/players") List<Map<String,Object>> players(){return players.findAll().stream().map(this::playerWithTeam).toList();}
    @GetMapping("/players/{id}") Map<String,Object> getPlayer(@PathVariable Long id){return playerWithTeam(players.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Player not found")));}
    @PostMapping("/players") ResponseEntity<?> player(@RequestBody Map<String,Object>b){Team t=teams.findById(Long.valueOf(b.get("teamId").toString())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Team not found"));Player p=players.save(new Player(b.get("name").toString(),t,Objects.toString(b.get("role"),"BATSMAN"),Objects.toString(b.get("battingStyle"),null),Objects.toString(b.get("bowlingStyle"),null)));return ResponseEntity.status(201).body(playerWithTeam(p));}
    @PutMapping("/players/{id}") Map<String,Object> updatePlayer(@PathVariable Long id,@RequestBody Map<String,Object>b){Player p=players.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Player not found"));if(b.containsKey("name"))p.name=b.get("name").toString();if(b.containsKey("role"))p.role=b.get("role").toString();if(b.containsKey("battingStyle"))p.battingStyle=Objects.toString(b.get("battingStyle"),null);if(b.containsKey("bowlingStyle"))p.bowlingStyle=Objects.toString(b.get("bowlingStyle"),null);if(b.containsKey("nationality"))p.nationality=Objects.toString(b.get("nationality"),null);if(b.containsKey("jerseyNumber"))p.jerseyNumber=Integer.valueOf(b.get("jerseyNumber").toString());if(b.containsKey("teamId"))p.team=teams.findById(Long.valueOf(b.get("teamId").toString())).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Team not found"));return playerWithTeam(players.save(p));}
    @DeleteMapping("/players/{id}") ResponseEntity<Void> deletePlayer(@PathVariable Long id){Player p=players.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Player not found"));players.delete(p);return ResponseEntity.noContent().build();}
    @PostMapping("/matches/{id}/innings") Map<String,Object> newInnings(@PathVariable Long id,@RequestBody Map<String,Object>b){Match m=matches.findById(id).orElseThrow();Team bat=teams.findById(Long.valueOf(b.get("battingTeamId").toString())).orElseThrow();Team bowl=bat.id.equals(m.teamA.id)?m.teamB:m.teamA;Innings i=innings.save(new Innings(m,innings.findByMatchIdOrderByNumber(id).size()+1,bat,bowl,m.totalOvers));i.status="LIVE";i.striker=Objects.toString(b.get("striker"),null);i.nonStriker=Objects.toString(b.get("nonStriker"),null);i.bowler=Objects.toString(b.get("bowler"),null);innings.save(i);m.status="LIVE";matches.save(m);return svc.score(i);}
    @PostMapping("/innings/{id}/ball") Map<String,Object> ball(@PathVariable Long id,@Valid @RequestBody BallRequest request){return svc.ball(id,request);}
    @GetMapping("/innings/{id}/score") Map<String,Object> score(@PathVariable Long id){return svc.score(innings.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Innings not found")));}
    @GetMapping("/innings/{id}/scorecard") Map<String,Object> card(@PathVariable Long id){return svc.scorecard(id);}
    @GetMapping("/innings/{id}/events") List<Map<String,Object>> events(@PathVariable Long id){return events.findByInningsIdOrderByIdAsc(id).stream().map(e->{Map<String,Object> row=new LinkedHashMap<>();row.put("id",e.id);row.put("over",e.overNumber+"."+e.ballNumber);row.put("runs",e.teamRuns==null?e.runs:e.teamRuns);row.put("batRuns",e.runs);row.put("type",e.type);row.put("striker",e.striker);row.put("bowler",e.bowler);row.put("timestamp",e.createdAt);return row;}).toList();}
    @PostMapping("/innings/{id}/complete") Map<String,Object> complete(@PathVariable Long id){Innings i=innings.findById(id).orElseThrow();i.status="COMPLETED";return svc.score(innings.save(i));}
    @PostMapping("/matches/{id}/finish") Map<String,Object> finish(@PathVariable Long id){Match m=matches.findById(id).orElseThrow();m.status="COMPLETED";return svc.match(matches.save(m));}
}
