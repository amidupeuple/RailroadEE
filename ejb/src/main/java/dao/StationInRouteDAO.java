package dao;

import dto.ScheduleDTO;
import entity.Route;
import entity.Station;
import entity.StationInRoute;
import entity.Train;
import org.apache.log4j.Logger;
import exceptions.EntityUpdateException;
import exceptions.GetScheduleException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.sql.Time;
import java.util.*;

/**
 * DAO for StationInRoute class.
 */
@Stateless
public class StationInRouteDAO {
    private static final Logger log = Logger.getLogger(StationInRouteDAO.class);

    @PersistenceContext
    EntityManager entityManager;

    @EJB
    RouteDAO routeDAO;

    public StationInRouteDAO() {}

    // Get departure time of certain train from the certain station.
    public Time getDepartureTime(int trainNumber, String station) {
        log.debug("Start method getDepartureTime(...)");

        List<Time> buffer = entityManager.createQuery(
                "select s.departureTime from StationInRoute s " +
                "where s.train.number = ?1 and s.station.name = ?2"
        ).setParameter(1, trainNumber)
         .setParameter(2, station)
         .getResultList();

        log.debug("Finish method getDepartureTime(...)");
        return (Time) buffer.get(0);
    }

    /**
     * Get list of schedule objects, it represents train schedule from station A to station B in given time interval.
     * We set in the fromStation field of ScheduleDTO object starting station in particular route, and in the field
     * toStation we set finish station of particular route. In the field departureTime we set departureTime from
     * station A, and in arrivalTime we set arrival time to the station B.
     * @param request - required info
     * @return list os schedule objects.
     */
    public List<ScheduleDTO> getScheduleFromAtoB(ScheduleDTO request) throws GetScheduleException {
        log.debug("Start: getScheduleFromAtoB()");

        //Check if desired stations exist in DB.
        List<Station> stationABuf = entityManager.createQuery(
                "select s from Station s where s.name = ?1"
        ).setParameter(1, request.getFromStation()).getResultList();

        List<Station> stationBBuf = entityManager.createQuery(
                "select s from Station s where s.name = ?1"
        ).setParameter(1, request.getToStation()).getResultList();

        if (stationABuf.isEmpty()) {
            log.error("Departure station is absent in Station table in DB");
            throw new GetScheduleException("Станция отправления отсутствует в базе данных");
        }
        else if (stationBBuf.isEmpty()) {
            log.error("Arrival station is absent in Station table in DB");
            throw new GetScheduleException("Станция прибытия отсутствует в базе данных");
        }

        //Result of this query is list of Object's arrays. Length of each array is 4 elements: 0 - train number,
        //1 - departure time from station A,
        //2 - arrival time to station B, 3 - amount of vacancies in this train.
        List<Object[]> resultList = entityManager.createQuery(
                "select sir3.train.number, sir3.departureTime, " +
                "       sir4.arrivalTime, sir3.train.vacancies " +
                "from StationInRoute sir3, StationInRoute sir4 " +
                "where sir3.train.id = sir4.train.id and " +
                "      sir3.station.name = ?1 and sir4.station.name = ?2 and " +
                "      sir3.departureTime > ?3 and sir4.arrivalTime < ?4"
        ).setParameter(1, request.getFromStation())
        .setParameter(2, request.getToStation())
        .setParameter(3, request.getDepartureTime())
        .setParameter(4, request.getArrivalTime())
        .getResultList();

        int size = resultList.size();

        if (size == 0) {
            log.warn("No schedule for given conditions");
            throw new GetScheduleException("Для задданных условий нет расписания поездов");
        }

        // Packaging response list of objects based on data retrieved from data base.
        ArrayList<ScheduleDTO> scheduleList = new ArrayList<ScheduleDTO>(size);
        for (int i = 0; i < size; i++) {
            Object[] stationsBuf = resultList.get(i);
            scheduleList.add(new ScheduleDTO((Integer) stationsBuf[0],
                    request.getFromStation(),
                    request.getToStation(),
                    (Time) stationsBuf[1],
                    (Time) stationsBuf[2],
                    (Integer) stationsBuf[3]));
        }

        log.debug("Finish: getScheduleFromAtoB()");
        return  scheduleList;
    }

    /**
     * Based on user requirements this method find schedule for given station (e.g. station A).
     * @param userRequirements - it contains name of station A.
     * @return - list of ScheduleDTO objects.
     */
    public List<ScheduleDTO> getScheduleForStation(ScheduleDTO userRequirements) throws GetScheduleException {
        log.debug("Start: getScheduleForStation()");

        //Check, if required station exists in DB.
        List<Station> stationBuf = entityManager.createQuery(
                "select s from Station s where s.name = ?1"
        ).setParameter(1, userRequirements.getFromStation()).getResultList();

        if (stationBuf.isEmpty()) {
            log.warn("given station is absent in DB");
            throw new GetScheduleException("Выбранная станция отсутствует в базе данных");
        }

        //This query returns list of Objects arrays, each array contains such elements: 0 - train number, 1 - first
        //station in route, 2 - last station in route, 3 - departure time from station A, 4 - arrival to station A,
        //5 - vacancies in appropriate train.
        List<Object[]> resultList = entityManager.createQuery(
                "select sir1.train.number, sir1.station.name, sir2.station.name, " +
                "       sir3.departureTime, sir3.arrivalTime, sir1.train.vacancies " +
                "from StationInRoute sir1, StationInRoute sir2, StationInRoute sir3 " +
                "where sir1.train.id = sir2.train.id and sir1.train.id = sir3.train.id and " +
                "      sir1.departureTime = (select min(sir4.departureTime) " +
                "                            from StationInRoute sir4 " +
                "                            where sir4.train.id = sir1.train.id) and " +
                "      sir2.arrivalTime = (select max(sir5.arrivalTime) " +
                "                          from StationInRoute sir5 " +
                "                          where sir5.train.id = sir2.train.id) and " +
                "      sir3.station.name = ?1"
        ).setParameter(1, userRequirements.getFromStation())
         .getResultList();

        int size = resultList.size();

        //Check, if size == 0, then no schedule for given station.
        if (size == 0) {
            log.warn("No schedule for requested station");
            throw new GetScheduleException("Для запрошенной станции нет расписания");
        }

        ArrayList<ScheduleDTO> scheduleList = new ArrayList<ScheduleDTO>(size);
        for (int i = 0; i < size; i++) {
            Object[] arr = resultList.get(i);
            scheduleList.add(new ScheduleDTO((Integer) arr[0],
                    (String) arr[1],
                    (String) arr[2],
                    (Time) arr[3],
                    (Time) arr[4],
                    (Integer) arr[5]));
        }

        return scheduleList;
    }

    public List<Integer> getListOfRoutesIDForStation(String station) {
        log.debug("Start method getListOfRoutesIDForStation(...)");

        List<Integer> routesID = entityManager.createQuery(
                "select s.route.id from StationInRoute s " +
                "where s.station.name = ?1"
        ).setParameter(1, station)
         .getResultList();

        log.debug("Start method getListOfRoutesIDForStation(...)");
        return routesID;
    }

    /**
     * Creation of new route consists of the following steps:
     * - check if number of train specified in administrator's requirements not used in existing route
     * - create new record in Route table
     * - create as many records in StationInRoute table as administrator mentioned in his requirements
     * @param stations - administrator requirements which contains: number of train corresponding to a new route,
     *                   set of stations necessary to add to a new route
     * @throws EntityUpdateException
     */
    public void addRoute(List<ScheduleDTO> stations) throws EntityUpdateException{
        log.debug("Start: addRoute()");
        Set<StationInRoute> setOfStationsInRoute = new HashSet<StationInRoute>();

        //Create new route record
        routeDAO.addRoute();

        //Check if this train already in some route
        List<Integer> bufForRouteId = entityManager.createQuery(
                "select s.route.id from StationInRoute s where s.train.number = ?1"
        ).setParameter(1, stations.get(0).getNumber()).getResultList();

        if (!bufForRouteId.isEmpty()) {
            log.debug("Exception: train already in route");
            throw new EntityUpdateException("Указанный поезд уже задействован в другом маршруте");
        }

        //Get desired train for new route
        List<Train> trainBuf = entityManager.createQuery(
                "select t from Train t where t.number = ?1"
        ).setParameter(1, stations.get(0).getNumber()).getResultList();

        if (trainBuf.isEmpty()) {
            log.debug("Exception: there is no train with mentioned number.");
            throw new EntityUpdateException("Поезда с указанным номером не существует");
        }

        Train train = trainBuf.get(0);

        //Get new route instance
        List<Route> routeBuf = entityManager.createQuery(
                "select r from Route r where r.id = (select max(r1.id) from Route r1)"
        ).getResultList();
        Route route = routeBuf.get(0);

        //Create set of StationInRoute instances.
        for (int i = 0; i < stations.size(); i++) {
            List<Station> buf = entityManager.createQuery(
                    "select s from Station s where s.name = ?1"
            ).setParameter(1, stations.get(i).getFromStation()).getResultList();

            if (buf.isEmpty()) {
                log.debug("Exception: no such station name in Station table.");
                throw new EntityUpdateException("Отсутсвует станция с названием " + stations.get(i).getFromStation());
            }

            StationInRoute sir = new StationInRoute(stations.get(i).getDepartureTime(),
                                                    stations.get(i).getArrivalTime());
            sir.setTrain(train);
            sir.setRoute(route);
            sir.setStation(buf.get(0));

            setOfStationsInRoute.add(sir);
        }

        train.setStationsInRoute(setOfStationsInRoute);
        route.setStationsInRoute(setOfStationsInRoute);

        entityManager.persist(train);
        entityManager.persist(route);

        Iterator<StationInRoute> it = setOfStationsInRoute.iterator();
        while (it.hasNext()) {
            entityManager.persist(it.next());
        }

        log.debug("Finish: addRoute()");
    }
}
