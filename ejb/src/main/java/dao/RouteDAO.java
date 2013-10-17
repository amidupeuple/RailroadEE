package dao;

import entity.Passenger;
import entity.Route;
import org.apache.log4j.Logger;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * DAO for Route entity.
 */
@Stateless
public class RouteDAO {
    private static final Logger log = Logger.getLogger(Passenger.class);

    @PersistenceContext
    private EntityManager entityManager;

    public RouteDAO() {}

    public void addRoute() {
        log.debug("Start: addRoute()");

        Route newRoute = new Route();
        entityManager.persist(newRoute);

        log.debug("Finish: addRoute()");
    }
}
