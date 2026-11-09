import model.*;
import util.CsvDataLoader;
import util.CsvWriter;

import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * PS-045 command-line engine. Commands are processed in physical file order.
 * The source CSV loader and domain model classes are reused without modification.
 */
public class Main {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter OUT_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, Ticket> tickets = new LinkedHashMap<>();
    private static final Map<String, Trip> trips = new LinkedHashMap<>();
    private static final Map<String, Partner> partners = new LinkedHashMap<>();
    private static final List<String[]> commandResults = new ArrayList<>();
    private static final List<String[]> chartRows = new ArrayList<>();
    private static final List<String[]> settlementRows = new ArrayList<>();
    private static final List<String> deferredTicketPrints = new ArrayList<>();

    private static class Pax {
        String no, name, seat, tripId;
        Passenger.Gender gender;
        int age, board, drop;
        boolean cancelled;
        double stageFare, dynamicPct, dynamicAmount, premium, childPct, taxable, gst, total;
        String status = "BOOKED";
        Pax(String no, String name, Passenger.Gender gender, int age, String seat, int board, int drop) {
            this.no=no; this.name=name; this.gender=gender; this.age=age; this.seat=seat; this.board=board; this.drop=drop;
        }
        boolean freeChild() { return age < 5; }
    }
    private static class Ticket {
        String id, tripId, channel, partnerId;
        LocalDateTime bookedAt;
        List<Pax> passengers = new ArrayList<>();
        int rescheduleCount;
        boolean rescheduled;
        Ticket(String id,String tripId,LocalDateTime bookedAt,String channel,String partnerId) {
            this.id=id; this.tripId=tripId; this.bookedAt=bookedAt; this.channel=channel; this.partnerId=partnerId;
        }
        long activeCount() { return passengers.stream().filter(p -> !p.cancelled).count(); }
    }
    private static class Result {
        String status="OK", reason="";
        double refund, fee, fareDifference, credit;
    }

    public static void main(String[] args) {
        Path sampleDir;
        Path commandFile;
        Path outputDir;
        if (args.length == 0) {
            sampleDir = Path.of("input/T062-PS-045-sample-data");
            commandFile = sampleDir.resolve("commands.txt");
            outputDir = Path.of("output");
        } else if (args.length == 1 && args[0].toLowerCase(Locale.ROOT).endsWith("commands.txt")) {
            commandFile = Path.of(args[0]);
            sampleDir = commandFile.getParent();
            outputDir = Path.of("output");
        } else {
            sampleDir = Path.of(args[0]);
            commandFile = args.length > 1 ? Path.of(args[1]) : sampleDir.resolve("commands.txt");
            outputDir = args.length > 2 ? Path.of(args[2]) : Path.of("output");
        }
        try {
            CsvDataLoader.Data data = new CsvDataLoader().loadAll(sampleDir.toString());
            trips.putAll(data.trips);
            partners.putAll(data.partners);
            List<String> lines = Files.readAllLines(commandFile);
            for (int i=0; i<lines.size(); i++) {
                String line=lines.get(i).trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] p=line.split("\\|",-1);
                Result r=new Result();
                String command=p[0].trim().toUpperCase(Locale.ROOT);
                String ref=p.length>1?p[1].trim():"";
                try { execute(command,p,r); }
                catch (RuntimeException ex) { r.status="REJECTED"; r.reason=reason(ex.getMessage()); }
                commandResults.add(new String[]{String.valueOf(i+1),command,ref,r.status,r.reason,money(r.refund),money(r.fee),money(r.fareDifference),money(r.credit)});
            }
            // The specification says PRINT_TICKET reflects the state at end-of-file.
            for (String ticketId : deferredTicketPrints) printTicketNow(ticketId);
            writeOutputs(outputDir);
            System.out.println("Command processing completed.");
            System.out.println("Tickets: "+tickets.size());
            System.out.println("Command results: "+commandResults.size());
            System.out.println("Output folder: "+outputDir.toAbsolutePath());
            System.out.println("Generated tickets.csv, command_results.csv, trip_chart.csv and partner_settlement.csv");
        } catch (Exception e) {
            System.err.println("FATAL: "+e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void execute(String cmd,String[] p,Result r) {
        switch(cmd) {
            case "BOOK": book(p); break;
            case "CANCEL": cancel(p,r); break;
            case "RESCHEDULE": reschedule(p,r); break;
            case "CHANGE_BOARDING": changeBoarding(p,r); break;
            case "OPERATOR_CANCEL": operatorCancel(p,r); break;
            case "PRINT_TICKET": printTicket(p); break;
            case "PRINT_CHART": printChart(p); break;
            case "PRINT_SETTLEMENT": printSettlement(p); break;
            default: throw new IllegalArgumentException("UNKNOWN_COMMAND");
        }
    }

    private static void require(boolean ok,String code) { if(!ok) throw new IllegalArgumentException(code); }
    private static String reason(String message) {
        if(message==null || message.isBlank()) return "INVALID_COMMAND";
        String m=message.trim().toUpperCase(Locale.ROOT).replace(' ','_').replace('-','_');
        if(m.contains(":")) m=m.substring(0,m.indexOf(':'));
        return m.length()>40?m.substring(0,40):m;
    }
    private static String[] fields(String[] p,int count) { require(p.length==count,"INVALID_ARGUMENT_COUNT"); return p; }
    private static LocalDateTime parseTs(String s) { return LocalDateTime.parse(s.trim(),TS); }
    private static double round(double x) { return Math.round(x*100.0)/100.0; }
    private static String money(double x) { return String.format(Locale.US,"%.2f",round(x)); }
    private static Trip trip(String id) { Trip t=trips.get(id); require(t!=null,"TRIP_NOT_FOUND"); return t; }
    private static Ticket ticket(String id) { Ticket t=tickets.get(id); require(t!=null,"TICKET_NOT_FOUND"); return t; }
    private static int stopIndex(Trip t,String stopId) { return t.getRoute().findStopIndex(stopId); }
    private static LocalDateTime boardTime(Trip t,int index) { return t.getBoardingTime(index); }
    private static boolean overlap(int a,int b,int c,int d) { return a<d && c<b; }

    private static void book(String[] p) {
        fields(p,9);
        String id=p[1].trim(), tripId=p[2].trim();
        require(!id.isEmpty() && !tickets.containsKey(id),"DUPLICATE_TICKET");
        Trip t=trip(tripId); require(!t.isCancelled(),"TRIP_CANCELLED");
        LocalDateTime booked=parseTs(p[3]);
        String channel=p[4].trim().toUpperCase(Locale.ROOT), partnerId=p[5].trim();
        require(channel.equals("COUNTER")||channel.equals("PARTNER"),"INVALID_CHANNEL");
        if(channel.equals("PARTNER")) require(partners.containsKey(partnerId),"PARTNER_NOT_FOUND");
        else require(partnerId.isEmpty(),"COUNTER_PARTNER_NOT_ALLOWED");
        int b=stopIndex(t,p[6].trim()), d=stopIndex(t,p[7].trim());
        require(b>=0 && d>=0 && b<d,"INVALID_JOURNEY_STOPS");
        String[] specs=p[8].split(";",-1); require(specs.length>0,"NO_PASSENGERS");
        Ticket candidate=new Ticket(id,tripId,booked,channel,partnerId);
        Set<String> paxNos=new HashSet<>();
        int adultCount=0, freeChildCount=0;
        for(String spec:specs) {
            String[] a=spec.split(":",-1); require(a.length==5,"INVALID_PASSENGER_FORMAT");
            String no=a[0].trim(), name=a[1].trim(); require(!no.isEmpty()&&!name.isEmpty()&&paxNos.add(no),"DUPLICATE_PAX_NO");
            String genderCode = a[2].trim().toUpperCase(Locale.ROOT);
            require(genderCode.equals("M") || genderCode.equals("F"), "INVALID_GENDER");
            Passenger.Gender g = genderCode.equals("M") ? Passenger.Gender.MALE : Passenger.Gender.FEMALE;
            int age=Integer.parseInt(a[3].trim()); require(age>=0,"INVALID_AGE");
            String seat=a[4].trim();
            if(age<5) { require(seat.equalsIgnoreCase("NOSEAT"),"FREE_CHILD_NOSEAT_REQUIRED"); freeChildCount++; }
            else { require(!seat.equalsIgnoreCase("NOSEAT")&&!seat.isEmpty(),"SEAT_REQUIRED"); }
            if(age>=13) adultCount++;
            Pax pax = new Pax(no,name,g,age,seat,b,d);
            pax.tripId = tripId;
            candidate.passengers.add(pax);
        }
        require(freeChildCount<=adultCount,"CHILD_WITHOUT_ADULT");
        require(freeChildCount<=adultCount,"MAX_ONE_FREE_CHILD_PER_ADULT");
        validateAllSeats(t,candidate,null);
        // Dynamic pricing uses occupancy immediately before this booking.
        int occ=maximumOccupancy(t,null);
        double pct=occ*1.0/t.getBus().getTotalSeats()>=0.90?30:occ*1.0/t.getBus().getTotalSeats()>=0.70?15:0;
        for(Pax pax:candidate.passengers) calculateFare(t,pax,pct);
        tickets.put(id,candidate);
    }

    private static void validateAllSeats(Trip t,Ticket candidate,Ticket ignored) {
        Set<String> local=new HashSet<>();
        for(Pax pax:candidate.passengers) {
            if(pax.cancelled||pax.freeChild()) continue;
            Seat s=t.getBus().findSeat(pax.seat); require(s!=null,"INVALID_SEAT");
            if(s.isLadiesOnly()) require(pax.gender==Passenger.Gender.FEMALE,"LADIES_SEAT");
            for(Ticket other:tickets.values()) {
                if(other==ignored || !other.tripId.equals(t.getTripId())) continue;
                for(Pax q:other.passengers) {
                    if(q.cancelled||q.freeChild()||!q.seat.equalsIgnoreCase(pax.seat)) continue;
                    require(!overlap(pax.board,pax.drop,q.board,q.drop),"SEAT_OVERLAP");
                }
            }
            for(Pax q:candidate.passengers) {
                if(q==pax||q.cancelled||q.freeChild()) continue;
                if(q.seat.equalsIgnoreCase(pax.seat))
                    require(!overlap(pax.board,pax.drop,q.board,q.drop),"SEAT_OVERLAP");
            }
            // Adjacent-berth protection is symmetric: either booking may arrive first.
            for (Ticket other : tickets.values()) {
                if (other == ignored || !other.tripId.equals(t.getTripId())) continue;
                for (Pax q : other.passengers) {
                    if (q.cancelled || q.freeChild() || !overlap(pax.board,pax.drop,q.board,q.drop)) continue;
                    Seat otherSeat = t.getBus().findSeat(q.seat);
                    if (otherSeat == null) continue;
                    boolean adjacentPair = pax.seat.equalsIgnoreCase(otherSeat.getAdjacentSeatId())
                            || q.seat.equalsIgnoreCase(s.getAdjacentSeatId());
                    if (!adjacentPair) continue;
                    if (pax.gender == Passenger.Gender.MALE && q.gender == Passenger.Gender.FEMALE
                            && isSoloFemale(other, q)) throw new IllegalArgumentException("ADJACENT_FEMALE");
                    if (pax.gender == Passenger.Gender.FEMALE && q.gender == Passenger.Gender.MALE
                            && isSoloFemale(candidate, pax)) throw new IllegalArgumentException("ADJACENT_FEMALE");
                }
            }
            local.add(pax.seat.toUpperCase(Locale.ROOT));
        }
    }

    private static boolean isSoloFemale(Ticket booking, Pax female) {
        long activeFemales = booking.passengers.stream()
                .filter(p -> !p.cancelled && !p.freeChild() && p.gender == Passenger.Gender.FEMALE)
                .count();
        return activeFemales == 1 && female.gender == Passenger.Gender.FEMALE;
    }

    private static int maximumOccupancy(Trip t,Ticket ignored) {
        int max=0;
        for(int stage=0;stage<t.getRoute().getStopCount()-1;stage++) {
            Set<String> occupied=new HashSet<>();
            for(Ticket tk:tickets.values()) if(tk!=ignored&&tk.tripId.equals(t.getTripId())) for(Pax p:tk.passengers)
                if(!p.cancelled&&!p.freeChild()&&p.board<=stage&&p.drop>stage) occupied.add(p.seat.toUpperCase(Locale.ROOT));
            max=Math.max(max,occupied.size());
        }
        return max;
    }

    private static void calculateFare(Trip t,Pax p,double dynamicPct) {
        p.stageFare=round(t.getRoute().calculateStageFare(p.board,p.drop));
        p.dynamicPct=dynamicPct;
        p.dynamicAmount=round(p.stageFare*dynamicPct/100.0);
        Seat seat=p.freeChild()?null:t.getBus().findSeat(p.seat);
        p.premium=seat==null?0:seat.getPremium();
        p.childPct=p.age<5?0:p.age<=12?(seat!=null&&seat.getSeatType()==Seat.SeatType.SEATER?75:100):100;
        double factor=p.childPct/100.0;
        p.taxable=round((p.stageFare+p.dynamicAmount)*factor+p.premium);
        p.gst=round(p.taxable*t.getBus().getGstRate());
        p.total=round(p.taxable+p.gst);
    }

    private static void cancel(String[] p,Result r) {
        fields(p,4); Ticket tk=ticket(p[1].trim()); LocalDateTime when=parseTs(p[2]);
        List<Pax> targets=new ArrayList<>();
        if(p[3].trim().equalsIgnoreCase("ALL")) tk.passengers.stream().filter(x->!x.cancelled).forEach(targets::add);
        else {
            Set<String> requested = new HashSet<>();
            for(String no:p[3].split(",")) {
                String key = no.trim().toUpperCase(Locale.ROOT);
                require(requested.add(key), "DUPLICATE_PASSENGER_IN_CANCEL");
                Pax x=findPax(tk,key); require(x!=null&&!x.cancelled,"PASSENGER_NOT_FOUND"); targets.add(x);
            }
        }
        require(!targets.isEmpty(),"NO_ACTIVE_PASSENGERS");
        for(Pax x:targets) if(x.age>=13) {
            long freeChildren=tk.passengers.stream().filter(c->!c.cancelled&&c.freeChild()).count();
            long adults=tk.passengers.stream().filter(a->!a.cancelled&&a.age>=13).count();
            require(adults>1||freeChildren==0||targets.stream().anyMatch(y->y.freeChild()),"CHILD_ALONE");
        }
        Trip t=trip(tk.tripId);
        for(Pax x:targets) {
            LocalDateTime bt=boardTime(t,x.board);
            Duration remaining = Duration.between(when, bt);
            require(!remaining.isNegative(),"BOARDING_TIME_PASSED");
            // Compare exact durations: 24h + 1 second is already in the 90% tier.
            double pct=remaining.compareTo(Duration.ofHours(24))>0?90
                    :remaining.compareTo(Duration.ofHours(12))>=0?75
                    :remaining.compareTo(Duration.ofHours(4))>=0?50:0;
            double refund=round(x.total*pct/100.0); r.refund+=refund; r.fee+=round(x.total-refund);
            x.cancelled=true; x.status="CANCELLED";
            if(tk.channel.equals("PARTNER")) {
                Partner partner=partners.get(tk.partnerId);
                if(partner!=null) tkCancellationShare.merge(tk.id,round((x.total-refund)*partner.getCancellationFeeSharePct()/100.0),Double::sum);
            }
        }
        // Recompute the remaining passenger fare lines after a partial cancellation.
        // Occupancy uplift is retained from booking time; the remaining journey's
        // stage fare and tax breakdown are recalculated from its current passenger state.
        for (Pax remaining : tk.passengers) {
            if (!remaining.cancelled) calculateFare(t, remaining, remaining.dynamicPct);
        }
        r.refund=round(r.refund); r.fee=round(r.fee);
    }
    private static final Map<String,Double> tkCancellationShare=new HashMap<>();
    private static Pax findPax(Ticket t,String no) { for(Pax x:t.passengers) if(x.no.equalsIgnoreCase(no)) return x; return null; }

    private static void reschedule(String[] p,Result r) {
        fields(p,5); Ticket tk=ticket(p[1].trim()); LocalDateTime request=parseTs(p[2]); Trip old=trip(tk.tripId), next=trip(p[3].trim());
        require(tk.rescheduleCount==0,"RESCHEDULE_LIMIT"); require(!next.isCancelled(),"TRIP_CANCELLED");
        require(!next.getJourneyDate().isBefore(old.getJourneyDate())&&!next.getJourneyDate().isAfter(old.getJourneyDate().plusDays(30)),"RESCHEDULE_DATE_RANGE");
        List<Pax> active=new ArrayList<>(); for(Pax x:tk.passengers) if(!x.cancelled) active.add(x);
        require(!active.isEmpty(),"NO_ACTIVE_PASSENGERS");
        for(Pax x:active) require(Duration.between(request,old.getBoardingTime(x.board)).toMinutes()>=360,"LATE_RESCHEDULE");
        String[] mappings=p[4].split(";",-1); Map<String,String> seatMap=new HashMap<>();
        for(String m:mappings) { String[] a=m.split(":",-1); require(a.length==2,"INVALID_SEAT_MAP"); seatMap.put(a[0].trim(),a[1].trim()); }
        Ticket candidate=new Ticket(tk.id,next.getTripId(),tk.bookedAt,tk.channel,tk.partnerId);
        for(Pax x:active) {
            String newSeat=x.freeChild()?"NOSEAT":seatMap.get(x.no); require(newSeat!=null,"MISSING_SEAT_MAP");
            Pax q=new Pax(x.no,x.name,x.gender,x.age,newSeat,x.board,x.drop);
            q.tripId = next.getTripId();
            candidate.passengers.add(q);
        }
        // Same stop IDs must exist on the destination route.
        for(int i=0;i<active.size();i++) { Pax oldP=active.get(i), newP=candidate.passengers.get(i); String bId=old.getRoute().getStop(oldP.board).getStopId(), dId=old.getRoute().getStop(oldP.drop).getStopId();
            newP.board=stopIndex(next,bId); newP.drop=stopIndex(next,dId); require(newP.board>=0&&newP.drop>newP.board,"RESCHEDULE_ROUTE_MISMATCH"); }
        validateAllSeats(next,candidate,null);
        int occ=maximumOccupancy(next,null); double pct=occ*1.0/next.getBus().getTotalSeats()>=.9?30:occ*1.0/next.getBus().getTotalSeats()>=.7?15:0;
        double oldTotal=0,newTotal=0; for(int i=0;i<active.size();i++) { Pax oldP=active.get(i), q=candidate.passengers.get(i); oldTotal+=oldP.taxable; calculateFare(next,q,pct); newTotal+=q.taxable; q.status="RESCHEDULED_OUT"; }
        r.fee=round(active.size()*50.0); r.fareDifference=round(Math.max(0,newTotal-oldTotal));
        // Keep cancelled passenger rows from the original journey; only active passengers move.
        // The evaluation format requires one final row per passenger per ticket.
        List<Pax> finalPassengers = new ArrayList<>();
        for (Pax oldP : tk.passengers) {
            if (oldP.cancelled) finalPassengers.add(oldP);
        }
        finalPassengers.addAll(candidate.passengers);
        tk.tripId=next.getTripId();
        tk.passengers=finalPassengers;
        tk.rescheduleCount++; tk.rescheduled=true;
    }

    private static void changeBoarding(String[] p,Result r) {
        fields(p,4); Ticket tk=ticket(p[1].trim()); LocalDateTime request=parseTs(p[2]); Trip t=trip(tk.tripId); int newIndex=stopIndex(t,p[3].trim()); require(newIndex>=0,"INVALID_BOARDING_STOP");
        List<Pax> active=tk.passengers.stream().filter(x->!x.cancelled).toList(); require(!active.isEmpty(),"NO_ACTIVE_PASSENGERS");
        for(Pax x:active) { require(newIndex<x.drop,"BOARDING_AFTER_DROPPING"); require(newIndex<x.board,"BOARDING_CHANGE_MUST_BE_EARLIER"); require(Duration.between(request,t.getBoardingTime(x.board)).toMinutes()>=120,"LATE_BOARDING_CHANGE"); }
        for(Pax x:active) { x.board=newIndex; calculateFare(t,x,x.dynamicPct); }
    }

    private static void operatorCancel(String[] p,Result r) {
        fields(p,4); Trip t=trip(p[1].trim()); require(!t.isCancelled(),"TRIP_ALREADY_CANCELLED"); LocalDateTime when=parseTs(p[2]);
        t.cancelTrip();
        for(Ticket tk:tickets.values()) if(tk.tripId.equals(t.getTripId())) for(Pax x:tk.passengers) if(!x.cancelled) {
            r.refund+=x.total; r.credit+=100; x.cancelled=true; x.status="CANCELLED";
            if(tk.channel.equals("PARTNER")) { Partner partner = partners.get(tk.partnerId);
                double commission=round(x.stageFare*x.childPct/100.0*partner.getCommissionPct()/100.0);
                partnerCommissionReversed.merge(tk.id,commission,Double::sum); }
        }
        r.refund=round(r.refund); r.credit=round(r.credit);
    }
    private static final Map<String,Double> partnerCommissionReversed=new HashMap<>();

    private static void printTicket(String[] p) {
        fields(p,2);
        String id = p[1].trim();
        ticket(id); // validate now so the command result remains in file order
        deferredTicketPrints.add(id);
    }

    private static void printTicketNow(String id) {
        Ticket tk = ticket(id);
        System.out.println("\n========== BUS TICKET ==========");
        System.out.println("Ticket ID: " + tk.id + " | Channel: " + tk.channel + " | Partner: " + tk.partnerId);
        double total = 0.0;
        for (Pax pax : tk.passengers) {
            Trip passengerTrip = trip(pax.tripId == null ? tk.tripId : pax.tripId);
            System.out.println("--------------------------------");
            System.out.println("Passenger " + pax.no + ": " + pax.name + " | " + pax.gender + " | Age " + pax.age);
            System.out.println("Trip: " + passengerTrip.getTripId() + " | Seat: " + pax.seat
                    + " | From: " + passengerTrip.getRoute().getStop(pax.board).getStopId()
                    + " | To: " + passengerTrip.getRoute().getStop(pax.drop).getStopId());
            System.out.println("Status: " + (pax.cancelled ? "CANCELLED" : pax.status));
            System.out.println("Stage fare: Rs. " + money(pax.stageFare)
                    + " | Dynamic: " + money(pax.dynamicAmount)
                    + " | Seat premium: Rs. " + money(pax.premium)
                    + " | Child factor: " + money(pax.childPct) + "%");
            System.out.println("Taxable: Rs. " + money(pax.taxable)
                    + " | GST: Rs. " + money(pax.gst)
                    + " | Passenger total: Rs. " + money(pax.total));
            if (!pax.cancelled) total += pax.total;
        }
        System.out.println("--------------------------------");
        System.out.println("ACTIVE BOOKING TOTAL: Rs. " + money(total));
        System.out.println("================================\n");
    }
    private static void printChart(String[] p) {
        fields(p,3); Trip t=trip(p[1].trim()); int idx=stopIndex(t,p[2].trim()); require(idx>=0,"INVALID_BOARDING_STOP");
        List<String[]> rows=new ArrayList<>();
        for(Ticket tk:tickets.values()) if(tk.tripId.equals(t.getTripId())) for(Pax x:tk.passengers) if(!x.cancelled&&x.board==idx) {
            String seat=x.seat; rows.add(new String[]{t.getTripId(),p[2].trim(),t.getBoardingTime(idx).format(OUT_TS),seat,tk.id,x.no,x.name,x.gender==Passenger.Gender.MALE?"M":x.gender==Passenger.Gender.FEMALE?"F":"OTHER",String.valueOf(x.age),t.getRoute().getStop(x.drop).getStopId()});
        }
        rows.sort(Comparator.comparing((String[] a)->a[2]).thenComparing(a->a[3])); chartRows.addAll(rows);
    }
    private static void printSettlement(String[] p) {
        fields(p,4); String pid=p[1].trim(); Partner partner=partners.get(pid); require(partner!=null,"PARTNER_NOT_FOUND"); LocalDate from=LocalDate.parse(p[2]),to=LocalDate.parse(p[3]); require(!to.isBefore(from),"INVALID_DATE_RANGE");
        int sold=0; double base=0,commission=0,feeShare=0,reversed=0;
        for(Ticket tk:tickets.values()) if(tk.channel.equals("PARTNER")&&tk.partnerId.equals(pid)&&!tk.bookedAt.toLocalDate().isBefore(from)&&!tk.bookedAt.toLocalDate().isAfter(to)) {
            sold++; for(Pax x:tk.passengers) { base+=x.stageFare*x.childPct/100.0; commission+=x.stageFare*x.childPct/100.0*partner.getCommissionPct()/100.0; }
            feeShare+=tkCancellationShare.getOrDefault(tk.id,0.0); reversed+=partnerCommissionReversed.getOrDefault(tk.id,0.0);
        }
        double net=commission+feeShare-reversed;
        settlementRows.add(new String[]{pid,from.toString(),to.toString(),String.valueOf(sold),money(base),money(commission),money(feeShare),money(reversed),money(net)});
    }

    private static void writeOutputs(Path out) throws IOException {
        Files.createDirectories(out);
        List<String[]> ticketRows=new ArrayList<>();
        ticketRows.add(new String[]{"ticket_id","pax_no","passenger_name","gender","age","trip_id","seat_no","board_stop_id","drop_stop_id","stage_fare","dynamic_pct","dynamic_amount","seat_premium","child_factor_pct","taxable_fare","gst","passenger_total","status"});
        List<Ticket> sorted=new ArrayList<>(tickets.values()); sorted.sort(Comparator.comparing(x->x.id));
        for(Ticket tk:sorted) { tk.passengers.sort(Comparator.comparingInt(x -> { try { return Integer.parseInt(x.no); } catch (NumberFormatException ex) { return Integer.MAX_VALUE; } })); for(Pax x:tk.passengers) { Trip passengerTrip=trip(x.tripId==null?tk.tripId:x.tripId); ticketRows.add(new String[]{tk.id,x.no,x.name,x.gender==Passenger.Gender.MALE?"M":x.gender==Passenger.Gender.FEMALE?"F":"OTHER",String.valueOf(x.age),passengerTrip.getTripId(),x.seat,passengerTrip.getRoute().getStop(x.board).getStopId(),passengerTrip.getRoute().getStop(x.drop).getStopId(),money(x.stageFare),money(x.dynamicPct),money(x.dynamicAmount),money(x.premium),money(x.childPct),money(x.taxable),money(x.gst),money(x.total),x.cancelled?"CANCELLED":x.status}); } }
        List<String[]> results=new ArrayList<>(); results.add(new String[]{"line_no","command","ref_id","status","reason_code","refund_amount","fee_amount","fare_difference","credit_amount"}); results.addAll(commandResults);
        chartRows.sort(Comparator.comparing((String[] a)->a[2]).thenComparing(a->a[3]));
        List<String[]> chart=new ArrayList<>(); chart.add(new String[]{"trip_id","board_stop_id","board_time","seat_no","ticket_id","pax_no","passenger_name","gender","age","drop_stop_id"}); chart.addAll(chartRows);
        List<String[]> settlement=new ArrayList<>(); settlement.add(new String[]{"partner_id","from_date","to_date","tickets_sold","base_fare_total","commission","cancellation_fee_share","commission_reversed","net_payable"}); settlement.addAll(settlementRows);
        CsvWriter.write(out.resolve("tickets.csv"),ticketRows); CsvWriter.write(out.resolve("command_results.csv"),results); CsvWriter.write(out.resolve("trip_chart.csv"),chart); CsvWriter.write(out.resolve("partner_settlement.csv"),settlement);
    }
}
